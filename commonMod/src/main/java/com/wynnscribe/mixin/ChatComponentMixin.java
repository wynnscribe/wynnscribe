package com.wynnscribe.mixin;

import com.wynnscribe.Translator;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true)
    private Component modifyChatMessageComponent(Component originalComponent) {
        try {
            return Translator.INSTANCE.translateChat(originalComponent);
        } catch (Exception e) {
            e.printStackTrace();
            return originalComponent;
        }
    }
}
