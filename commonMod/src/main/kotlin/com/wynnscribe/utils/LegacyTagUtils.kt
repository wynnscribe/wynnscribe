package com.wynnscribe.utils

object LegacyTagUtils {
    fun replaceLegacyTags(text: String, remove: Boolean): String {
        return text.replace("```\n", "").replace("\n```", "").replace("```", "")
            .replace("§0", if (remove) "" else "<black>")
            .replace("§1", if (remove) "" else "<dark_blue>")
            .replace("§2", if (remove) "" else "<dark_green>")
            .replace("§3", if (remove) "" else "<dark_aqua>")
            .replace("§4", if (remove) "" else "<dark_red>")
            .replace("§5", if (remove) "" else "<dark_purple>")
            .replace("§6", if (remove) "" else "<gold>")
            .replace("§7", if (remove) "" else "<gray>")
            .replace("§8", if (remove) "" else "<dark_gray>")
            .replace("§9", if (remove) "" else "<blue>")
            .replace("§a", if (remove) "" else "<green>")
            .replace("§b", if (remove) "" else "<aqua>")
            .replace("§c", if (remove) "" else "<red>")
            .replace("§d", if (remove) "" else "<light_purple>")
            .replace("§e", if (remove) "" else "<yellow>")
            .replace("§f", if (remove) "" else "<white>")
            .replace("§k", if (remove) "" else "<obf>")
            .replace("§l", if (remove) "" else "<bold>")
            .replace("§m", if (remove) "" else "<st>")
            .replace("§n", if (remove) "" else "<u>")
            .replace("§o", if (remove) "" else "<i>")
            .replace("§r", if (remove) "" else "<reset>")
    }
}