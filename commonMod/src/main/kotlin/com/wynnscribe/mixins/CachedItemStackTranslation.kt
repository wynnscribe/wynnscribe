package com.wynnscribe.mixins

import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

interface CachedItemStackTranslation {
    fun `wynnscribeKt$setCachedTranslation`(key: String, refreshed: Long, tooltip: List<Component>)

    fun `wynnscribeKt$cachedTranslation`(key: String, refreshed: Long): List<Component>?


    companion object {
        fun ItemStack.setCacheTranslation(key: String, refreshed: Long, tooltip: List<Component>) {
            (this as CachedItemStackTranslation).`wynnscribeKt$setCachedTranslation`(key, refreshed = refreshed, tooltip)
        }

        fun ItemStack.cachedTranslation(key: String, refreshed: Long): List<Component>? {
            return (this as CachedItemStackTranslation).`wynnscribeKt$cachedTranslation`(key, refreshed = refreshed)
        }
    }
}