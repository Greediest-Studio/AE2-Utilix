package com.ae2utilix.integration.jei;

import com.ae2utilix.AE2Utilix;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.config.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrystalGrowthRecipeCategory implements IRecipeCategory<CrystalGrowthRecipeWrapper> {

    public static final String UID = AE2Utilix.MODID + ".crystal_growth";

    private static final int INPUT_GRID_X = 0;
    private static final int INPUT_GRID_Y = 0;
    private static final int INPUT_GRID_W = 54;
    private static final int INPUT_GRID_H = 54;

    private static final int OUTPUT_GRID_X = 84;
    private static final int OUTPUT_GRID_Y = 0;
    private static final int OUTPUT_GRID_W = 36;
    private static final int OUTPUT_GRID_H = 54;

    private static final int FLUID_INPUT_SLOT_X = 0;
    private static final int FLUID_INPUT_SLOT_Y = 56;

    private static final int FLUID_OUTPUT_SLOT_X = 84;
    private static final int FLUID_OUTPUT_SLOT_Y = 56;

    private static final int FLUID_W = 18;
    private static final int FLUID_H = 18;

    private static final int ARROW_X = 58;
    private static final int ARROW_Y = 18;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;

    private static final int WIDTH = 122;
    private static final int HEIGHT = 76;

    private static final int SLOT_SIZE = 18;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable inputGridDrawable;
    private final IDrawable outputGridDrawable;
    private final IDrawable slotDrawable;
    private final IDrawableStatic arrowStatic;
    private final IDrawableStatic arrowEmpty;
    private final IGuiHelper guiHelper;
    private final String localizedName;

    private CrystalGrowthRecipeWrapper currentWrapper;
    private IDrawableAnimated currentArrow;

    private final BlockTextureFluidRenderer fluidRenderer;

    public CrystalGrowthRecipeCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        icon = guiHelper.createDrawableIngredient(new ItemStack(AE2Utilix.BLOCK_CRYSTAL_GROWTH_CHAMBER));

        ResourceLocation inputGridTex = new ResourceLocation(AE2Utilix.MODID, "textures/guis/item_3_3_9.png");
        inputGridDrawable = guiHelper.drawableBuilder(inputGridTex, 0, 0, INPUT_GRID_W, INPUT_GRID_H)
                .setTextureSize(INPUT_GRID_W, INPUT_GRID_H).build();

        ResourceLocation outputGridTex = new ResourceLocation(AE2Utilix.MODID, "textures/guis/item_3_2_6.png");
        outputGridDrawable = guiHelper.drawableBuilder(outputGridTex, 0, 0, OUTPUT_GRID_W, OUTPUT_GRID_H)
                .setTextureSize(OUTPUT_GRID_W, OUTPUT_GRID_H).build();

        slotDrawable = guiHelper.getSlotDrawable();

        arrowStatic = guiHelper.drawableBuilder(Constants.RECIPE_GUI_VANILLA, 82, 128, ARROW_W, ARROW_H).build();
        arrowEmpty = guiHelper.drawableBuilder(Constants.RECIPE_GUI_VANILLA, 24, 132, ARROW_W, ARROW_H).build();
        currentArrow = guiHelper.createAnimatedDrawable(arrowStatic, 100, IDrawableAnimated.StartDirection.LEFT, false);

        fluidRenderer = new BlockTextureFluidRenderer(FLUID_W, FLUID_H);

        localizedName = I18n.format("ae2_utilix.jei.crystal_growth.title");
    }

    @Override
    public String getUid() { return UID; }

    @Override
    public String getTitle() { return localizedName; }

    @Override
    public String getModName() { return AE2Utilix.NAME; }

    @Override
    public IDrawable getBackground() { return background; }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void drawExtras(Minecraft minecraft) {
        inputGridDrawable.draw(minecraft, INPUT_GRID_X, INPUT_GRID_Y);
        outputGridDrawable.draw(minecraft, OUTPUT_GRID_X, OUTPUT_GRID_Y);
        arrowEmpty.draw(minecraft, ARROW_X, ARROW_Y);
        currentArrow.draw(minecraft, ARROW_X, ARROW_Y);
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        if (mouseX >= ARROW_X && mouseX < ARROW_X + ARROW_W
                && mouseY >= ARROW_Y && mouseY < ARROW_Y + ARROW_H) {
            CrystalGrowthRecipeWrapper w = currentWrapper;
            if (w != null) {
                List<String> tooltip = new ArrayList<>();
                tooltip.add(w.getProcessingTime() + " tick");
                tooltip.add(String.format("%.0f AE", w.getEnergyCost()));
                return tooltip;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, CrystalGrowthRecipeWrapper recipeWrapper, IIngredients ingredients) {
        this.currentWrapper = recipeWrapper;
        this.currentArrow = guiHelper.createAnimatedDrawable(
                arrowStatic, recipeWrapper.getProcessingTime(),
                IDrawableAnimated.StartDirection.LEFT, false);

        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
        IGuiFluidStackGroup guiFluidStacks = recipeLayout.getFluidStacks();

        int slotIdx = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                guiItemStacks.init(slotIdx, true,
                        INPUT_GRID_X + col * SLOT_SIZE,
                        INPUT_GRID_Y + row * SLOT_SIZE);
                guiItemStacks.setBackground(slotIdx, slotDrawable);
                slotIdx++;
            }
        }

        guiFluidStacks.init(0, true, fluidRenderer, FLUID_INPUT_SLOT_X, FLUID_INPUT_SLOT_Y, FLUID_W, FLUID_H, 1, 1);
        guiFluidStacks.setBackground(0, slotDrawable);

        guiFluidStacks.init(1, false, fluidRenderer, FLUID_OUTPUT_SLOT_X, FLUID_OUTPUT_SLOT_Y, FLUID_W, FLUID_H, 1, 1);
        guiFluidStacks.setBackground(1, slotDrawable);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                guiItemStacks.init(slotIdx, false,
                        OUTPUT_GRID_X + col * SLOT_SIZE,
                        OUTPUT_GRID_Y + row * SLOT_SIZE);
                guiItemStacks.setBackground(slotIdx, slotDrawable);
                slotIdx++;
            }
        }

        guiItemStacks.set(ingredients);
        guiFluidStacks.set(ingredients);
    }
}
