package com.wynnscribe.utils

import java.util.concurrent.atomic.AtomicInteger

private fun isEmpty(l: String): Boolean {
    return l == "<!italic><gray> " || l == "<!italic><gray>" ||
            l == "<!italic><dark_purple> " || l == "<!italic><dark_purple>" || l == "<!italic><dark_gray> " || l == "<!italic><dark_gray>" ||
            l.endsWith(" Archetype")
}

fun extractAbilityDescription(msg: String): Pair<String, Map<String, String>> {
    var empty = 0
    val lore = mutableListOf<String>()
    for (line in msg.lines()) {
        if (isEmpty(line)) {
            empty++
        }
        if (empty >= 1) {
            if (line.contains(": ")) {
                break
            }
            lore.add(line)
        }
    }

    val placeholders = mutableMapOf<String, String>()

    val preFormatted = mutableListOf<String>()
    var endedEmpty = false
    for (line in lore) {
        if (isEmpty(line)) {
            if (endedEmpty) {
                preFormatted.add(line)
            } else {
                continue
            }
        } else {
            endedEmpty = true
            preFormatted.add(line)
        }
    }

    val formatted = mutableListOf<String>()
    endedEmpty = false
    for (line in preFormatted.reversed()) {
        if (isEmpty(line)) {
            if (endedEmpty) {
                formatted.add(line)
            } else {
                continue
            }
        } else {
            endedEmpty = true
            formatted.add(line)
        }
    }

    val description = formatted.reversed().joinToString("\n")

    val textCounter = AtomicInteger(0)

    val textReplacerLambda = { m: MatchResult ->
        val group1 = m.groups[1]?.value
            if(group1 != null) {
                group1
            } else {
                val count = textCounter.getAndIncrement() + 1
                placeholders["{${count}}"] = m.value
                "{${count}}"
            }
    }

    val regexPattern = """(<#[0-9a-fA-F]+>)|([+-]?\d+(?:\.\d+)?)"""
    val regex = Regex(regexPattern)

    val finalResult = regex.replace(description, textReplacerLambda)

    return finalResult to placeholders
}