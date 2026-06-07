package com.ae2utilix.client;

import com.ae2utilix.block.terminal.TileFullTerminal;
import net.minecraftforge.common.property.IUnlistedProperty;

public class TerminalStateProperty implements IUnlistedProperty<TileFullTerminal> {

    public static final TerminalStateProperty TILE_PROPERTY = new TerminalStateProperty();

    @Override
    public String getName() {
        return "terminal_tile";
    }

    @Override
    public boolean isValid(TileFullTerminal value) {
        return true;
    }

    @Override
    public Class<TileFullTerminal> getType() {
        return TileFullTerminal.class;
    }

    @Override
    public String valueToString(TileFullTerminal value) {
        return value != null ? value.toString() : "null";
    }
}
