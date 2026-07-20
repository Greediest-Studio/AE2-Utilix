package com.ae2utilix.integration;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.client.button.GuiFCImgButton;
import com.glodblock.github.network.CPacketFluidPatternTermBtns;
import net.minecraft.client.gui.GuiButton;

public final class AE2FCRUDirectCompat {

    private AE2FCRUDirectCompat() {
    }

    public static void sendPatternButton(String name, String value) {
        FluidCraft.proxy.netHandler.sendToServer(new CPacketFluidPatternTermBtns(name, value));
    }

    public static GuiButton createButton(int x, int y, String buttonType, String actionType) {
        return new GuiFCImgButton(x, y, buttonType, actionType);
    }

    public static void setHalfSize(GuiButton button, boolean halfSize) {
        ((GuiFCImgButton) button).setHalfSize(halfSize);
    }
}
