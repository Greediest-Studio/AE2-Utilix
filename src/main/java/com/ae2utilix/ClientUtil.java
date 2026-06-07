package com.ae2utilix;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class ClientUtil {

    public static void sendActionBar(EntityPlayerMP player, String key, Object... args) {
        ITextComponent text = new TextComponentTranslation(key, args);
        player.sendStatusMessage(text, true);
    }
}
