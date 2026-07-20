package com.ae2utilix.gui;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.container.implementations.ContainerMEMonitorable;
import com.ae2utilix.block.terminal.TileInterfaceTerminal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class FullTerminalGuiHandler implements IGuiHandler {

    public static final int GUI_STORAGE_TERMINAL = 10;
    public static final int GUI_CRAFTING_TERMINAL = 11;
    public static final int GUI_PATTERN_TERMINAL = 12;
    public static final int GUI_INTERFACE_TERMINAL = 13;
    public static final int GUI_COMMON_INTERFACE = 14;

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (ID == GUI_COMMON_INTERFACE && te instanceof com.ae2utilix.block.TileCommonInterfaceAlternate) {
            return new ContainerCommonInterface(player.inventory, (com.ae2utilix.block.TileCommonInterfaceAlternate) te);
        }
        if (!(te instanceof ITerminalHost)) {
            return null;
        }

        ITerminalHost host = (ITerminalHost) te;
        switch (ID) {
            case GUI_STORAGE_TERMINAL:
                return new ContainerMEMonitorable(player.inventory, host);
            case GUI_CRAFTING_TERMINAL:
                return new ContainerFullCrafting(player.inventory, host);
            case GUI_PATTERN_TERMINAL:
                return new ContainerFullPattern(player.inventory, host);
            case GUI_INTERFACE_TERMINAL:
                if (te instanceof TileInterfaceTerminal) {
                    return new ContainerFullInterface(player.inventory, (TileInterfaceTerminal) te);
                }
                return null;
            case GUI_COMMON_INTERFACE:
                if (te instanceof com.ae2utilix.block.TileCommonInterfaceAlternate) {
                    return new ContainerCommonInterface(player.inventory, (com.ae2utilix.block.TileCommonInterfaceAlternate) te);
                }
                return null;
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (ID == GUI_COMMON_INTERFACE && te instanceof com.ae2utilix.block.TileCommonInterfaceAlternate) {
            return new GuiCommonInterface(player.inventory, (com.ae2utilix.block.TileCommonInterfaceAlternate) te);
        }
        if (!(te instanceof ITerminalHost)) {
            return null;
        }

        ITerminalHost host = (ITerminalHost) te;
        switch (ID) {
            case GUI_STORAGE_TERMINAL:
                return new GuiMEMonitorable(player.inventory, host);
            case GUI_CRAFTING_TERMINAL:
                return new GuiFullCrafting(player.inventory, host);
            case GUI_PATTERN_TERMINAL:
                return new GuiFullPattern(player.inventory, host);
            case GUI_INTERFACE_TERMINAL:
                if (te instanceof TileInterfaceTerminal) {
                    return new GuiFullInterface(player.inventory, (TileInterfaceTerminal) te);
                }
                return null;
            case GUI_COMMON_INTERFACE:
                if (te instanceof com.ae2utilix.block.TileCommonInterfaceAlternate) {
                    return new GuiCommonInterface(player.inventory, (com.ae2utilix.block.TileCommonInterfaceAlternate) te);
                }
                return null;
            default:
                return null;
        }
    }
}
