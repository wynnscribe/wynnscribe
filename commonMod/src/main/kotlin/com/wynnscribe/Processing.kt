package com.wynnscribe

import net.minecraft.client.Minecraft

object Processing {

    val EXTRACT_NUMBERS_REGEX = Regex("(<#[0-9a-fA-F]+>|<click:.+?>)|([+-]?\\d+(?:\\.\\d+)?)")

    val DIALOGUE_PROGRESS_REGEX = Regex("(\\[\\d+/\\d+])")

    val CLICK_TAG_REGEX = Regex("(<click:.+?>)")

    val COMMON_START_TAGS_REGEX = Regex("(<!?italic><!?underlined><!?strikethrough><!?bold><!?obfuscated><font:.+?>)")
    val COMMON_END_TAGS_REGEX = Regex("(</font></!?obfuscated></!?bold></!?strikethrough></!?underlined></!?italic>)")

    val GROUPING_TAG_REGEX = Regex("(</[^>]+>)|(<[^/].*?>)|([^<]+)")

    fun preprocessing(message: String, resetPerLines: Boolean = true, newLineCode: String = "\n", replaceNumbers: Boolean = true): Pair<String, Map<String, String>> {
        val matchedPairs = mutableMapOf<String, String>()
        var msg = message.replace(Minecraft.getInstance().user?.name?:"", "{playername}")
        val progress = DIALOGUE_PROGRESS_REGEX.find(msg)?.groupValues?.getOrNull(1)
        msg = progress?.let { msg.replaceFirst(it, "{progress}") }?:msg
        matchedPairs["{progress}"] = progress?:""

        var clickTagCounter = 0
        CLICK_TAG_REGEX.findAll(msg).distinctBy { it.value }.forEach { matchResult ->
            msg = msg.replace(matchResult.value, "<click:${++clickTagCounter}>")
            matchedPairs["<click:${clickTagCounter}>"] = matchResult.value
        }

        if(replaceNumbers) {
            var textCounter = 0
            msg = EXTRACT_NUMBERS_REGEX.replace(msg) { matchResult ->
                val group1Value = matchResult.groupValues[1]
                group1Value.ifEmpty {
                    matchedPairs["{${++textCounter}}"] = matchResult.value
                    "{$textCounter}"
                }
            }
        }

        var tagCounter = 0
        COMMON_START_TAGS_REGEX.findAll(msg).distinctBy { it.value }.forEach { matchResult ->
            msg = msg.replace(matchResult.value, "<t${++tagCounter}>")
            matchedPairs["<t${tagCounter}>"] = matchResult.value
        }
        var closedTagCounter = 0
        COMMON_END_TAGS_REGEX.findAll(msg).distinctBy { it.value }.forEach { matchResult ->
            msg = msg.replace(matchResult.value, "</t${++closedTagCounter}>")
            matchedPairs["</t${closedTagCounter}>"] = matchResult.value
        }

        val groupedStartTags = mutableListOf<String>()
        val groupedEndTags = mutableListOf<String>()

        val startTags = mutableListOf<MutableList<String>>()
        val endTags = mutableListOf<String>()
        var last = "start"

        val lines = msg.split("\n", "<br/>").map { oldLine ->
            var line = oldLine

            if(resetPerLines) {
                startTags.clear()
                endTags.clear()
                last = "start"
            }

            fun onEndTag() {
                if(endTags.isEmpty()) return
                val tagsList = mutableListOf<String>()
                repeat(endTags.size) {
                    tagsList.add(startTags.last().removeLast())
                }
                if(startTags.last().isEmpty()) {
                    startTags.removeLast()
                }
                tagsList.reverse()
                val startTagsGroup = tagsList.joinToString("")
                val endTagsGroup = endTags.joinToString("")
                endTags.clear()
                if(!groupedStartTags.contains(startTagsGroup)) {
                    groupedStartTags.add(startTagsGroup)
                }
                if(!groupedEndTags.contains(endTagsGroup)) {
                    groupedEndTags.add(endTagsGroup)
                }
                line = line.replaceFirst(startTagsGroup, "<s${groupedStartTags.indexOf(startTagsGroup) + 1}>")
                line = line.replaceFirst(endTagsGroup, "</s${groupedEndTags.indexOf(endTagsGroup) + 1}>")
            }

            GROUPING_TAG_REGEX.findAll(oldLine).forEach { matchResult ->
                val start = matchResult.groupValues[2].ifEmpty { null }
                val end = matchResult.groupValues[1].ifEmpty { null }

                if(start != null) {
                    if(last == "end") {
                        onEndTag()
                    }
                    if(last == "start") {
                        val tagsList = startTags.removeLastOrNull()?:mutableListOf()
                        tagsList.add(start)
                        startTags.add(tagsList)
                    } else {
                        val tagsList = mutableListOf<String>()
                        tagsList.add(start)
                        startTags.add(tagsList)
                    }
                    last = "start"
                } else if(end != null) {
                    last = "end"
                    endTags.add(end)
                    if(startTags.last().size == endTags.size) {
                        onEndTag()
                    }
                } else {
                    if(last == "end") {
                        onEndTag()
                    }
                    last = "value"
                }
            }
            if (last == "end") {
                onEndTag()
            }

            return@map line
        }

        groupedStartTags.forEachIndexed { index, s ->
            matchedPairs["<s${index + 1}>"] = s
        }
        groupedEndTags.forEachIndexed { index, s ->
            matchedPairs["</s${index + 1}>"] = s
        }

        msg = lines.joinToString(newLineCode)

        return msg to matchedPairs.toList().reversed().toMap()
    }

    fun postprocessing(message: String, replacedPair: Map<String, String>): String {
        var msg = message.replace("{playername}", Minecraft.getInstance().user?.name?:"")
        replacedPair.forEach { (key, value) -> msg = msg.replace(key, value) }
        msg = msg.replace("<br/>", "\n")
        return msg
    }
}