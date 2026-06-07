package com.ae2utilix.block.terminal;

public class BlockCraftingTerminal extends BlockFullTerminal {

    public BlockCraftingTerminal() {
        super("crafting_terminal", TileCraftingTerminal.class);
    }

    @Override
    protected int getGuiId() {
        return 11;
    }
}
