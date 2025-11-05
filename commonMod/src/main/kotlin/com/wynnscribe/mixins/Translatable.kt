package com.wynnscribe.mixins

import net.minecraft.network.chat.Component

interface Translatable {
    fun `wynnscribeKt$getTranslatedText`(component: Component): Component
}