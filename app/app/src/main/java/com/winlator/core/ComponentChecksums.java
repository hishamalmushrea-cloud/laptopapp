package com.winlator.core;

import android.content.Context;

import com.winlator.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Verifies installable components (Box64, DXVK, VKD3D, Turnip, WineD3D, SoundFont)
 * against a pinned SHA-256 manifest shipped inside the APK.
 *
 * Why this exists: components are downloaded at runtime, and upstream fetched them
 * from a moving branch with no integrity check at all. A commit pushed to that branch
 * would land inside the user's container unverified. Here every downloaded file must
 * match an entry in assets/component_checksums.json, which is generated from
 * scripts/assets.sha256 at the same pinned upstream commit as the rest of the tree.
 *
 * Keys are "folder/filename", e.g. "box64/box64-0.4.4.tzst".
 */
public abstract class ComponentChecksums {
    public static final String ASSET_FILE = "component_checksums.json";

    private static JSONObject cache;

    /** Loaded once; the manifest is tiny (a few KB) and immutable for the APK's life. */
    public static synchronized JSONObject load(Context context) {
        if (cache == null) {
            try {
                cache = new JSONObject(FileUtils.readString(context, ASSET_FILE));
            }
            catch (JSONException | NullPointerException e) {
                cache = new JSONObject();
            }
        }
        return cache;
    }

    /** @return the pinned sha256 for the component key, or null when it is not pinned */
    public static String expected(Context context, String componentKey) {
        String value = load(context).optString(componentKey, null);
        return value == null || value.isEmpty() ? null : value;
    }

    public static boolean isPinned(Context context, String componentKey) {
        return expected(context, componentKey) != null;
    }

    /**
     * Hashes the file and compares it with the pinned manifest.
     *
     * MUST be called off the UI thread - components are tens of megabytes.
     *
     * @return 0 on success, otherwise a string resource id describing the failure
     */
    public static int verify(Context context, String componentKey, File file) {
        String expected = expected(context, componentKey);
        if (expected == null) return R.string.component_checksum_unknown;
        if (file == null || !file.isFile()) return R.string.component_checksum_mismatch;
        return ChecksumUtils.matches(file, expected) ? 0 : R.string.component_checksum_mismatch;
    }

    /** Test hook. */
    static synchronized void resetCache() {
        cache = null;
    }
}
