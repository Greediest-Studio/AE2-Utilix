package com.ae2utilix.block.terminal;

public class BlockPatternTerminal extends BlockFullTerminal {

    public BlockPatternTerminal() {
        super("pattern_terminal", TilePatternTerminal.class);
    }

    @Override
    protected int getGuiId() {
        return 12;
    }
}
