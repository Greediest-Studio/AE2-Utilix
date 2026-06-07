package com.ae2utilix.client;

import com.ae2utilix.block.terminal.TileFullTerminal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TerminalBakedModel implements IBakedModel {

    private final IBakedModel offModel;
    private final IBakedModel onModel;
    private final IBakedModel hasChannelModel;
    private final IBakedModel baseModel;

    public TerminalBakedModel(IBakedModel baseModel, IBakedModel offModel, IBakedModel onModel, IBakedModel hasChannelModel) {
        this.baseModel = baseModel;
        this.offModel = offModel;
        this.onModel = onModel;
        this.hasChannelModel = hasChannelModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        TileFullTerminal tile = null;
        if (state instanceof IExtendedBlockState) {
            IExtendedBlockState extState = (IExtendedBlockState) state;
            tile = extState.getValue(TerminalStateProperty.TILE_PROPERTY);
        }

        if (tile != null) {
            boolean powered = tile.isClientPowered();
            boolean hasChannel = tile.isClientHasChannel();

            if (powered && hasChannel) {
                return hasChannelModel.getQuads(state, side, rand);
            } else if (powered) {
                return onModel.getQuads(state, side, rand);
            }
        }

        return offModel.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return baseModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseModel.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return baseModel.getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return baseModel.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return baseModel.getOverrides();
    }
}
