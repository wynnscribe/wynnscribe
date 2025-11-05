package com.wynnscribe.utils

import com.wynnscribe.MiniMessage
import com.wynnscribe.schemas.ValueProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

fun Component.toString(properties: List<ValueProperty>): String {
    if(ValueProperty.WITH_COLOR in properties) {
        // with color
        return MiniMessage.serialize(this)
    } else {
        // 色なし
        return LegacyTagUtils.replaceLegacyTags(PlainTextComponentSerializer.plainText().serialize(this), true)
    }
}