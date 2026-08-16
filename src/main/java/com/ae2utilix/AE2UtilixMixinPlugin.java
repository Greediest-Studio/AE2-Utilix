package com.ae2utilix;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.ArrayList;
import java.util.List;

public class AE2UtilixMixinPlugin implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        List<String> configs = new ArrayList<>();
        configs.add("mixins.ae2_utilix.json");
        if (Loader.isModLoaded("nae2")) {
            configs.add("mixins.ae2_utilix_nae2.json");
        }
        if (Loader.isModLoaded("random_complement")) {
            configs.add("mixins.ae2_utilix_random_complement.json");
        }
        if (Loader.isModLoaded("ae2fc")) {
            configs.add("mixins.ae2_utilix_ae2fc.json");
        }
        if (Loader.isModLoaded("mekeng")) {
            configs.add("mixins.ae2_utilix_mekeng.json");
        }
        if (Loader.isModLoaded("thaumcraft")
                && Loader.isModLoaded("thaumicenergistics")) {
            configs.add("mixins.ae2_utilix_thaumcraft.json");
        }
        if (Loader.isModLoaded("botania") && Loader.isModLoaded("botania_applie")) {
            configs.add("mixins.ae2_utilix_botania.json");
        }
        return configs;
    }
}
