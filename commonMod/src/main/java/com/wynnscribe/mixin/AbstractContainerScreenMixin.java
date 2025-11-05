package com.wynnscribe.mixin;

import com.wynnscribe.mixins.HasHoveredSlot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin implements HasHoveredSlot {

    @Shadow
    protected Slot hoveredSlot;


    @Override
    public @Nullable Slot wynnscribe$getHoveredSlot() {
        return this.hoveredSlot;
    }
}
