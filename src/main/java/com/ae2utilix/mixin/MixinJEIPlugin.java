package com.ae2utilix.mixin;

import com.ae2utilix.AE2Utilix;
import com.ae2utilix.gui.ContainerFullCrafting;
import com.ae2utilix.gui.ContainerFullPattern;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.config.Constants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;

@Mixin(targets = "appeng.integration.modules.jei.JEIPlugin", remap = false)
public class MixinJEIPlugin {

    @Inject(method = "register", at = @At("RETURN"), remap = false)
    private void ae2utilix$registerRecipeTransferHandlers(mezz.jei.api.IModRegistry registry, CallbackInfo ci) {
        try {
            Class<?> handlerClass = Class.forName("appeng.integration.modules.jei.RecipeTransferHandler");
            Constructor<?> ctor = handlerClass.getDeclaredConstructor(Class.class);
            ctor.setAccessible(true);

            // Crafting terminal - same as ContainerCraftingTerm
            Object craftingHandler = ctor.newInstance(ContainerFullCrafting.class);
            registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                    (mezz.jei.api.recipe.transfer.IRecipeTransferHandler) craftingHandler,
                    VanillaRecipeCategoryUid.CRAFTING);

            // Pattern terminal - universal (handles all recipe types)
            Object patternHandler = ctor.newInstance(ContainerFullPattern.class);
            registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                    (mezz.jei.api.recipe.transfer.IRecipeTransferHandler) patternHandler,
                    Constants.UNIVERSAL_RECIPE_TRANSFER_UID);

            AE2Utilix.LOGGER.debug("Registered JEI recipe transfer handlers for block terminals");
        } catch (Exception e) {
            AE2Utilix.LOGGER.warn("Failed to register JEI recipe transfer handlers: " + e.getMessage());
        }
    }
}
