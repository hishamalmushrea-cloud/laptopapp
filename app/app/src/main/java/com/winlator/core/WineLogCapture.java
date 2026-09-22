package com.winlator.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * Persistent, size-bounded capture of the Wine/Box64 process output.
 *
 * <p>ProcessHelper sends that output to /dev/null unless a debug callback is registered,
 * and the on-screen DebugDialog only lives while the session is in the foreground - so a
 * crash normally leaves nothing behind to diagnose (upstream #2001). This callback is
 * registered unconditionally and keeps the tail of the output in "&lt;container&gt;/wine.log",
 * which the user can open from the container menu or carry away inside an exported
 * container archive.
 *
 * <p>Callbacks arrive from the stdout and stderr reader threads, so every entry point is
 * synchronized. The file is capped and rotated, so even a "+all" debug channel cannot
 * fill the device.
 */
public class WineLogCapture implements Callback<String> {
    public static final String FILENAME = "wine.log";
    private static final int MAX_BYTES = 256 * 1024;
    private static final int KEEP_BYTES = MAX_BYTES / 2;
    private static final int FLUSH_EVERY_LINES = 50;

    private final File file;
    private BufferedWriter writer;
    private int written;
    private int sinceFlush;
    private boolean closed;

    public WineLogCapture(File file) {
        this.file = file;
        open(false);
    }

    public static File getFile(File rootDir) {
        return new File(rootDir, FILENAME);
    }

    @Override
    public synchronized void call(String line) {
        if (closed || writer == null || line == null) return;
        try {
            writer.write(line);
            writer.newLine();
            written += line.length() + 1;
            if (++sinceFlush >= FLUSH_EVERY_LINES) flush();
        }
        catch (IOException e) {
            writer = null;
            return;
        }
        if (written > MAX_BYTES) rotate();
    }

    public synchronized void close() {
        closed = true;
        closeWriter();
    }

    private void open(boolean append) {
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), StandardCharsets.UTF_8));
        }
        catch (IOException | NullPointerException e) {
            writer = null;
        }
    }

    private void flush() {
        if (writer == null) return;
        try {
            writer.flush();
            sinceFlush = 0;
        }
        catch (IOException e) {
            writer = null;
        }
    }

    private void closeWriter() {
        if (writer != null) {
            try {
                writer.close();
            }
            catch (IOException e) {}
            writer = null;
        }
    }

    /** Drops the oldest half of the file so the tail - where a crash lives - survives. */
    private void rotate() {
        closeWriter();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            if (length > KEEP_BYTES) {
                byte[] tail = new byte[KEEP_BYTES];
                raf.seek(length - KEEP_BYTES);
                raf.readFully(tail);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(tail);
                }
                written = KEEP_BYTES;
            }
        }
        catch (IOException e) {
            written = 0;
        }
        open(true);
    }
}
