package com.wynnscribe.mixin;

import com.wynnscribe.ThreadExecutorsKt;
import com.wynnscribe.Translator;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.ActivityModel;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Mixin(value = ActivityModel.class, remap = false)
public class ActivityModelMixin {

    @Unique
    List<Component> wynnscribeKt$translatedText = null;

    @Unique
    Component wynnscribeKt$old = null;

    @Inject(method = "getTrackedTask", at = @At("RETURN"), cancellable = true)
    public void getTrackedTask(CallbackInfoReturnable<StyledText> cir) {
        if(cir.getReturnValue() == StyledText.EMPTY) { return; }
        var component = cir.getReturnValue().getComponent();
        if(this.wynnscribeKt$translatedText == null || !Objects.equals(component, this.wynnscribeKt$old)) {
            this.wynnscribeKt$old = component;
            var info = new ArrayList<Component>(Collections.singletonList(component));
            ThreadExecutorsKt.getThreadExecutors().execute(()-> Translator.INSTANCE.translateActivity(info));
            this.wynnscribeKt$translatedText = info;
        }
        if(this.wynnscribeKt$translatedText.isEmpty()) { return; }
        cir.setReturnValue(StyledText.fromComponent(this.wynnscribeKt$translatedText.getFirst()));
    }
}
