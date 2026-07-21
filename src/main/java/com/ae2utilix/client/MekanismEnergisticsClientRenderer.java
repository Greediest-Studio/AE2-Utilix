package com.ae2utilix.client;

import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.client.render.GasStackSizeRenderer;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import com.mekeng.github.common.ItemAndBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.Minecraft;

/** Client-only rendering bridge for Mekanism Energistics gas tokens. */
public final class MekanismEnergisticsClientRenderer {

    private static final GasStackSizeRenderer SIZE_RENDERER = new GasStackSizeRenderer();

    private MekanismEnergisticsClientRenderer() {
    }

    public static TextureAtlasSprite getGasSprite(String gasName) {
        Gas gas = GasRegistry.getGas(gasName);
        return gas == null ? Minecraft.getMinecraft().getTextureMapBlocks()
                .getAtlasSprite("minecraft:blocks/water_still") : gas.getSprite();
    }

    public static int getGasTint(String gasName) {
        Gas gas = GasRegistry.getGas(gasName);
        return gas == null ? 0xFFFFFFFF : gas.getTint();
    }

    public static ItemStack getGasItemStack(String gasName) {
        Gas gas = GasRegistry.getGas(gasName);
        if (gas == null || ItemAndBlocks.DUMMY_GAS == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(ItemAndBlocks.DUMMY_GAS);
        ItemAndBlocks.DUMMY_GAS.setGasStack(stack, new GasStack(gas, 1));
        return stack;
    }

    public static void renderGasAmount(FontRenderer font, String gasName, int amount, int x, int y) {
        Gas gas = GasRegistry.getGas(gasName);
        if (gas == null || amount <= 0) return;
        IAEGasStack stack = AEGasStack.of(new GasStack(gas, amount));
        if (stack != null) SIZE_RENDERER.renderStackSize(font, stack, x, y);
    }

    public static void renderGasSlot(FontRenderer font, String gasName, int amount, int x, int y) {
        Gas gas = GasRegistry.getGas(gasName);
        if (gas == null || amount <= 0) return;
        TextureAtlasSprite sprite = gas.getSprite();
        int tint = gas.getTint();
        float red = (tint >> 16 & 255) / 255.0F;
        float green = (tint >> 8 & 255) / 255.0F;
        float blue = (tint & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.color(red, green, blue, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + 16, 0).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
        buffer.pos(x + 16, y + 16, 0).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
        buffer.pos(x + 16, y, 0).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
        buffer.pos(x, y, 0).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
        tessellator.draw();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        renderGasAmount(font, gasName, amount, x, y);
    }
}
