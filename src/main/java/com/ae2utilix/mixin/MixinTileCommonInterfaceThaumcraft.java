package com.ae2utilix.mixin;

import com.ae2utilix.block.TileCommonInterfaceAlternate;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
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
public abstract class MixinTileCommonInterfaceThaumcraft implements IEssentiaTransport, IAspectSource {

    @Unique
    private TileCommonInterfaceAlternate ae2utilix$tile() {
        return (TileCommonInterfaceAlternate) (Object) this;
    }

    @Unique
    private String ae2utilix$firstStoredAspect() {
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        // Prefer stored essentia belonging to a marked slot. The transport
        // surface can only expose one aspect at a time, so an unrelated
        // unconfigured buffer must not mask the aspect requested by a marker.
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                String configured = tile.getEssentiaConfigAspect(extended, slot);
                String stored = tile.getStoredEssentiaAspect(extended, slot);
                if (configured != null && configured.equals(stored)
                        && tile.getStoredEssentiaAmount(extended, slot) > 0) {
                    return stored;
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
    private String ae2utilix$transportAspect() {
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        String configuredFallback = null;
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                String configured = tile.getEssentiaConfigAspect(extended, slot);
                if (configured == null || configured.isEmpty()) continue;
                if (configuredFallback == null
                        && tile.canStoreEssentiaInSlot(extended, slot)) {
                    configuredFallback = configured;
                }
                if (configured.equals(tile.getStoredEssentiaAspect(extended, slot))
                        && tile.getStoredEssentiaAmount(extended, slot) > 0) {
                    return configured;
                }
            }
        }
        return configuredFallback;
    }

    @Unique
    private int ae2utilix$storedAmount(String aspectTag) {
        if (aspectTag == null || aspectTag.isEmpty()) return 0;
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        int total = 0;
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                if (aspectTag.equals(tile.getEssentiaConfigAspect(extended, slot))
                        && aspectTag.equals(tile.getStoredEssentiaAspect(extended, slot))) {
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
                if (!tag.equals(tile.getEssentiaConfigAspect(extended, slot))
                        || !tag.equals(tile.getStoredEssentiaAspect(extended, slot))) continue;
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

    @Unique
    private void ae2utilix$ensureMarkedEssentia() {
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        if (tile.getWorld() == null || tile.getWorld().isRemote
                || this.ae2utilix$firstConfiguredAspect() == null) return;
        // Thaumcraft consumers can query transport state before the AE2 tick
        // manager runs. Populate the virtual slot on demand in that case.
        com.ae2utilix.integration.ThaumicEnergisticsIntegration
                .requestMarkedEssentia(tile, false);
        com.ae2utilix.integration.ThaumicEnergisticsIntegration
                .requestMarkedEssentia(tile, true);
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
        this.ae2utilix$ensureMarkedEssentia();
        // Keep the endpoint discoverable while its marked amount is being
        // requested from the AE2 network. Thaumcraft consumers call this
        // before getEssentiaAmount(); hiding an empty marked slot here means
        // the consumer never gets a chance to perform the request.
        return this.ae2utilix$firstConfiguredAspect() != null;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // The interface owns its virtual storage and does not accept a
        // neighbor's suction value.
    }

    @Override
    public Aspect getSuctionType(EnumFacing side) {
        // Match Thaumic Energistics' native storage interface: suction is
        // untyped; the actual aspect is supplied by getEssentiaType().
        return null;
    }

    @Override
    public int getSuctionAmount(EnumFacing side) {
        // Match the native Thaumic Energistics output endpoint. A negative
        // output suction keeps this storage endpoint from winning a tube pull
        // contest; consumers that explicitly pull still use takeEssentia().
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
        String tag = this.ae2utilix$transportAspect();
        Aspect aspect = tag == null ? null : Aspect.getAspect(tag);
        return aspect != null && this.ae2utilix$storedAmount(tag) > 0 ? aspect : null;
    }

    @Override
    public int getEssentiaAmount(EnumFacing side) {
        this.ae2utilix$ensureMarkedEssentia();
        String tag = this.ae2utilix$transportAspect();
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

    @Override
    public boolean isBlocked() {
        // The interface is a valid source whenever its virtual slot contains
        // essentia. Thaumcraft's source search rejects blocked IAspectSource
        // instances before it ever calls takeFromContainer().
        return false;
    }

    /*
     * Thaumcraft has two native storage surfaces.  Tubes generally use
     * IEssentiaTransport, while several machines (including the infusion
     * matrix in some builds) first test IAspectContainer.  The marker item is
     * only a visual token, so expose the real virtual slot contents here as a
     * container as well.  This keeps the fake item out of the transfer path.
     */
    @Override
    public AspectList getAspects() {
        this.ae2utilix$ensureMarkedEssentia();
        AspectList result = new AspectList();
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                String tag = this.ae2utilix$tile().getStoredEssentiaAspect(extended, slot);
                String configured = this.ae2utilix$tile()
                        .getEssentiaConfigAspect(extended, slot);
                int amount = this.ae2utilix$tile().getStoredEssentiaAmount(extended, slot);
                Aspect aspect = tag == null ? null : Aspect.getAspect(tag);
                if (aspect != null && configured != null && configured.equals(tag) && amount > 0) {
                    result.add(aspect, amount);
                }
            }
        }
        return result;
    }

    @Override
    public void setAspects(AspectList aspects) {
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                tile.setStoredEssentia(extended, slot, null, 0);
            }
        }
        if (aspects == null) return;
        for (Aspect aspect : aspects.getAspects()) {
            if (aspect == null) continue;
            this.ae2utilix$insert(aspect, Math.max(0, aspects.getAmount(aspect)));
        }
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        if (aspect == null) return false;
        TileCommonInterfaceAlternate tile = this.ae2utilix$tile();
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                if (this.ae2utilix$accepts(aspect, extended, slot)) return true;
            }
        }
        return false;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) return amount;
        return amount - this.ae2utilix$insert(aspect, amount);
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (aspect == null || amount < 0) return false;
        if (amount == 0) return true;
        if (!this.doesContainerContainAmount(aspect, amount)) return false;
        return this.ae2utilix$extract(aspect, amount) == amount;
    }

    @Override
    public boolean takeFromContainer(AspectList requested) {
        if (requested == null) return true;
        for (Aspect aspect : requested.getAspects()) {
            if (aspect == null) continue;
            int amount = Math.max(0, requested.getAmount(aspect));
            if (!this.doesContainerContainAmount(aspect, amount)) return false;
        }
        for (Aspect aspect : requested.getAspects()) {
            if (aspect != null && !this.takeFromContainer(
                    aspect, Math.max(0, requested.getAmount(aspect)))) return false;
        }
        return true;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        if (aspect == null || amount < 0) return false;
        if (amount > 0) this.ae2utilix$ensureMarkedEssentia();
        return this.ae2utilix$storedAmount(aspect.getTag()) >= amount;
    }

    @Override
    public boolean doesContainerContain(AspectList requested) {
        if (requested == null) return true;
        this.ae2utilix$ensureMarkedEssentia();
        for (Aspect aspect : requested.getAspects()) {
            if (aspect == null || !this.doesContainerContainAmount(
                    aspect, requested.getAmount(aspect))) return false;
        }
        return true;
    }

    @Override
    public int containerContains(Aspect aspect) {
        this.ae2utilix$ensureMarkedEssentia();
        return aspect == null ? 0 : this.ae2utilix$storedAmount(aspect.getTag());
    }
}
