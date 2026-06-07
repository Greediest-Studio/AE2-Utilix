package com.ae2utilix;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;

import java.util.List;

public interface IProductReturnHost {
    List<IAEItemStack> ae2utilix$getExpectedResults();

    IMEInventory<IAEItemStack> ae2utilix$getStorageInventory();

    IEnergySource ae2utilix$getEnergySource();

    IActionSource ae2utilix$getActionSource();
}
