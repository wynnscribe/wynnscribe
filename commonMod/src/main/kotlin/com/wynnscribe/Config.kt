package com.wynnscribe

import net.minecraft.client.gui.screens.Screen

object Config {
    val enabled: Boolean get() = !Screen.hasShiftDown()
}