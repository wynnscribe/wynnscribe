package com.wynnscribe

import com.wynnscribe.utils.LegacyTagUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.minecraft.client.Minecraft

object MiniMessage {
    val miniMessage = MiniMessage.builder().strict(false).build()
    val legacy = LegacyComponentSerializer.legacySection()

    fun serialize(component: Component): String {
        return this.miniMessage.serialize(this.miniMessage.deserialize(this.miniMessage.serialize(legacy.deserialize(miniMessage.serialize(component))).replace("\\<", "<")))
    }

    fun serializeList(components: Collection<Component>): String {
        return components.joinToString("\n", transform = ::serialize)
    }

    fun deserialize(text: String): Component {
        return this.miniMessage.deserialize(LegacyTagUtils.replaceLegacyTags(text.replace("<br>", "\n"), false))
    }
}