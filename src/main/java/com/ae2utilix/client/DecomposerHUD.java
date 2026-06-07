package com.ae2utilix.client;

import com.ae2utilix.AE2Utilix;
import com.ae2utilix.AE2UtilixConfig;
import com.ae2utilix.item.ItemMatterDecomposer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = AE2Utilix.MODID, value = Side.CLIENT)
public class DecomposerHUD {

    private static final int DISPLAY_TICKS = 100;
    private static int modeSwitchTimer = 0;
    private static long lastTick = -1;

    public static void setTimer() {
        modeSwitchTimer = DISPLAY_TICKS;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.getItem() != AE2Utilix.MATTER_DECOMPOSER) return;

        if (mc.gameSettings.hideGUI) return;

        long currentTick = player.world.getTotalWorldTime();
        if (lastTick != currentTick) {
            lastTick = currentTick;
            if (modeSwitchTimer > 0) {
                modeSwitchTimer--;
            }
        }

        ItemMatterDecomposer.DecomposerMode mode = ItemMatterDecomposer.getMode(heldItem);
        String modeKey = "item.ae2_utilix.matter_decomposer.mode." + mode.name().toLowerCase();
        String modeText = net.minecraft.client.resources.I18n.format("item.ae2_utilix.matter_decomposer.mode_label")
                + net.minecraft.client.resources.I18n.format(modeKey);

        float alpha;
        if (modeSwitchTimer > 0) {
            alpha = Math.min(1.0f, modeSwitchTimer / 20.0f);
        } else {
            alpha = 0.6f;
        }

        int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        int textWidth = mc.fontRenderer.getStringWidth(modeText);
        int padding = 4;
        int fontHeight = mc.fontRenderer.FONT_HEIGHT;

        int x, y;
        switch (AE2UtilixConfig.decomposerHudPosition) {
            case "top_left":
                x = padding;
                y = padding;
                break;
            case "center_left":
                x = padding;
                y = (screenHeight - fontHeight) / 2;
                break;
            case "center_top":
                x = (screenWidth - textWidth) / 2;
                y = padding;
                break;
            case "top_right":
                x = screenWidth - textWidth - padding;
                y = padding;
                break;
            case "center_right":
                x = screenWidth - textWidth - padding;
                y = (screenHeight - fontHeight) / 2;
                break;
            case "bottom_right":
                x = screenWidth - textWidth - padding;
                y = screenHeight - padding - fontHeight;
                break;
            case "bottom_left":
            default:
                x = padding;
                y = screenHeight - padding - fontHeight;
                break;
        }

        mc.fontRenderer.drawString(modeText, x, y, color, true);
    }
}
