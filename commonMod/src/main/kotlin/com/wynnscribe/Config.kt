package com.wynnscribe

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import java.util.Properties

object Config {
    val AI_ENABLED: Boolean

    val enabled: Boolean get() = !Screen.hasShiftDown()

    init {
        val props = Properties()
        var aiFlag = false

        try {
            val stream = javaClass.classLoader.getResourceAsStream("wynnscribe.config.properties")
            if(stream != null) {
                stream.use { props.load(it) }
                aiFlag = props.getProperty("AI_ENABLED", "false").toBooleanStrictOrNull()?:false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        AI_ENABLED = aiFlag
    }
}