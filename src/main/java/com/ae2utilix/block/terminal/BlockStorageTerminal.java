package com.ae2utilix.block.terminal;

public class BlockStorageTerminal extends BlockFullTerminal {

    public BlockStorageTerminal() {
        super("storage_terminal", TileStorageTerminal.class);
    }

    @Override
    protected int getGuiId() {
        return 10;
    }
}
