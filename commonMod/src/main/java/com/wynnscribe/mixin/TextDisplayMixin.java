package com.wynnscribe.mixin;

import com.wynnscribe.Config;
import com.wynnscribe.api.API;
import com.wynnscribe.Translator;
import com.wynnscribe.mixins.Translatable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Display.TextDisplay.class)
public class TextDisplayMixin implements Translatable {
    @Inject(method = "getText", at = @At("RETURN"), cancellable = true)
    private void getText(CallbackInfoReturnable<Component> cir) {
        if(!Config.INSTANCE.getEnabled()) { return; }
        cir.setReturnValue(this.wynnscribeKt$getTranslatedText(cir.getReturnValue()));
    }

    @Unique
    @Nullable
    private Component wynnscribeKt$cached = null;

    @Unique
    long wynnscribeKt$refreshed = 0;

    @Unique
    @Nullable
    Component old = null;

    @Override
    public @NotNull Component wynnscribeKt$getTranslatedText(@NotNull Component component) {
        API.TranslationData translation = Translator.INSTANCE.getTranslation();
        if(translation != null) {
            boolean isOld = this.wynnscribeKt$refreshed != translation.getAt().getEpochSeconds();
            if(this.wynnscribeKt$cached == null || isOld || this.old != component) {
                this.wynnscribeKt$cached = Translator.INSTANCE.translateTextDisplay(component);
                this.wynnscribeKt$refreshed = Objects.requireNonNull(Translator.INSTANCE.getTranslation()).getAt().getEpochSeconds();
            }
        } else {
            return component;
        }
        return this.wynnscribeKt$cached;
    }
}
