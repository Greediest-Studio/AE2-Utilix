package com.ae2utilix.mixin;

import com.ae2utilix.AE2Utilix;
import com.ae2utilix.AE2UtilixConfig;
import com.ae2utilix.CpuAccessMode;
import com.ae2utilix.ICpuStatusAccessMode;
import com.ae2utilix.network.PacketSwitchCpuAccessMode;
import appeng.container.implementations.CraftingCPUStatus;
import appeng.container.implementations.ContainerCraftingStatus;
import appeng.client.gui.widgets.GuiScrollbar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
@Mixin(value = appeng.client.gui.implementations.GuiCraftingStatus.class, remap = false)
public abstract class MixinGuiCraftingCPU {

    @Accessor("status")
    public abstract ContainerCraftingStatus ae2utilix$getStatus();

    @Accessor("cpuScrollbar")
    public abstract GuiScrollbar ae2utilix$getCpuScrollbar();

    @Unique
    private static final ResourceLocation ae2utilix$TEXTURE_BUTTON_ON =
            new ResourceLocation(AE2Utilix.MODID, "textures/guis/18_18_button_on.png");

    @Unique
    private static final ResourceLocation ae2utilix$TEXTURE_BUTTON_SELECTED =
            new ResourceLocation(AE2Utilix.MODID, "textures/guis/18_18_button_selected.png");

    @Unique
    private static final int ae2utilix$BTN_SIZE = 18;

    @Unique
    private static final int ae2utilix$BTN_OFFSET_X = -21;
    @Unique
    private static final int ae2utilix$BTN_OFFSET_Y = 2;

    @Unique
    private static final ItemStack[] ae2utilix$BTN_ITEMS = {
            new ItemStack(Item.getByNameOrId("appliedenergistics2:crafting_storage_1k")),
            new ItemStack(Blocks.CRAFTING_TABLE),
            new ItemStack(Item.getByNameOrId("appliedenergistics2:material"), 1, 53)
    };

    @Unique
    private static final String[] ae2utilix$BTN_TOOLTIPS = {
            "\u6240\u6709\u7c7b\u578b",
            "\u4ec5\u73a9\u5bb6\u53d1\u8d77",
            "\u4ec5\u81ea\u52a8\u5316\u88c5\u7f6e"
    };

    @Unique
    private final Map<Integer, Integer> ae2utilix$itemIndexMap = new HashMap<>();

    @Unique
    private int ae2utilix$lastOffsetX = 0;
    @Unique
    private int ae2utilix$lastOffsetY = 0;

    @Unique
    private int ae2utilix$getAccessModeIndex(CraftingCPUStatus cpu) {
        Integer local = ae2utilix$itemIndexMap.get(cpu.getSerial());
        if (local != null) {
            return local;
        }
        if (cpu instanceof ICpuStatusAccessMode) {
            return ((ICpuStatusAccessMode) cpu).ae2utilix$getAccessMode().id;
        }
        return 0;
    }

    @Inject(method = "drawFG", at = @At("HEAD"))
    private void ae2utilix$storeOffsets(int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        ae2utilix$lastOffsetX = offsetX;
        ae2utilix$lastOffsetY = offsetY;
    }

    @Inject(method = "hitCpu", at = @At("HEAD"), cancellable = true)
    private void ae2utilix$blockHitCpu(int x, int y, CallbackInfoReturnable<CraftingCPUStatus> cir) {
        if (!AE2UtilixConfig.enableCpuAccessMode) return;
        ContainerCraftingStatus status = ae2utilix$getStatus();
        GuiScrollbar scrollbar = ae2utilix$getCpuScrollbar();
        if (status == null || scrollbar == null) return;

        List<CraftingCPUStatus> cpus = status.getCPUs();
        int firstCpu = scrollbar.getCurrentScroll();
        int rowX = -94 + 9;
        int ox = ae2utilix$lastOffsetX;
        int oy = ae2utilix$lastOffsetY;

        for (int i = firstCpu; i < firstCpu + 6; i++) {
            if (i < 0 || i >= cpus.size()) continue;
            CraftingCPUStatus cpu = cpus.get(i);
            if (cpu == null) continue;

            int rowY = 19 + (i - firstCpu) * 23;
            int btnX = rowX + ae2utilix$BTN_OFFSET_X;
            int btnY = rowY + ae2utilix$BTN_OFFSET_Y;
            int screenBtnX = ox + btnX;
            int screenBtnY = oy + btnY;

            if (x >= screenBtnX && x < screenBtnX + ae2utilix$BTN_SIZE &&
                    y >= screenBtnY && y < screenBtnY + ae2utilix$BTN_SIZE) {
                cir.setReturnValue(null);
                return;
            }
        }
    }

