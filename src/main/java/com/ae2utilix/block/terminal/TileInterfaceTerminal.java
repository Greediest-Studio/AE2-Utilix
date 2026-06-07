package com.ae2utilix.block.terminal;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.IMEMonitor;

public class TileInterfaceTerminal extends TileFullTerminal {

    @Override
    public <T extends IAEStack<T>> IMEMonitor<T> getInventory(IStorageChannel<T> channel) {
        return null;
    }
}
