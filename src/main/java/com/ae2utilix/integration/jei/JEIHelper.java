package com.ae2utilix.integration.jei;

import mezz.jei.api.IIngredientListOverlay;
import mezz.jei.api.IJeiRuntime;

public class JEIHelper {

    public static boolean isMouseOverJEI() {
        try {
            IJeiRuntime runtime = JEIUtilixPlugin.getRuntime();
            if (runtime == null) return false;
            IIngredientListOverlay overlay = runtime.getIngredientListOverlay();
            if (overlay == null) return false;
            return overlay.getIngredientUnderMouse() != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
