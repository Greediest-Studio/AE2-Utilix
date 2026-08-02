package com.ae2utilix.client;

import com.ae2utilix.item.ItemDevicePackage;
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
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.common.model.IModelState;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Uses the package texture in inventories and resolves to the stored AE item
 * while the package is held in the world, so the normal AE item placement model
 * is used for the preview.
 */
public class DevicePackageModel implements IModel {

    public static final ResourceLocation MODEL = new ResourceLocation("ae2_utilix", "builtin/device_package");
    private static final ResourceLocation PACKAGE_TEXTURE = new ResourceLocation("ae2_utilix", "item/device_package");

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
            return new DevicePackageModel();
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
            this.fallback = resolve(PACKAGE_TEXTURE);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return fallback.getQuads(state, side, rand);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return fallback.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return fallback.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return fallback.isBuiltInRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return fallback.getParticleTexture();
        }

        @Override
        public boolean isAmbientOcclusion(IBlockState state) {
            return fallback.isAmbientOcclusion(state);
        }

        @Override
        public ItemCameraTransforms getItemCameraTransforms() {
            return fallback.getItemCameraTransforms();
        }

        @Override
        public ItemOverrideList getOverrides() {
            return overrides;
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
            return fallback.handlePerspective(type);
        }

        private IBakedModel resolve(ResourceLocation texture) {
            TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                    .getAtlasSprite(texture.toString());
            return resolve(sprite);
        }

        private IBakedModel resolve(TextureAtlasSprite sprite) {
            return new SpriteModel(sprite, state, format, overrides);
        }

        private class Overrides extends ItemOverrideList {
            Overrides() {
                super(Collections.emptyList());
            }

            @Override
            public IBakedModel handleItemState(IBakedModel original, ItemStack stack,
                    @Nullable World world, @Nullable EntityLivingBase entity) {
                // Inventory and GUI rendering has no living entity. Keep the
                // package icon there, while a held stack resolves to the device.
                if (world == null || entity == null) {
                    return fallback;
                }

                ItemStack target = ItemDevicePackage.getTargetStack(stack);
                if (target.isEmpty()) {
                    return fallback;
                }

                try {
                    return Minecraft.getMinecraft().getRenderItem()
                            .getItemModelWithOverrides(target, world, entity);
                } catch (RuntimeException ignored) {
                    return fallback;
                }
            }
        }

        private static class SpriteModel implements IBakedModel {
            private final TextureAtlasSprite sprite;
            private final List<BakedQuad> quads;
            private final ItemOverrideList overrides;

            SpriteModel(TextureAtlasSprite sprite, IModelState state, VertexFormat format,
                    ItemOverrideList overrides) {
                this.sprite = sprite;
                this.quads = ItemLayerModel.getQuadsForSprite(1, sprite, format,
                        Optional.ofNullable(state.apply(Optional.empty()).orElse(null)));
                this.overrides = overrides;
            }

            @Override
            public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
                return quads;
            }

            @Override
            public boolean isAmbientOcclusion() {
                return false;
            }

            @Override
            public boolean isGui3d() {
                return false;
            }

            @Override
            public boolean isBuiltInRenderer() {
                return false;
            }

            @Override
            public TextureAtlasSprite getParticleTexture() {
                return sprite;
            }

            @Override
            public boolean isAmbientOcclusion(IBlockState state) {
                return false;
            }

            @Override
            public ItemCameraTransforms getItemCameraTransforms() {
                return ItemCameraTransforms.DEFAULT;
            }

            @Override
            public ItemOverrideList getOverrides() {
                return overrides;
            }

            @Override
            public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
                return Pair.of(this, null);
            }
        }
    }
}
