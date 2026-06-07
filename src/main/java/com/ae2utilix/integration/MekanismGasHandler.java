package com.ae2utilix.integration;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.Platform;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.IGasHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

public class MekanismGasHandler {

    @Nullable
    public static EnumFacing findGasOutputFace(TileEntity te, EnumFacing primaryFace, String gasName, int amount) {
        if (te == null || gasName == null) return null;

        if (canDrawGas(te, primaryFace, gasName, amount)) {
            return primaryFace;
        }

        for (EnumFacing face : EnumFacing.values()) {
            if (face == primaryFace) continue;
            if (canDrawGas(te, face, gasName, amount)) {
                return face;
            }
        }

        if (canDrawGas(te, null, gasName, amount)) {
            return null;
        }

        return null;
    }

    public static long extractAndInsertGas(TileEntity te, EnumFacing primaryFace, String gasName, int amount,
                                            IMEInventory<IAEItemStack> dest, IEnergySource energy, IActionSource source) {
        EnumFacing extractFace = findGasOutputFace(te, primaryFace, gasName, amount);
        if (extractFace == null) return 0;

        IGasHandler handler = te.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, extractFace);
        if (handler == null) return 0;

        GasStack simulated = handler.drawGas(extractFace, amount, false);
        if (simulated == null || simulated.amount <= 0) return 0;

        if (!isSameGasType(simulated, gasName)) return 0;

        GasStack actual = handler.drawGas(extractFace, simulated.amount, true);
        if (actual == null || actual.amount <= 0) return 0;

        IAEItemStack toInsert = GasReturnHandler.packGas2AEDrops(actual.getGas().getName(), actual.amount);
        if (toInsert == null) {
            handler.receiveGas(extractFace, actual, true);
            return 0;
        }

        long totalAmount = toInsert.getStackSize();
        IAEItemStack notInserted = Platform.poweredInsert(energy, dest, toInsert, source);

        long insertedAmount = totalAmount;
        if (notInserted != null && notInserted.getStackSize() > 0) {
            insertedAmount -= notInserted.getStackSize();
            Gas leftover = GasRegistry.getGas(gasName);
            if (leftover != null) {
                GasStack leftoverGas = new GasStack(leftover, (int) notInserted.getStackSize());
                handler.receiveGas(extractFace, leftoverGas, true);
            }
        }

        return insertedAmount;
    }

    public static boolean extractAllGases(TileEntity te, EnumFacing extractFace,
                                           IMEInventory<IAEItemStack> dest, IEnergySource energy, IActionSource source) {
        if (te == null) return false;
        boolean didWork = false;
        for (EnumFacing face : EnumFacing.values()) {
            EnumFacing tryFace = (face == extractFace) ? extractFace : face;
            IGasHandler handler = te.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, tryFace);
            if (handler == null) continue;
            for (int attempt = 0; attempt < 4; attempt++) {
                GasStack simulated = handler.drawGas(tryFace, 1000, false);
                if (simulated == null || simulated.amount <= 0) break;
                GasStack actual = handler.drawGas(tryFace, simulated.amount, true);
                if (actual == null || actual.amount <= 0) break;
                IAEItemStack toInsert = GasReturnHandler.packGas2AEDrops(actual.getGas().getName(), actual.amount);
                if (toInsert == null) {
                    handler.receiveGas(tryFace, actual, true);
                    break;
                }
                long totalAmount = toInsert.getStackSize();
                IAEItemStack notInserted = Platform.poweredInsert(energy, dest, toInsert, source);
                if (notInserted != null && notInserted.getStackSize() > 0) {
                    Gas leftover = GasRegistry.getGas(actual.getGas().getName());
                    if (leftover != null) {
                        GasStack leftoverGas = new GasStack(leftover, (int) notInserted.getStackSize());
                        handler.receiveGas(tryFace, leftoverGas, true);
                    }
                }
                didWork = true;
            }
        }
        return didWork;
    }

    private static boolean canDrawGas(TileEntity te, EnumFacing face, String gasName, int amount) {
        IGasHandler handler = te.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, face);
        if (handler == null) return false;
        GasStack simulated = handler.drawGas(face, amount, false);
        if (simulated == null) return false;
        return isSameGasType(simulated, gasName);
    }

    private static boolean isSameGasType(GasStack gasStack, String expectedName) {
        if (gasStack == null || gasStack.getGas() == null) return false;
        return expectedName.equals(gasStack.getGas().getName());
    }
}
