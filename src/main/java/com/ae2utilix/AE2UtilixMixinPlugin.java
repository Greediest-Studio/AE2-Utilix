package com.ae2utilix;

import net.minecraftforge.fml.common.Loader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AE2UtilixMixinPlugin implements ILateMixinLoader, IMixinConfigPlugin {

    private static void ensureConfigLoaded() {
        try {
            File configDir = Loader.instance().getConfigDir();
            if (configDir == null) {
                configDir = new File("config");
            }
            AE2UtilixConfig.loadEarly(new File(configDir, "ae2_utilix.cfg"));
        } catch (Throwable ignored) {
            // The normal preInit load still supplies the defaults if the loader
            // is queried before Forge has exposed its config directory.
        }
    }

    @Override
    public List<String> getMixinConfigs() {
        ensureConfigLoaded();
        List<String> configs = new ArrayList<>();
        configs.add("mixins.ae2_utilix.json");
        if ((AE2UtilixConfig.registerCommonInterface
                || AE2UtilixConfig.registerPhaseInterface
                || AE2UtilixConfig.registerProductReturnCard
                || AE2UtilixConfig.registerPhaseCard)
                && Loader.isModLoaded("nae2")) {
            configs.add("mixins.ae2_utilix_nae2.json");
        }
        if (AE2UtilixConfig.registerFullTerminals && Loader.isModLoaded("random_complement")) {
            configs.add("mixins.ae2_utilix_random_complement.json");
        }
        if (AE2UtilixConfig.registerFullTerminals && Loader.isModLoaded("ae2fc")) {
            configs.add("mixins.ae2_utilix_ae2fc.json");
        }
        if (AE2UtilixConfig.registerOverflowDestructionCard && Loader.isModLoaded("mekeng")) {
            configs.add("mixins.ae2_utilix_mekeng.json");
        }
        if (AE2UtilixConfig.registerCommonInterface
                && Loader.isModLoaded("thaumcraft")
                && Loader.isModLoaded("thaumicenergistics")) {
            configs.add("mixins.ae2_utilix_thaumcraft.json");
        }
        if (AE2UtilixConfig.registerCommonInterface
                && Loader.isModLoaded("botania") && Loader.isModLoaded("botania_applie")) {
            configs.add("mixins.ae2_utilix_botania.json");
        }
        return configs;
    }

    @Override
    public void onLoad(String mixinPackage) {
        ensureConfigLoaded();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        ensureConfigLoaded();
        String name = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);

        if (name.equals("MixinDualityInterface")) {
            return AE2UtilixConfig.registerCommonInterface
                    || AE2UtilixConfig.registerPhaseInterface
                    || AE2UtilixConfig.registerProductReturnCard
                    || AE2UtilixConfig.registerPhaseCard;
        }
        if (name.equals("MixinAEBaseContainer") || name.equals("MixinUpgradeInventory")
                || name.equals("MixinUpgradeInvFilter")) {
            return AE2UtilixConfig.registerCommonInterface
                    || AE2UtilixConfig.registerCrystalGrowthChamber
                    || AE2UtilixConfig.registerProductReturnCard
                    || AE2UtilixConfig.registerPhaseCard
                    || AE2UtilixConfig.registerParallelCard
                    || AE2UtilixConfig.registerOverflowDestructionCard;
        }
        if (name.equals("MixinBasicCellInventory") || name.equals("MixinApiClientHelper")) {
            return AE2UtilixConfig.registerOverflowDestructionCard;
        }
        if (name.equals("MixinPartStorageBusAccessor")) {
            return AE2UtilixConfig.registerBlockStorageBus;
        }
        if (name.equals("MixinGrid")) {
            return AE2UtilixConfig.registerPhaseInterface
                    || AE2UtilixConfig.registerCommonInterface;
        }
        if (name.equals("MixinCraftingGridCache") || name.equals("MixinCraftingCPUStatus")
                || name.equals("MixinCraftingCPUCluster")) {
            return AE2UtilixConfig.enableCpuAccessMode;
        }
        if (name.equals("MixinContainerCraftConfirm")) {
            return AE2UtilixConfig.enableCpuAccessMode || AE2UtilixConfig.registerFullTerminals;
        }
        if (name.equals("MixinPacketValueConfig") || name.equals("MixinPacketSwitchGuis")
                || name.equals("MixinPacketJEIRecipe") || name.equals("MixinPacketCompressedNBT")
                || name.equals("MixinGuiMEMonitorable") || name.equals("MixinGuiMEMonitorableAccessor")
                || name.equals("MixinGuiCraftConfirm") || name.equals("MixinJEIPlugin")
                || name.equals("MixinRecipeTransferHandler") || name.equals("MixinAEBaseGuiHandleClick")) {
            return AE2UtilixConfig.registerFullTerminals;
        }
        if (name.equals("MixinGuiCraftingCPU")) {
            return AE2UtilixConfig.enableCpuAccessMode || AE2UtilixConfig.registerFullTerminals;
        }
        if (name.equals("MixinStackSizeRenderer") || name.equals("MixinRenderItemOverlay")) {
            return AE2UtilixConfig.registerFluidMark
                    || AE2UtilixConfig.registerCommonInterface
                    || AE2UtilixConfig.registerCommonBuses;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                         IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                          IMixinInfo mixinInfo) {
    }
}
