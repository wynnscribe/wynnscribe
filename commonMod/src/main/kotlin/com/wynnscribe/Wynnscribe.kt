package com.wynnscribe

import com.mojang.blaze3d.platform.InputConstants
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

object Wynnscribe {

    const val MOD_ID = "wynnscribe"

    val P = KeyMapping("key.wynnscribe.p", InputConstants.Type.KEYSYM, InputConstants.KEY_P, "category.wynnscribe.debug")
    val O = KeyMapping("key.wynnscribe.o", InputConstants.Type.KEYSYM, InputConstants.KEY_O, "category.wynnscribe.debug")

    fun init() {
        KeyMappings.register(P, KeyMappings.RegisterType.INVENTORY) {
            val screen = Minecraft.getInstance().screen
            if (screen is AbstractContainerScreen<*>) {
                val serialized = MiniMessage.miniMessage().serialize(MinecraftClientAudiences.of().asAdventure(Minecraft.getInstance().screen!!.getTitle()))
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val selection = StringSelection(serialized)
                clipboard.setContents(selection, selection)
            }
        }

        KeyMappings.register(O, KeyMappings.RegisterType.INVENTORY) {
            try {
                val screen = Minecraft.getInstance().screen
                if (screen is AbstractContainerScreen<*>) {
                    val lore = DeveloperUtils.lastHoveredLore.map(MinecraftClientAudiences.of()::asAdventure)
                        .joinToString("\n", transform = com.wynnscribe.MiniMessage::serialize)

                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    val selection = StringSelection(lore)
                    clipboard.setContents(selection, selection)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}