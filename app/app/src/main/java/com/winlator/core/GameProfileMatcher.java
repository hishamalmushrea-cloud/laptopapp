package com.winlator.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Matches a Windows executable to one of the game control profiles bundled in the APK
 * ({@code assets/inputcontrols/library}), so a shortcut can be set up automatically
 * instead of by hand (upstream #2000).
 *
 * <p>An exe name is rarely the marketing title - {@code TESV.exe} is Skyrim,
 * {@code MassEffect2.exe} is "Mass Effect 2" - so matching happens on normalized forms
 * (lower case, letters and digits only) plus a deliberately tiny alias table for the
 * cases no amount of normalization can bridge.
 *
 * <p>The rules are intentionally conservative: a wrong control profile is worse than no
 * profile, because it silently maps the gamepad incorrectly. Substring matching only
 * kicks in from {@link #MIN_SUBSTRING_LENGTH} characters, which keeps short titles such
 * as "RAGE" from matching inside an unrelated exe like {@code average.exe}.
 *
 * <p>Pure and side-effect free, and therefore unit tested; see
 * {@code GameProfileMatcherTest}.
 */
public abstract class GameProfileMatcher {
    /** Below this an exe name carries too little signal to match on. */
    private static final int MIN_EXE_LENGTH = 3;
    /** Below this a title is only matched exactly, never as a substring. */
    private static final int MIN_SUBSTRING_LENGTH = 6;

    /**
     * Normalized exe name -> bundled profile name, for executables whose name shares no
     * substring with the title they belong to. Only titles that actually ship a profile
     * are listed here.
     */
    private static final String[][] ALIASES = {
        {"tesv", "Skyrim"},
        {"skyrimse", "Skyrim"},
        {"skyrimvr", "Skyrim"},
        {"deusexhr", "Deus Ex Human Revolution"},
        {"ff8", "Final Fantasy 8"},
        {"dxhr", "Deus Ex Human Revolution"}
    };

    /**
     * @param exePathOrName a DOS or unix path, or a bare file name; may carry ".exe"
     * @param profileNames  the candidate profile names to choose from
     * @return the matching profile name exactly as it appears in {@code profileNames},
     *         or null when nothing matches confidently
     */
    public static String findBestMatch(String exePathOrName, Collection<String> profileNames) {
        if (profileNames == null || profileNames.isEmpty()) return null;

        String exe = normalize(baseName(exePathOrName));
        if (exe.length() < MIN_EXE_LENGTH) return null;

        String alias = aliasFor(exe);
        if (alias != null) {
            String found = exactMatch(profileNames, normalize(alias));
            if (found != null) return found;
        }

        String exact = exactMatch(profileNames, exe);
        if (exact != null) return exact;

        // Deterministic: iterate in a stable order and let the longest title win, so the
        // same input always produces the same suggestion.
        List<String> sorted = new ArrayList<>(profileNames);
        Collections.sort(sorted);

        String best = null;
        int bestLength = 0;
        for (String name : sorted) {
            if (name == null) continue;
            String normalized = normalize(name);
            if (normalized.length() < MIN_SUBSTRING_LENGTH) continue;
            if (exe.contains(normalized) || (exe.length() >= MIN_SUBSTRING_LENGTH && normalized.contains(exe))) {
                if (normalized.length() > bestLength) {
                    best = name;
                    bestLength = normalized.length();
                }
            }
        }
        return best;
    }

    /** Lower case, letters and digits only - so "GTA 5", "gta_5" and "GTA5" all agree. */
    public static String normalize(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length());
        String lower = value.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) sb.append(c);
        }
        return sb.toString();
    }

    /** Last path segment of a DOS or unix path, without the file extension. */
    private static String baseName(String path) {
        if (path == null) return "";
        int separator = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
        String name = separator != -1 ? path.substring(separator + 1) : path;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String aliasFor(String normalizedExe) {
        for (String[] alias : ALIASES) {
            if (alias[0].equals(normalizedExe)) return alias[1];
        }
        return null;
    }

    private static String exactMatch(Collection<String> profileNames, String normalizedExe) {
        for (String name : profileNames) {
            if (name != null && normalize(name).equals(normalizedExe)) return name;
        }
        return null;
    }
}
