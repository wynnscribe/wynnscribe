package com.wynnscribe.mixin;

import com.wynnscribe.mixins.CachedItemStackTranslation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(ItemStack.class)
public class ItemStackMixin implements CachedItemStackTranslation {

    @Unique
    AtomicReference<@Nullable List<@NotNull Component>> wynnscribeKt$cached = new AtomicReference<>();

    @Unique
    @Nullable
    String wynnscribeKt$cacheKey = null;

    @Unique
    @Nullable
    Long wynnscribeKt$refreshed = null;


    @Override
    public void wynnscribeKt$setCachedTranslation(@NotNull String key, long refreshed, @NotNull List<? extends @NotNull Component> tooltip) {
        this.wynnscribeKt$cacheKey = key;
        this.wynnscribeKt$refreshed = refreshed;
        this.wynnscribeKt$cached.set(new ArrayList<>(tooltip));
    }

    @Override
    public @Nullable List<@NotNull Component> wynnscribeKt$cachedTranslation(@NotNull String key, long refreshed) {
        if(!Objects.equals(this.wynnscribeKt$cacheKey, key) || !Objects.equals(this.wynnscribeKt$refreshed, refreshed)) { return null; }
        return this.wynnscribeKt$cached.get();
    }
}
