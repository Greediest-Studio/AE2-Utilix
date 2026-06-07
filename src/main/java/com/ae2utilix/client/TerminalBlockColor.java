package com.ae2utilix.client;

import appeng.api.util.AEColor;
import com.ae2utilix.block.terminal.TileFullTerminal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

public class TerminalBlockColor implements IBlockColor {

    @Override
    public int colorMultiplier(IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, int tintIndex) {
        if (tintIndex == 0) {
            return -1;
        }

        AEColor color = AEColor.TRANSPARENT;
        if (world != null && pos != null) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileFullTerminal) {
                try {
                    color = ((TileFullTerminal) te).getProxy().getColor();
                } catch (Exception ignored) {
                }
            }
        }

        return color.getVariantByTintIndex(tintIndex);
    }
}
