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
        return configs;
    }
}
