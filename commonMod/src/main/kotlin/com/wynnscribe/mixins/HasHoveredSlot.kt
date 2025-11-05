package com.wynnscribe.mixins

import net.minecraft.world.inventory.Slot
import org.jetbrains.annotations.Nullable;

interface HasHoveredSlot {
    @Nullable
    fun `wynnscribe$getHoveredSlot`(): Slot?

    companion object {
        fun HasHoveredSlot.hoveredSlot(): Slot? = this.`wynnscribe$getHoveredSlot`()
    }
}
