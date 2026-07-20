package com.ae2utilix.mixin;

import com.ae2utilix.block.TileCommonInterfaceAlternate;
import com.ae2utilix.integration.BotaniaFluxIntegration;
import net.minecraft.item.ItemStack;
import net.minecraft.item.EnumDyeColor;
import nyonio.IFluixManaReceiver;
import org.spongepowered.asm.mixin.Mixin;

/** Adds Botania's real mana receiver contract only when Botania Applie is present. */
@Mixin(value = TileCommonInterfaceAlternate.class, remap = false)
public abstract class MixinTileCommonInterfaceBotania implements IFluixManaReceiver {

    private TileCommonInterfaceAlternate ae2utilix$tile() {
        return (TileCommonInterfaceAlternate) (Object) this;
    }

    @Override
    public boolean hasFluixPoolCard() {
        return true;
    }

    @Override
    public Object getFluixManaTarget() {
        return ae2utilix$tile();
    }

    @Override
    public int getCurrentMana() {
        return (int) Math.min(Integer.MAX_VALUE,
                BotaniaFluxIntegration.getCurrentMana(ae2utilix$tile()));
    }

    @Override
    public int getMaxMana() {
        return (int) Math.min(Integer.MAX_VALUE,
                BotaniaFluxIntegration.getCapacity(ae2utilix$tile(), BotaniaFluxIntegration.MANA));
    }

    @Override
    public boolean isFull() {
        return getAvailableSpaceForMana() <= 0;
    }

    @Override
    public void recieveMana(int amount) {
        if (amount >= 0) {
            BotaniaFluxIntegration.receiveMana(ae2utilix$tile(), amount, false);
        } else {
            BotaniaFluxIntegration.extractMana(ae2utilix$tile(), -amount, false);
        }
    }

    @Override
    public boolean canRecieveManaFromBursts() {
        return true;
    }

    @Override
    public boolean isOutputtingPower() {
        return true;
    }

    @Override
    public EnumDyeColor getColor() {
        return EnumDyeColor.WHITE;
    }

    @Override
    public void setColor(EnumDyeColor color) {
    }

    @Override
    public boolean canAttachSpark(ItemStack stack) {
        return true;
    }

    @Override
    public int getAvailableSpaceForMana() {
        long space = BotaniaFluxIntegration.getCapacity(ae2utilix$tile(), BotaniaFluxIntegration.MANA)
                - BotaniaFluxIntegration.getCurrentMana(ae2utilix$tile());
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, space));
    }

    @Override
    public boolean areIncomingTranfersDone() {
        return false;
    }
}
