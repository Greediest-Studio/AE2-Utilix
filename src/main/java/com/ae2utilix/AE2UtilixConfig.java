package com.ae2utilix;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class AE2UtilixConfig {

    public static boolean renderFloatingItem = true;
    public static boolean enableCpuAccessMode = true;
    public static String decomposerHudPosition = "bottom_left";

    public static void load(File configFile) {
        Configuration config = new Configuration(configFile);
        try {
            config.load();
            renderFloatingItem = config.getBoolean("renderFloatingItem", "client", true,
                    "Render floating item and particles in Crystal Growth Chamber");
            enableCpuAccessMode = config.getBoolean("enableCpuAccessMode", "common", true,
                    "Enable CPU access mode buttons in Crafting Status GUI (ALL/Player Only/Automation Only)");
            decomposerHudPosition = config.getString("decomposerHudPosition", "client", "bottom_left",
                    "Position of Matter Decomposer HUD overlay [bottom_left, center_left, top_left, center_top, top_right, center_right, bottom_right]",
                    new String[]{"bottom_left", "center_left", "top_left", "center_top", "top_right", "center_right", "bottom_right"});
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}
