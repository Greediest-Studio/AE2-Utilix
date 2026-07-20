package com.ae2utilix.client;

import com.ae2utilix.item.ItemFluidMark;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.client.model.ItemLayerModel;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class FluidMarkModel implements IModel {
    public static final ResourceLocation MODEL = new ResourceLocation("ae2_utilix", "builtin/fluid_mark");

    @Override
    @Nonnull
    public IBakedModel bake(@Nonnull IModelState state, @Nonnull VertexFormat format,
                            @Nonnull Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
        return new Baked(state, format);
    }

    public static class Loader implements ICustomModelLoader {
        @Override
        public boolean accepts(ResourceLocation location) {
            return MODEL.equals(location);
        }

        @Override
        public IModel loadModel(ResourceLocation location) {
            return new FluidMarkModel();
        }

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager) {
        }
    }

    private static class Baked implements IBakedModel {
        private final IModelState state;
        private final VertexFormat format;
        private final ItemOverrideList overrides;
        private final IBakedModel fallback;

        Baked(IModelState state, VertexFormat format) {
            this.state = state;
            this.format = format;
            this.overrides = new Overrides();
            this.fallback = ((Overrides) this.overrides)
                    .resolve(new ResourceLocation("minecraft", "blocks/water_still"));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return fallback.getQuads(state, side, rand);
        }

        @Override public boolean isAmbientOcclusion() { return fallback.isAmbientOcclusion(); }
        @Override public boolean isGui3d() { return false; }
        @Override public boolean isBuiltInRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleTexture() { return fallback.getParticleTexture(); }
        @Override public boolean isAmbientOcclusion(IBlockState state) { return false; }
        @Override public ItemCameraTransforms getItemCameraTransforms() { return fallback.getItemCameraTransforms(); }
        @Override public ItemOverrideList getOverrides() { return overrides; }
        @Override public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
            return fallback.handlePerspective(type);
        }

        private class Overrides extends ItemOverrideList {
            Overrides() { super(Collections.emptyList()); }

            @Override
            public IBakedModel handleItemState(IBakedModel original, ItemStack stack,
                                               @Nullable World world, @Nullable EntityLivingBase entity) {
                net.minecraftforge.fluids.FluidStack fluid = ItemFluidMark.getFluid(stack);
                if (fluid != null) return resolve(fluid.getFluid().getStill(fluid));
                String gasName = ItemFluidMark.getGasName(stack);
                if (gasName != null && com.ae2utilix.integration.MekanismEnergisticsIntegration.isAvailable()) {
                    return resolve(com.ae2utilix.client.MekanismEnergisticsClientRenderer.getGasSprite(gasName));
                }
                return resolve(new ResourceLocation("minecraft", "blocks/water_still"));
            }

            IBakedModel resolve(ResourceLocation texture) {
                TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                        .getAtlasSprite(texture.toString());
                return resolve(sprite);
            }

            IBakedModel resolve(TextureAtlasSprite sprite) {
                return new SpriteModel(sprite, state, format, this);
            }
        }

        private static class SpriteModel implements IBakedModel {
            private final TextureAtlasSprite sprite;
            private final List<BakedQuad> quads;
            private final ItemOverrideList overrides;

            SpriteModel(TextureAtlasSprite sprite, IModelState state, VertexFormat format, ItemOverrideList overrides) {
                this.sprite = sprite;
                this.quads = ItemLayerModel.getQuadsForSprite(1, sprite, format, java.util.Optional.ofNullable(state.apply(java.util.Optional.empty()).orElse(null)));
                this.overrides = overrides;
            }

            @Override public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) { return quads; }
            @Override public boolean isAmbientOcclusion() { return false; }
            @Override public boolean isGui3d() { return false; }
            @Override public boolean isBuiltInRenderer() { return false; }
            @Override public TextureAtlasSprite getParticleTexture() { return sprite; }
            @Override public boolean isAmbientOcclusion(IBlockState state) { return false; }
            @Override public ItemCameraTransforms getItemCameraTransforms() { return ItemCameraTransforms.DEFAULT; }
            @Override public ItemOverrideList getOverrides() { return overrides; }
            @Override public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) { return Pair.of(this, null); }
        }
    }
}
