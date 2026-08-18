package com.ae2utilix.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.cache.CraftingGridCache;
import com.ae2utilix.AE2UtilixConfig;
import com.ae2utilix.CpuAccessMode;
import com.ae2utilix.ICpuAccessModeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mixin(value = CraftingGridCache.class, remap = false)
public abstract class MixinCraftingGridCache {

    @Shadow
    @Final
    private Set<CraftingCPUCluster> craftingCPUClusters;

    @Shadow
    @Final
    private IGrid grid;

    @Inject(method = "submitJob", at = @At("HEAD"), cancellable = true)
    private void ae2utilix$filterByAccessMode(ICraftingJob job, ICraftingRequester requestingMachine,
                                               ICraftingCPU target, boolean prioritizePower,
                                               IActionSource src, CallbackInfoReturnable<ICraftingLink> cir) {
        if (!AE2UtilixConfig.enableCpuAccessMode) return;
        if (job.isSimulation()) {
            cir.setReturnValue(null);
            return;
        }

        if (target instanceof CraftingCPUCluster) {
            Object targetObject = target;
            if (!(targetObject instanceof ICpuAccessModeHolder)) {
                // If another transformer replaced the CPU implementation or
                // this feature was only partially applied, preserve AE2's
                // original submission path instead of casting blindly.
                return;
            }
            CpuAccessMode mode = ((ICpuAccessModeHolder) (Object) target).ae2utilix$getAccessMode();
            if (prioritizePower && !mode.allowsPlayer()) {
                cir.setReturnValue(null);
                return;
            }
            if (!prioritizePower && !mode.allowsAutomation()) {
                cir.setReturnValue(null);
                return;
            }
            ICraftingLink result = ((CraftingCPUCluster) target).submitJob(this.grid, job, src, requestingMachine);
            cir.setReturnValue(result);
            return;
        }

        if (target == null) {
            final List<CraftingCPUCluster> validCpusClusters = new ArrayList<>();
            boolean hasRestrictedEligibleCpu = false;
            for (final CraftingCPUCluster cpu : this.craftingCPUClusters) {
                if (!cpu.isActive() || cpu.isBusy() || cpu.getAvailableStorage() < job.getByteTotal()) {
                    continue;
                }

                Object cpuObject = cpu;
                if (!(cpuObject instanceof ICpuAccessModeHolder)) {
                    // A CPU without our optional interface is left to AE2's
                    // normal selection logic for maximum compatibility.
                    validCpusClusters.add(cpu);
                    continue;
                }

                CpuAccessMode mode = ((ICpuAccessModeHolder) cpuObject).ae2utilix$getAccessMode();
                boolean allowed = prioritizePower ? mode.allowsPlayer() : mode.allowsAutomation();
                if (!allowed) {
                    hasRestrictedEligibleCpu = true;
                    continue;
                }

                validCpusClusters.add(cpu);
            }

            // When every eligible CPU is allowed, do not replace AE2's CPU
            // ordering/selection algorithm. Only take over when filtering is
            // actually needed.
            for (final CraftingCPUCluster cpu : this.craftingCPUClusters) {
                Object cpuObject = cpu;
                if (!cpu.isActive() || cpu.isBusy() || cpu.getAvailableStorage() < job.getByteTotal()
                        || !(cpuObject instanceof ICpuAccessModeHolder)) {
                    continue;
                }
                CpuAccessMode mode = ((ICpuAccessModeHolder) cpuObject).ae2utilix$getAccessMode();
                if ((prioritizePower && !mode.allowsPlayer())
                        || (!prioritizePower && !mode.allowsAutomation())) {
                    hasRestrictedEligibleCpu = true;
                    break;
                }
            }
            if (!hasRestrictedEligibleCpu) {
                return;
            }

            Collections.sort(validCpusClusters, (firstCluster, nextCluster) -> {
                if (prioritizePower) {
                    final int comparison1 = Long.compare(nextCluster.getCoProcessors(), firstCluster.getCoProcessors());
                    if (comparison1 != 0) {
                        return comparison1;
                    }
                    return Long.compare(nextCluster.getAvailableStorage(), firstCluster.getAvailableStorage());
                }

                final int comparison2 = Long.compare(firstCluster.getCoProcessors(), nextCluster.getCoProcessors());
                if (comparison2 != 0) {
                    return comparison2;
                }
                return Long.compare(firstCluster.getAvailableStorage(), nextCluster.getAvailableStorage());
            });

            if (!validCpusClusters.isEmpty()) {
                CraftingCPUCluster cpuCluster = validCpusClusters.get(0);
                cir.setReturnValue(cpuCluster.submitJob(this.grid, job, src, requestingMachine));
            } else {
                cir.setReturnValue(null);
            }
        }
    }
}
