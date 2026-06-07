package com.ae2utilix.integration.jei;

import com.ae2utilix.AE2Utilix;
import com.ae2utilix.recipe.CrystalGrowthRecipe;
import com.ae2utilix.recipe.CrystalGrowthRecipes;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@JEIPlugin
public class JEIUtilixPlugin implements IModPlugin {

    private static IJeiRuntime runtime;

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new CrystalGrowthRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void register(IModRegistry registry) {
        registry.addAdvancedGuiHandlers(new CrystalGrowthChamberGuiHandler());

        List<CrystalGrowthRecipeWrapper> wrappers = new ArrayList<>();
        for (CrystalGrowthRecipe recipe : CrystalGrowthRecipes.getRecipes()) {
            wrappers.add(new CrystalGrowthRecipeWrapper(recipe));
        }
        registry.addRecipes(wrappers, CrystalGrowthRecipeCategory.UID);

        registry.addRecipeCatalyst(new ItemStack(AE2Utilix.BLOCK_CRYSTAL_GROWTH_CHAMBER), CrystalGrowthRecipeCategory.UID);

        registry.addRecipeClickArea(com.ae2utilix.gui.GuiCrystalGrowthChamber.class,
                86, 38, 17, 10,
                CrystalGrowthRecipeCategory.UID);

        // Register AE2FCRU fluid recipe transfer handler for our pattern terminal
        try {
            registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                    new FullPatternRecipeTransferHandler(),
                    mezz.jei.config.Constants.UNIVERSAL_RECIPE_TRANSFER_UID);
        } catch (NoClassDefFoundError ignored) {
            // AE2FCRU not loaded
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static IJeiRuntime getRuntime() {
        return runtime;
    }
}
