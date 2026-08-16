package com.ae2utilix.mixin;

import com.ae2utilix.block.TileCommonInterfaceAlternate;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

/**
 * Optional Thaumcraft transport surface for the common interface.
 *
 * <p>The interface keeps essentia in its own virtual slots, just like its
 * fluid and gas implementations.  Thaumcraft devices do not look at AE2
 * storage capabilities when pulling essentia; they look for
 * {@link IEssentiaTransport}, so this adapter exposes those virtual slots to
 * tubes, rune matrices, and other native Thaumcraft consumers.</p>
 */
@Mixin(TileCommonInterfaceAlternate.class)
public abstract class MixinTileCommonInterfaceThaumcraft implements IEssentiaTransport {

    @Unique
    private TileCommonInterfaceAlternate ae2utilix$tile() {
        return (TileCommonInterfaceAlternate) (Object) this;
    }

    @Unique
    private String ae2utilix$firstStoredAspect() {
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                String tag = tile.getStoredEssentiaAspect(extended, slot);
                if (tag != null && !tag.isEmpty()
                        && tile.getStoredEssentiaAmount(extended, slot) > 0) {
                    return tag;
                }
            }
        }
        return null;
    }

    @Unique
    private String ae2utilix$firstConfiguredAspect() {
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                String tag = tile.getEssentiaConfigAspect(extended, slot);
                if (tag != null && !tag.isEmpty()
                        && tile.canStoreEssentiaInSlot(extended, slot)) {
                    return tag;
                }
            }
        }
        return null;
    }

    @Unique
    private int ae2utilix$storedAmount(String aspectTag) {
        if (aspectTag == null || aspectTag.isEmpty()) return 0;
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        int total = 0;
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                if (aspectTag.equals(tile.getStoredEssentiaAspect(extended, slot))) {
                    total += Math.max(0, tile.getStoredEssentiaAmount(extended, slot));
                }
            }
        }
        return total;
    }

    @Unique
    private boolean ae2utilix$hasInputCapacity() {
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                if (!tile.canStoreEssentiaInSlot(extended, slot)) continue;
                if (tile.getStoredEssentiaAmount(extended, slot)
                        < tile.getVirtualStorageCapacity()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Unique
    private boolean ae2utilix$accepts(Aspect aspect, boolean extended, int slot) {
        if (aspect == null) return false;
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        if (!tile.canStoreEssentiaInSlot(extended, slot)) return false;

        String tag = aspect.getTag();
        String configured = tile.getEssentiaConfigAspect(extended, slot);
        if (configured != null && !tag.equals(configured)) return false;

        String stored = tile.getStoredEssentiaAspect(extended, slot);
        if (stored != null && !tag.equals(stored)) return false;
        return tile.getStoredEssentiaAmount(extended, slot)
                < tile.getVirtualStorageCapacity();
    }

    @Unique
    private int ae2utilix$insert(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) return 0;
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        String tag = aspect.getTag();
        int remaining = amount;

        // Fill matching source slots first, then empty virtual slots.
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (boolean extended : new boolean[]{false, true}) {
                for (int slot = 0; slot < 9 && remaining > 0; slot++) {
                    String stored = tile.getStoredEssentiaAspect(extended, slot);
                    boolean matching = tag.equals(stored);
                    boolean empty = stored == null
                            || tile.getStoredEssentiaAmount(extended, slot) <= 0;
                    if (pass == 0 ? !matching : !empty) continue;
                    if (!this.ae2utilix$accepts(aspect, extended, slot)) continue;

                    int current = Math.max(0,
                            tile.getStoredEssentiaAmount(extended, slot));
                    int accepted = Math.min(remaining,
                            tile.getVirtualStorageCapacity() - current);
                    if (accepted <= 0) continue;
                    tile.setStoredEssentia(extended, slot, tag, current + accepted);
                    remaining -= accepted;
                }
            }
        }
        return amount - remaining;
    }

    @Unique
    private int ae2utilix$extract(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) return 0;
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        String tag = aspect.getTag();
        int remaining = amount;
        int extracted = 0;
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9 && remaining > 0; slot++) {
                if (!tag.equals(tile.getStoredEssentiaAspect(extended, slot))) continue;
                int current = Math.max(0,
                        tile.getStoredEssentiaAmount(extended, slot));
                int taken = Math.min(remaining, current);
                if (taken > 0) {
                    tile.setStoredEssentia(extended, slot, tag, current - taken);
                    extracted += taken;
                    remaining -= taken;
                }
            }
        }
        return extracted;
    }

    @Override
    public boolean isConnectable(EnumFacing side) {
        return this.canOutputTo(side) || this.canInputFrom(side);
    }

    @Override
    public boolean canInputFrom(EnumFacing side) {
        return this.ae2utilix$hasInputCapacity();
    }

    @Override
    public boolean canOutputTo(EnumFacing side) {
        return this.ae2utilix$firstStoredAspect() != null;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // The interface owns its virtual storage and does not accept a
        // neighbor's suction value.
    }

    @Override
    public Aspect getSuctionType(EnumFacing side) {
        String tag = this.ae2utilix$firstStoredAspect();
        if (tag == null) tag = this.ae2utilix$firstConfiguredAspect();
        return tag == null ? null : Aspect.getAspect(tag);
    }

    @Override
    public int getSuctionAmount(EnumFacing side) {
        if (this.canOutputTo(side)) return -1;
        return this.canInputFrom(side) ? 128 : 0;
    }

    @Override
    public int getMinimumSuction() {
        return 1;
    }

    @Override
    public Aspect getEssentiaType(EnumFacing side) {
        if (!this.canOutputTo(side)) return null;
        String tag = this.ae2utilix$firstStoredAspect();
        return tag == null ? null : Aspect.getAspect(tag);
    }

    @Override
    public int getEssentiaAmount(EnumFacing side) {
        String tag = this.ae2utilix$firstStoredAspect();
        return tag == null ? 0 : this.ae2utilix$storedAmount(tag);
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, EnumFacing side) {
        if (!this.canOutputTo(side) || aspect == null) return 0;
        Aspect output = this.getEssentiaType(side);
        if (output == null || !output.equals(aspect)) return 0;
        return this.ae2utilix$extract(aspect, amount);
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, EnumFacing side) {
        if (!this.canInputFrom(side) || aspect == null) return 0;
        return this.ae2utilix$insert(aspect, amount);
    }
}
