package com.ae2utilix.integration.jei;

import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import com.ae2utilix.gui.ContainerFullPattern;
import com.glodblock.github.FluidCraft;
import com.glodblock.github.integration.jei.RecipeTransferBuilder;
import com.glodblock.github.network.CPacketLoadPattern;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class FullPatternRecipeTransferHandler implements IRecipeTransferHandler<ContainerFullPattern> {

    @Override
    @Nonnull
    public Class<ContainerFullPattern> getContainerClass() {
        return ContainerFullPattern.class;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(@Nonnull final ContainerFullPattern container, @Nonnull final IRecipeLayout recipeLayout,
                                               @Nonnull final EntityPlayer player, final boolean maxTransfer, final boolean doTransfer) {
        try {
            if (doTransfer) {
                boolean craftMode = container.isCraftingMode();
                try {
                    if (container.isCraftingMode() && !recipeLayout.getRecipeCategory().getUid().equals(VanillaRecipeCategoryUid.CRAFTING)) {
                        NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.CraftMode", "0"));
                        craftMode = false;
                    } else if (!container.isCraftingMode() && recipeLayout.getRecipeCategory().getUid().equals(VanillaRecipeCategoryUid.CRAFTING)) {
                        NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.CraftMode", "1"));
                        craftMode = true;
                    }
                } catch (final IOException ignore) {
                }
                final RecipeTransferBuilder transfer = new RecipeTransferBuilder(recipeLayout)
                        .clearEmptySlot(!craftMode)
                        .putFluidFirst(container.fluidFirst)
                        .build();
                FluidCraft.proxy.netHandler.sendToServer(new CPacketLoadPattern(transfer.getInput(), transfer.getOutput(), container.combine));
            }
        } catch (NoClassDefFoundError ignored) {
            // AE2FCRU not loaded, fall through
        }
        return null;
    }
}
