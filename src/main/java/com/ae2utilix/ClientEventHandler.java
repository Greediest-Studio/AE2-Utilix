package com.ae2utilix;

import com.ae2utilix.gui.GuiCrystalGrowthChamber;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import com.ae2utilix.client.FluidMarkModel;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = AE2Utilix.MODID, value = Side.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoaderRegistry.registerLoader(new FluidMarkModel.Loader());
        ModelLoader.setCustomModelResourceLocation(AE2Utilix.PRODUCT_RETURN_CARD, 0,
                new ModelResourceLocation(AE2Utilix.MODID + ":product_return_card", "inventory"));

        ModelLoader.setCustomModelResourceLocation(AE2Utilix.PHASE_CARD, 0,
                new ModelResourceLocation(AE2Utilix.MODID + ":phase_card", "inventory"));

        ModelLoader.setCustomModelResourceLocation(AE2Utilix.COUPLING_STAFF, 0,
                new ModelResourceLocation(AE2Utilix.MODID + ":coupling_staff", "inventory"));

        ModelLoader.setCustomModelResourceLocation(AE2Utilix.PARALLEL_CARD, 0,
                new ModelResourceLocation(AE2Utilix.MODID + ":parallel_card", "inventory"));

        ModelLoader.setCustomModelResourceLocation(AE2Utilix.OVERFLOW_DESTRUCTION_CARD, 0,
                new ModelResourceLocation(AE2Utilix.MODID + ":overflow_destruction_card", "inventory"));

        ModelLoader.setCustomModelResourceLocation(AE2Utilix.FLUID_MARK, 0,
                new ModelResourceLocation(FluidMarkModel.MODEL, "inventory"));

        ModelLoader.setCustomModelResourceLocation(AE2Utilix.MATTER_DECOMPOSER, 0,
                new ModelResourceLocation(AE2Utilix.MODID + ":matter_decomposer", "inventory"));

        ModelLoader.setCustomModelResourceLocation(AE2Utilix.FLUIX_RESONANCE_PIVOT_CORE, 0,
                new ModelResourceLocation(AE2Utilix.MODID + ":fluix_resonance_pivot_core", "inventory"));

        Item phaseInterfaceItem = Item.getItemFromBlock(AE2Utilix.BLOCK_PHASE_INTERFACE);
        if (phaseInterfaceItem != null) {
            ModelLoader.setCustomModelResourceLocation(phaseInterfaceItem, 0,
                    new ModelResourceLocation(AE2Utilix.MODID + ":phase_interface", "inventory"));
        }

        Item commonInterfaceAlternateItem = Item.getItemFromBlock(AE2Utilix.BLOCK_COMMON_INTERFACE_ALTERNATE);
        if (commonInterfaceAlternateItem != null) {
            ModelLoader.setCustomModelResourceLocation(commonInterfaceAlternateItem, 0,
                    new ModelResourceLocation(AE2Utilix.MODID + ":common_interface", "inventory"));
        }

        Item cgcItem = Item.getItemFromBlock(AE2Utilix.BLOCK_CRYSTAL_GROWTH_CHAMBER);
        if (cgcItem != null) {
            ModelLoader.setCustomModelResourceLocation(cgcItem, 0,
                    new ModelResourceLocation(AE2Utilix.MODID + ":crystal_growth_chamber", "inventory"));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.getGui() instanceof GuiCrystalGrowthChamber) {
            GuiCrystalGrowthChamber gui = (GuiCrystalGrowthChamber) event.getGui();
            gui.drawTooltipsLate(event.getMouseX(), event.getMouseY());
        }
    }
}
