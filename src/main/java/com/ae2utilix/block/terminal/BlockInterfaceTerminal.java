package com.ae2utilix.block.terminal;

public class BlockInterfaceTerminal extends BlockFullTerminal {

    public BlockInterfaceTerminal() {
        super("interface_terminal", TileInterfaceTerminal.class);
    }

    @Override
    protected int getGuiId() {
        return 13;
    }
}
