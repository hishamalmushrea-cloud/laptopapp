package com.winlator.container;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.winlator.core.KeyValueSet;

import org.junit.Test;

/**
 * Pure-logic coverage of {@link GraphicsDrivers} (the context-dependent
 * getDefaultDriver is exercised on-device, not here).
 */
public class GraphicsDriversTest {
    @Test
    public void parseIdentifiersFallsBackToDefaults() {
        assertArrayEquals(new String[]{GraphicsDrivers.VORTEK, GraphicsDrivers.DEFAULT_OPENGL_DRIVER},
            GraphicsDrivers.parseIdentifiers(null));
        assertArrayEquals(new String[]{GraphicsDrivers.VORTEK, GraphicsDrivers.DEFAULT_OPENGL_DRIVER},
            GraphicsDrivers.parseIdentifiers(""));
        assertArrayEquals(new String[]{GraphicsDrivers.VORTEK, GraphicsDrivers.DEFAULT_OPENGL_DRIVER},
            GraphicsDrivers.parseIdentifiers("nonsense"));
    }

    @Test
    public void parseIdentifiersSplitsExplicitPairs() {
        assertArrayEquals(new String[]{"turnip", "gladio"}, GraphicsDrivers.parseIdentifiers("turnip,gladio"));
        assertArrayEquals(new String[]{"vortek", "zink"}, GraphicsDrivers.parseIdentifiers("vortek,zink"));
    }

    @Test
    public void parseIdentifiersCompletesASingleDriver() {
        assertArrayEquals(new String[]{"turnip", GraphicsDrivers.DEFAULT_OPENGL_DRIVER},
            GraphicsDrivers.parseIdentifiers("turnip"));
        assertArrayEquals(new String[]{GraphicsDrivers.DEFAULT_VULKAN_DRIVER, "zink"},
            GraphicsDrivers.parseIdentifiers("zink"));
    }

    @Test
    public void driverClassPredicates() {
        assertTrue(GraphicsDrivers.isVulkanDriver("turnip"));
        assertTrue(GraphicsDrivers.isVulkanDriver("vortek"));
        assertFalse(GraphicsDrivers.isVulkanDriver("zink"));
        assertFalse(GraphicsDrivers.isVulkanDriver(null));

        assertTrue(GraphicsDrivers.isOpenGLDriver("gladio"));
        assertTrue(GraphicsDrivers.isOpenGLDriver("zink"));
        assertTrue(GraphicsDrivers.isOpenGLDriver("virgl"));
        assertFalse(GraphicsDrivers.isOpenGLDriver("turnip"));
    }

    @Test
    public void namesAndItems() {
        assertEquals("Turnip", GraphicsDrivers.getName("turnip"));
        assertEquals("Gladio", GraphicsDrivers.getName("gladio"));
        assertEquals("None", GraphicsDrivers.getName("unknown"));

        assertArrayEquals(new String[]{"Turnip", "Vortek"}, GraphicsDrivers.getItems("VULKAN"));
        assertArrayEquals(new String[]{"Zink", "VirGL", "Gladio"}, GraphicsDrivers.getItems("OPENGL"));
        assertEquals(0, GraphicsDrivers.getItems("DIRECTX").length);
    }

    @Test
    public void parseConfigsEmpty() {
        KeyValueSet[] configs = GraphicsDrivers.parseConfigs("turnip,gladio", null);
        assertEquals(2, configs.length);
        assertTrue(configs[0].isEmpty() && configs[1].isEmpty());
    }

    @Test
    public void parseConfigsPipeSplitsBothSides() {
        KeyValueSet[] configs = GraphicsDrivers.parseConfigs("turnip,gladio", "version=26.1.0|dxvk=2.4.1");
        assertEquals("26.1.0", configs[0].get("version"));
        assertEquals("2.4.1", configs[1].get("dxvk"));
    }

    @Test
    public void parseConfigsAssignsSingleConfigByDriverClass() {
        KeyValueSet[] vulkan = GraphicsDrivers.parseConfigs("turnip", "version=9");
        assertEquals("9", vulkan[0].get("version"));
        assertTrue(vulkan[1].isEmpty());

        KeyValueSet[] opengl = GraphicsDrivers.parseConfigs("gladio", "version=9");
        assertTrue(opengl[0].isEmpty());
        assertEquals("9", opengl[1].get("version"));
    }
}