    @Inject(method = "drawFG", at = @At(value = "INVOKE", target = "Lappeng/client/gui/implementations/GuiCraftingCPU;drawFG(IIII)V", shift = At.Shift.AFTER, remap = false))
    private void ae2utilix$drawButtons(int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        if (!AE2UtilixConfig.enableCpuAccessMode) return;
        ContainerCraftingStatus status = ae2utilix$getStatus();
        GuiScrollbar scrollbar = ae2utilix$getCpuScrollbar();

        if (status == null || scrollbar == null) return;

        List<CraftingCPUStatus> cpus = status.getCPUs();
        int firstCpu = scrollbar.getCurrentScroll();
        int rowX = -94 + 9;

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (int i = firstCpu; i < firstCpu + 6; i++) {
            if (i < 0 || i >= cpus.size()) continue;
            CraftingCPUStatus cpu = cpus.get(i);
            if (cpu == null) continue;

            int rowY = 19 + (i - firstCpu) * 23;
            int btnX = rowX + ae2utilix$BTN_OFFSET_X;
            int btnY = rowY + ae2utilix$BTN_OFFSET_Y;

            boolean hover = mouseX >= (offsetX + btnX) && mouseX < (offsetX + btnX) + ae2utilix$BTN_SIZE &&
                    mouseY >= (offsetY + btnY) && mouseY < (offsetY + btnY) + ae2utilix$BTN_SIZE;

            ResourceLocation tex = hover ? ae2utilix$TEXTURE_BUTTON_SELECTED : ae2utilix$TEXTURE_BUTTON_ON;
            Minecraft.getMinecraft().renderEngine.bindTexture(tex);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.drawModalRectWithCustomSizedTexture(btnX, btnY, 0, 0,
                    ae2utilix$BTN_SIZE, ae2utilix$BTN_SIZE, ae2utilix$BTN_SIZE, ae2utilix$BTN_SIZE);

            int itemIdx = ae2utilix$getAccessModeIndex(cpu);
            ItemStack stack = ae2utilix$BTN_ITEMS[itemIdx];
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableDepth();
            GlStateManager.pushMatrix();
            GlStateManager.translate(btnX + 9, btnY + 9, 0);
            GlStateManager.scale(0.8, 0.8, 1.0);
            Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, -8, -8);
            GlStateManager.popMatrix();
            GlStateManager.disableDepth();
            RenderHelper.disableStandardItemLighting();

            if (hover) {
                int itemIdx2 = ae2utilix$getAccessModeIndex(cpu);
                List<String> tooltip = new ArrayList<>();
                tooltip.add(ae2utilix$BTN_TOOLTIPS[itemIdx2]);
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, 300);
                GuiUtils.drawHoveringText(tooltip, mouseX - offsetX, mouseY - offsetY,
                        Minecraft.getMinecraft().currentScreen.width,
                        Minecraft.getMinecraft().currentScreen.height, -1,
                        Minecraft.getMinecraft().fontRenderer);
                GlStateManager.popMatrix();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.disableLighting();
            }
        }

        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
    }

    @Inject(method = {"mouseClicked", "func_73864_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2utilix$onMouseClicked(int xCoord, int yCoord, int mouseButton, CallbackInfo ci) {
        if (!AE2UtilixConfig.enableCpuAccessMode) return;
        ContainerCraftingStatus status = ae2utilix$getStatus();
        GuiScrollbar scrollbar = ae2utilix$getCpuScrollbar();
        if (status == null || scrollbar == null) return;

        List<CraftingCPUStatus> cpus = status.getCPUs();
        int firstCpu = scrollbar.getCurrentScroll();
        int rowX = -94 + 9;
        int ox = ae2utilix$lastOffsetX;
        int oy = ae2utilix$lastOffsetY;

        for (int i = firstCpu; i < firstCpu + 6; i++) {
            if (i < 0 || i >= cpus.size()) continue;
            CraftingCPUStatus cpu = cpus.get(i);
            if (cpu == null) continue;

            int rowY = 19 + (i - firstCpu) * 23;
            int btnX = rowX + ae2utilix$BTN_OFFSET_X;
            int btnY = rowY + ae2utilix$BTN_OFFSET_Y;
            int screenBtnX = ox + btnX;
            int screenBtnY = oy + btnY;

            if (xCoord >= screenBtnX && xCoord < screenBtnX + ae2utilix$BTN_SIZE &&
                    yCoord >= screenBtnY && yCoord < screenBtnY + ae2utilix$BTN_SIZE) {
                int current = ae2utilix$getAccessModeIndex(cpu);
                if (mouseButton == 0) {
                    current = (current + 1) % ae2utilix$BTN_ITEMS.length;
                } else if (mouseButton == 1) {
                    current = (current - 1 + ae2utilix$BTN_ITEMS.length) % ae2utilix$BTN_ITEMS.length;
                }
                ae2utilix$itemIndexMap.put(cpu.getSerial(), current);
                AE2Utilix.NETWORK.sendToServer(new PacketSwitchCpuAccessMode(cpu.getSerial(), CpuAccessMode.fromId(current)));
                ci.cancel();
                return;
            }
        }
    }
}