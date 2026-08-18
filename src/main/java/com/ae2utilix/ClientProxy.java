package com.ae2utilix;

import com.ae2utilix.block.TileCrystalGrowthChamber;
import com.ae2utilix.block.terminal.*;
import com.ae2utilix.client.ModelBakeHandler;
import com.ae2utilix.network.HighlightRenderer;
import com.ae2utilix.item.ItemFluidMark;
import com.ae2utilix.parts.PartBlockStorageBus;
import com.ae2utilix.parts.PartCommonImportBus;
import com.ae2utilix.parts.PartCommonExportBus;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import appeng.api.AEApi;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(new ModelBakeHandler());
        MinecraftForge.EVENT_BUS.register(HighlightRenderer.class);

        // Register part models for Block Storage Bus
        try {
            AEApi.instance().registries().partModels().registerModels(PartBlockStorageBus.MODELS_OFF.getModels());
            AEApi.instance().registries().partModels().registerModels(PartBlockStorageBus.MODELS_ON.getModels());
            AEApi.instance().registries().partModels().registerModels(PartBlockStorageBus.MODELS_HAS_CHANNEL.getModels());
            AEApi.instance().registries().partModels().registerModels(PartCommonImportBus.MODELS_OFF.getModels());
            AEApi.instance().registries().partModels().registerModels(PartCommonImportBus.MODELS_ON.getModels());
            AEApi.instance().registries().partModels().registerModels(PartCommonImportBus.MODELS_HAS_CHANNEL.getModels());
            AEApi.instance().registries().partModels().registerModels(PartCommonExportBus.MODELS_OFF.getModels());
            AEApi.instance().registries().partModels().registerModels(PartCommonExportBus.MODELS_ON.getModels());
            AEApi.instance().registries().partModels().registerModels(PartCommonExportBus.MODELS_HAS_CHANNEL.getModels());
        } catch (Exception e) {
            AE2Utilix.LOGGER.warn("Failed to register Block Storage Bus part models", e);
        }

        // Register item model for Block Storage Bus
        ModelResourceLocation busMrl = new ModelResourceLocation(AE2Utilix.BLOCK_STORAGE_BUS.getRegistryName(), "inventory");
        ModelLoader.setCustomModelResourceLocation(AE2Utilix.BLOCK_STORAGE_BUS, 0, busMrl);
        ModelLoader.setCustomModelResourceLocation(AE2Utilix.COMMON_IMPORT_BUS, 0,
                new ModelResourceLocation(AE2Utilix.COMMON_IMPORT_BUS.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(AE2Utilix.COMMON_EXPORT_BUS, 0,
                new ModelResourceLocation(AE2Utilix.COMMON_EXPORT_BUS.getRegistryName(), "inventory"));

        ClientRegistry.bindTileEntitySpecialRenderer(TileCrystalGrowthChamber.class,
                new com.ae2utilix.client.RenderCrystalGrowthChamber());

        Block[] terminalBlocks = {AE2Utilix.BLOCK_STORAGE_TERMINAL, AE2Utilix.BLOCK_CRAFTING_TERMINAL,
                AE2Utilix.BLOCK_PATTERN_TERMINAL, AE2Utilix.BLOCK_INTERFACE_TERMINAL};
        for (Block block : terminalBlocks) {
            ModelResourceLocation mrl = new ModelResourceLocation(block.getRegistryName(), "inventory");
            Item terminalItem = Item.getItemFromBlock(block);
            if (terminalItem != null) {
                for (int metadata = 0; metadata < 16; metadata++) {
                    ModelLoader.setCustomModelResourceLocation(terminalItem, metadata, mrl);
                }
            }
        }
    }

    @Override
    public void init() {
        com.ae2utilix.client.TerminalBlockColor blockColor = new com.ae2utilix.client.TerminalBlockColor();
        Block[] terminalBlocks = {
                AE2Utilix.BLOCK_STORAGE_TERMINAL, AE2Utilix.BLOCK_CRAFTING_TERMINAL,
                AE2Utilix.BLOCK_PATTERN_TERMINAL, AE2Utilix.BLOCK_INTERFACE_TERMINAL
        };
        Item[] terminalItems = {
                Item.getItemFromBlock(AE2Utilix.BLOCK_STORAGE_TERMINAL),
                Item.getItemFromBlock(AE2Utilix.BLOCK_CRAFTING_TERMINAL),
                Item.getItemFromBlock(AE2Utilix.BLOCK_PATTERN_TERMINAL),
                Item.getItemFromBlock(AE2Utilix.BLOCK_INTERFACE_TERMINAL)
        };
        Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(blockColor, terminalBlocks);
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler(
                (stack, tintIndex) -> blockColor.colorMultiplier(null, null, null, tintIndex), terminalItems);
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tintIndex) -> {
            net.minecraftforge.fluids.FluidStack fluid = ItemFluidMark.getFluid(stack);
            if (fluid != null) {
                return fluid.getFluid().getColor(fluid);
            }

            String gasName = ItemFluidMark.getGasName(stack);
            if (gasName != null && com.ae2utilix.integration.MekanismEnergisticsIntegration.isAvailable()) {
                return com.ae2utilix.client.MekanismEnergisticsClientRenderer.getGasTint(gasName);
            }
            return 0xFFFFFFFF;
        }, AE2Utilix.FLUID_MARK);
    }
}
