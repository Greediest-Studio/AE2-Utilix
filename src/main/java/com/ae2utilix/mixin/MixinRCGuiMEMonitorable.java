package com.ae2utilix.mixin;

import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.container.implementations.ContainerMEMonitorable;
import com.ae2utilix.gui.ContainerFullPattern;
import com.circulation.random_complement.RandomComplement;
import com.circulation.random_complement.client.RCGuiButton;
import com.circulation.random_complement.client.RCSettings;
import com.circulation.random_complement.client.buttonsetting.PatternTermAutoFillPattern;
import com.circulation.random_complement.common.network.RCConfigButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiMEMonitorable.class, remap = false)
public abstract class MixinRCGuiMEMonitorable extends GuiContainer {

    protected MixinRCGuiMEMonitorable(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Shadow(remap = false)
    @Final
    private ContainerMEMonitorable monitorableContainer;

    @Unique
    private RCGuiButton ae2utilix$AutoFillPattern;

    // Use both MCP and SRG names since no refMap is available at runtime
    @Inject(method = {"initGui", "func_73866_w_"}, at = @At("TAIL"), remap = false)
    private void ae2utilix$addAutoFillButton(CallbackInfo ci) {
        if (this.monitorableContainer instanceof ContainerFullPattern) {
            try {
                int top = this.guiTop + 8;
                int left = this.guiLeft - 18;
                for (GuiButton guiButton : this.buttonList) {
                    if (guiButton.x != left) continue;
                    if (top < guiButton.y) top = guiButton.y;
                }
                this.ae2utilix$AutoFillPattern = new RCGuiButton(left, top + 20, RCSettings.PatternTermAutoFillPattern, PatternTermAutoFillPattern.CLOSE);
                this.buttonList.add(this.ae2utilix$AutoFillPattern);
            } catch (NoClassDefFoundError ignored) {
            }
        }
    }

    // Use both MCP and SRG names since no refMap is available at runtime
    @Inject(method = {"actionPerformed", "func_146284_a"}, at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;isButtonDown(I)Z", remap = false), cancellable = true, remap = false)
    protected void ae2utilix$handleAutoFillButton(GuiButton btn, CallbackInfo ci) {
        if (this.monitorableContainer instanceof ContainerFullPattern && this.ae2utilix$AutoFillPattern != null) {
            try {
                if (btn == this.ae2utilix$AutoFillPattern) {
                    boolean backwards = Mouse.isButtonDown(1);
                    RCSettings option = this.ae2utilix$AutoFillPattern.getRCSetting();
                    RandomComplement.NET_CHANNEL.sendToServer(new RCConfigButton(option, backwards));
                    ci.cancel();
                }
            } catch (NoClassDefFoundError ignored) {
            }
        }
    }

    @Inject(method = "drawFG", at = @At("HEAD"), remap = false)
    private void ae2utilix$syncAutoFillButton(int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.monitorableContainer instanceof ContainerFullPattern && this.ae2utilix$AutoFillPattern != null) {
            try {
                ContainerFullPattern cpt = (ContainerFullPattern) this.monitorableContainer;
                PatternTermAutoFillPattern value = "OPEN".equals(cpt.rc$autoFillPattern)
                        ? PatternTermAutoFillPattern.OPEN : PatternTermAutoFillPattern.CLOSE;
                this.ae2utilix$AutoFillPattern.set(value);
            } catch (NoClassDefFoundError ignored) {
            }
        }
    }
}
