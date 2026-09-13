package com.winlator.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Covers the exe -> bundled control profile matching used to configure a shortcut
 * automatically (upstream #2000).
 *
 * Every expected value below was produced by running the same algorithm over the real
 * list of bundled profile names, not guessed: a wrong profile silently remaps the
 * gamepad, so the "no match" cases matter as much as the matches.
 */
public class GameProfileMatcherTest {
    /** A representative slice of the bundled library, including the awkward short names. */
    private static final List<String> PROFILES = Arrays.asList(
        "Bioshock", "Dark Souls 2", "Deus Ex Human Revolution", "Fallout 3",
        "Final Fantasy 8", "GTA 5", "IGI 2", "Mass Effect 2", "Metro 2033",
        "Oblivion", "RAGE", "Skyrim", "Sonic Mania", "Stalker CS", "Wolfenstein"
    );

    @Test
    public void matchesWhenTheExeNameEqualsTheTitle() {
        assertEquals("GTA 5", GameProfileMatcher.findBestMatch("C:\\Games\\GTA5.exe", PROFILES));
        assertEquals("Fallout 3", GameProfileMatcher.findBestMatch("C:\\Games\\Fallout3.exe", PROFILES));
        assertEquals("Mass Effect 2", GameProfileMatcher.findBestMatch("C:\\ME2\\MassEffect2.exe", PROFILES));
        assertEquals("Sonic Mania", GameProfileMatcher.findBestMatch("C:\\SonicMania.exe", PROFILES));
        assertEquals("Stalker CS", GameProfileMatcher.findBestMatch("C:\\Stalker CS\\stalkercs.exe", PROFILES));
        assertEquals("Bioshock", GameProfileMatcher.findBestMatch("C:\\Bioshock\\Bioshock.exe", PROFILES));
    }

    @Test
    public void matchesThroughTheAliasTable() {
        assertEquals("Skyrim", GameProfileMatcher.findBestMatch("C:\\Games\\TESV.exe", PROFILES));
        assertEquals("Skyrim", GameProfileMatcher.findBestMatch("C:\\Games\\SkyrimSE.exe", PROFILES));
        assertEquals("Deus Ex Human Revolution", GameProfileMatcher.findBestMatch("C:\\DXHR\\DeusExHR.exe", PROFILES));
        assertEquals("Final Fantasy 8", GameProfileMatcher.findBestMatch("C:\\FF8\\ff8.exe", PROFILES));
    }

    @Test
    public void matchesAShortTitleExactlyButNeverAsASubstring() {
        // "rage" is only 4 characters: it must match its own exe and nothing else.
        assertEquals("RAGE", GameProfileMatcher.findBestMatch("C:\\rage.exe", PROFILES));
        assertNull(GameProfileMatcher.findBestMatch("C:\\average.exe", PROFILES));
        assertNull(GameProfileMatcher.findBestMatch("C:\\storage.exe", PROFILES));
    }

    @Test
    public void prefersTheLongestTitle() {
        // Neither title matches exactly here, so both match by substring and the more
        // specific one must win.
        List<String> profiles = Arrays.asList("Fallout", "Fallout 3");
        assertEquals("Fallout 3", GameProfileMatcher.findBestMatch("C:\\Fallout3Launcher.exe", profiles));
        assertEquals("Fallout 3", GameProfileMatcher.findBestMatch("C:\\Fallout3.exe", profiles));
    }

    @Test
    public void returnsNullWhenNothingMatchesConfidently() {
        assertNull(GameProfileMatcher.findBestMatch("C:\\Games\\launcher.exe", PROFILES));
        assertNull(GameProfileMatcher.findBestMatch("C:\\a.exe", PROFILES));
        assertNull(GameProfileMatcher.findBestMatch("C:\\WolfSP.exe", PROFILES));
        assertNull(GameProfileMatcher.findBestMatch(null, PROFILES));
        assertNull(GameProfileMatcher.findBestMatch("", PROFILES));
        assertNull(GameProfileMatcher.findBestMatch("C:\\GTA5.exe", Collections.<String>emptyList()));
        assertNull(GameProfileMatcher.findBestMatch("C:\\GTA5.exe", null));
    }

    @Test
    public void acceptsUnixPathsAndBareNames() {
        assertEquals("Oblivion", GameProfileMatcher.findBestMatch("/mnt/sdcard/Oblivion/Oblivion.exe", PROFILES));
        assertEquals("Oblivion", GameProfileMatcher.findBestMatch("/z/game/Oblivion", PROFILES));
        assertEquals("Oblivion", GameProfileMatcher.findBestMatch("Oblivion.exe", PROFILES));
        assertEquals("Oblivion", GameProfileMatcher.findBestMatch("OBLIVION", PROFILES));
    }

    @Test
    public void isDeterministicAcrossCalls() {
        String first = GameProfileMatcher.findBestMatch("C:\\Games\\GTA5.exe", PROFILES);
        for (int i = 0; i < 5; i++) {
            assertEquals(first, GameProfileMatcher.findBestMatch("C:\\Games\\GTA5.exe", PROFILES));
        }
    }

    @Test
    public void normalizeIgnoresCaseSeparatorsAndPunctuation() {
        assertEquals("gta5", GameProfileMatcher.normalize("GTA 5"));
        assertEquals("gta5", GameProfileMatcher.normalize("gta_5"));
        assertEquals("gta5", GameProfileMatcher.normalize("G.T.A. 5!"));
        assertEquals("finalfantasy8", GameProfileMatcher.normalize("Final Fantasy 8"));
        assertEquals("", GameProfileMatcher.normalize(null));
        assertEquals("", GameProfileMatcher.normalize("---"));
    }

    @Test
    public void doesNotMatchTheDotExeExtensionIntoTheName() {
        // "Oblivion.exe" without the strip would normalize to "oblivionexe".
        assertEquals("oblivionexe", GameProfileMatcher.normalize("Oblivion.exe"));
        assertEquals("Oblivion", GameProfileMatcher.findBestMatch("Oblivion.exe", PROFILES));
    }
}
