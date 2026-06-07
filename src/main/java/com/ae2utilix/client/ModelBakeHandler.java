package com.ae2utilix.client;

import appeng.client.render.model.AutoRotatingModel;
import com.ae2utilix.AE2Utilix;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ModelBakeHandler {

    private static final java.util.Set<String> ORIENTED_BLOCKS = new java.util.HashSet<>(java.util.Arrays.asList(
            "phase_interface"
    ));

    private static final String[] TERMINAL_TYPES = {
            "storage_terminal",
            "crafting_terminal",
            "pattern_terminal",
            "interface_terminal"
    };

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {
        for (ModelResourceLocation location : event.getModelRegistry().getKeys()) {
            if (!location.getResourceDomain().equals(AE2Utilix.MODID)) continue;
            if (!ORIENTED_BLOCKS.contains(location.getResourcePath())) continue;

            IBakedModel original = event.getModelRegistry().getObject(location);
            if (original != null) {
                event.getModelRegistry().putObject(location, new AutoRotatingModel(original));
            }
        }

        for (String terminalType : TERMINAL_TYPES) {
            try {
                TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();

                IModel offIModel = ModelLoaderRegistry.getModel(
                        new ResourceLocation(AE2Utilix.MODID, "block/" + terminalType + "_off"));
                IModel onIModel = ModelLoaderRegistry.getModel(
                        new ResourceLocation(AE2Utilix.MODID, "block/" + terminalType + "_on"));
                IModel hasChannelIModel = ModelLoaderRegistry.getModel(
                        new ResourceLocation(AE2Utilix.MODID, "block/" + terminalType + "_has_channel"));

                IBakedModel offBaked = offIModel.bake(TRSRTransformation.identity(), DefaultVertexFormats.BLOCK,
                        loc -> textureMap.getAtlasSprite(loc.toString()));
                IBakedModel onBaked = onIModel.bake(TRSRTransformation.identity(), DefaultVertexFormats.BLOCK,
                        loc -> textureMap.getAtlasSprite(loc.toString()));
                IBakedModel hasChannelBaked = hasChannelIModel.bake(TRSRTransformation.identity(), DefaultVertexFormats.BLOCK,
                        loc -> textureMap.getAtlasSprite(loc.toString()));

                TerminalBakedModel terminalModel = new TerminalBakedModel(offBaked, offBaked, onBaked, hasChannelBaked);

                // Register the same TerminalBakedModel wrapped in AutoRotatingModel for all facing variants
                for (ModelResourceLocation location : event.getModelRegistry().getKeys()) {
                    if (!location.getResourceDomain().equals(AE2Utilix.MODID)) continue;
                    if (location.getVariant().equals("inventory")) continue;
                    if (!location.getResourcePath().equals(terminalType)) continue;

                    event.getModelRegistry().putObject(location, new AutoRotatingModel(terminalModel));
                }
            } catch (Exception e) {
                AE2Utilix.LOGGER.error("Failed to load terminal models for " + terminalType, e);
            }
        }
    }
}
