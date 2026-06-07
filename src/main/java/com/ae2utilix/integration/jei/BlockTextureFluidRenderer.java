package com.ae2utilix.integration.jei;

import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlockTextureFluidRenderer implements IIngredientRenderer<FluidStack> {

    private final int fluidSize;

    public BlockTextureFluidRenderer(int width, int height) {
        this.fluidSize = 16;
    }

    @Override
    public void render(Minecraft minecraft, int xPosition, int yPosition, @Nullable FluidStack fluidStack) {
        if (fluidStack == null || fluidStack.getFluid() == null) {
            return;
        }

        Fluid fluid = fluidStack.getFluid();
        ResourceLocation still = fluid.getStill();
        if (still == null) {
            return;
        }

        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();

        minecraft.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        TextureMap textureMap = minecraft.getTextureMapBlocks();
        TextureAtlasSprite sprite = textureMap.getTextureExtry(still.toString());
        if (sprite == null) {
            sprite = textureMap.getMissingSprite();
        }

        int color = fluid.getColor(fluidStack);
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GlStateManager.color(r, g, b, 1.0F);

        drawTiledSprite(xPosition, yPosition, fluidSize, fluidSize, sprite);

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
    }

    private void drawTiledSprite(int x, int y, int tiledWidth, int tiledHeight, TextureAtlasSprite sprite) {
        int xTileCount = tiledWidth / 16;
        int xRemainder = tiledWidth - xTileCount * 16;
        int yTileCount = tiledHeight / 16;
        int yRemainder = tiledHeight - yTileCount * 16;

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int w = (xTile == xTileCount) ? xRemainder : 16;
                int h = (yTile == yTileCount) ? yRemainder : 16;
                int drawX = x + xTile * 16;
                int drawY = y + yTile * 16;
                if (w > 0 && h > 0) {
                    int maskTop = 16 - h;
                    int maskRight = 16 - w;
                    drawTextureWithMasking(drawX, drawY, sprite, maskTop, maskRight);
                }
            }
        }
    }

    private static void drawTextureWithMasking(double xCoord, double yCoord, TextureAtlasSprite sprite, int maskTop, int maskRight) {
        double uMin = sprite.getMinU();
        double uMax = sprite.getMaxU();
        double vMin = sprite.getMinV();
        double vMax = sprite.getMaxV();
        uMax = uMax - (maskRight / 16.0 * (uMax - uMin));
        vMax = vMax - (maskTop / 16.0 * (vMax - vMin));

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(xCoord, yCoord + 16, 100).tex(uMin, vMax).endVertex();
        buffer.pos(xCoord + 16 - maskRight, yCoord + 16, 100).tex(uMax, vMax).endVertex();
        buffer.pos(xCoord + 16 - maskRight, yCoord + maskTop, 100).tex(uMax, vMin).endVertex();
        buffer.pos(xCoord, yCoord + maskTop, 100).tex(uMin, vMin).endVertex();
        tessellator.draw();
    }

    @Override
    public List<String> getTooltip(Minecraft minecraft, FluidStack fluidStack, ITooltipFlag tooltipFlag) {
        List<String> tooltip = new ArrayList<>();
        if (fluidStack != null && fluidStack.getFluid() != null) {
            tooltip.add(fluidStack.getFluid().getLocalizedName(fluidStack));
            tooltip.add(TextFormatting.GRAY.toString() + I18n.format("ae2_utilix.fluid.mb", fluidStack.amount));
        }
        return tooltip;
    }
}
