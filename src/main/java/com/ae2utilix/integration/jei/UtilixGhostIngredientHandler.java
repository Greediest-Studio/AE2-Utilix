package com.ae2utilix.integration.jei;

import appeng.container.interfaces.IJEIGhostIngredients;
import mezz.jei.api.gui.IAdvancedGuiHandler;
import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.client.gui.inventory.GuiContainer;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

/** Bridges JEI's callback into Utilix GUIs that expose phantom marker targets. */
public final class UtilixGhostIngredientHandler<T extends GuiContainer>
        implements IGhostIngredientHandler<T>, IAdvancedGuiHandler<T> {
    private final Class<T> guiClass;

    public UtilixGhostIngredientHandler(Class<T> guiClass) {
        this.guiClass = guiClass;
    }

    @Override
    public Class<T> getGuiContainerClass() {
        return this.guiClass;
    }

    @Override
    public List<Rectangle> getGuiExtraAreas(T gui) {
        return Collections.emptyList();
    }

    @Override
    public Object getIngredientUnderMouse(T gui, int mouseX, int mouseY) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I> List<Target<I>> getTargets(T gui, I ingredient, boolean doStart) {
        if (!(gui instanceof IJEIGhostIngredients)) return Collections.emptyList();
        List<Target<?>> targets = ((IJEIGhostIngredients) gui).getPhantomTargets(ingredient);
        return (List<Target<I>>) (List<?>) targets;
    }

    @Override
    public void onComplete() {
    }

    @Override
    public boolean shouldHighlightTargets() {
        return true;
    }
}
