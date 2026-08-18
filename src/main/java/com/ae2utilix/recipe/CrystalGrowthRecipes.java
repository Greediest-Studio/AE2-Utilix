package com.ae2utilix.recipe;

import com.ae2utilix.AE2Utilix;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CrystalGrowthRecipes {

    private static final List<CrystalGrowthRecipe> recipes = new ArrayList<>();

    public static void init() {
        List<String> recipeFiles = new ArrayList<>();
        recipeFiles.add("assets/ae2_utilix/recipes/fluix_crystal.json");
        recipeFiles.add("assets/ae2_utilix/recipes/pure_certus_quartz.json");
        recipeFiles.add("assets/ae2_utilix/recipes/pure_nether_quartz.json");
        recipeFiles.add("assets/ae2_utilix/recipes/pure_fluix.json");

        for (String path : recipeFiles) {
            try (InputStreamReader reader = new InputStreamReader(
                    CrystalGrowthRecipes.class.getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8)) {
                JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                recipes.add(CrystalGrowthRecipe.fromJson(json));
            } catch (Exception e) {
                AE2Utilix.LOGGER.error("Failed to load recipe: " + path, e);
            }
        }
    }

    public static List<CrystalGrowthRecipe> getRecipes() {
        return recipes;
    }

    public static void addRecipe(CrystalGrowthRecipe recipe) {
        recipes.add(recipe);
    }

    public static boolean removeRecipeByOutput(ItemStack output) {
        return recipes.removeIf(recipe -> {
            for (ItemStack out : recipe.getOutputs()) {
                if (ItemStack.areItemsEqual(out, output) && ItemStack.areItemStackTagsEqual(out, output)) {
                    return true;
                }
            }
            return false;
        });
    }

    public static void removeAllRecipes() {
        recipes.clear();
    }

    public static CrystalGrowthRecipe findMatchingRecipe(IItemHandler inputInv, FluidStack availableFluid, int parallelMultiplier) {
        CrystalGrowthRecipe bestMatch = null;
        int bestInputCount = -1;
        for (CrystalGrowthRecipe recipe : recipes) {
            if (recipe.matches(inputInv, availableFluid, parallelMultiplier)) {
                int inputCount = recipe.getInputs().size() + (recipe.getInputFluid() != null ? 1 : 0);
                if (inputCount > bestInputCount) {
                    bestInputCount = inputCount;
                    bestMatch = recipe;
                }
            }
        }
        return bestMatch;
    }
}
