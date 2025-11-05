package com.wynnscribe

import com.wynnscribe.api.API
import com.wynnscribe.mixins.CachedItemStackTranslation.Companion.cachedTranslation
import com.wynnscribe.mixins.CachedItemStackTranslation.Companion.setCacheTranslation
import com.wynnscribe.schemas.ExportedTranslationSchema
import com.wynnscribe.utils.extractAbilityDescription
import com.wynntils.core.components.Models
import com.wynntils.core.text.StyledText
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import kotlin.collections.map
import kotlin.concurrent.thread

object Translator {

    var Translation: API.TranslationData? = null

    val PlainTextSerializer = PlainTextComponentSerializer.plainText()

    private fun sha256(input: String): String {
        val hexChars = "0123456789ABCDEF"
        val bytes = MessageDigest
            .getInstance("sha256")
            .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }

    val caches = mutableMapOf<String, CompletableFuture<List<StyledText>>>()

    var history: List<String> = listOf()

    private val TEXT_DISPLAY_TYPE_MAP = FilterValue(mapOf("type" to net.kyori.adventure.text.Component.text("#wynnscribe.textdisplay")))
    private val CHAT_TYPE_MAP = FilterValue(mapOf("type" to net.kyori.adventure.text.Component.text("#wynnscribe.chat")))
    private val ABILITY_TYPE_MAP = FilterValue(mapOf("type" to net.kyori.adventure.text.Component.text("#wynnscribe.ability")))

    fun translateNpcDialogue(styledTexts: List<StyledText>): CompletableFuture<List<StyledText>> {
        val completableFuture = CompletableFuture<List<StyledText>>()

        val dialogue = styledTexts.map(StyledText::getComponent).map(MinecraftClientAudiences.of()::asAdventure)
        var (plain,plainPairs) = Processing.preprocessing(dialogue.joinToString("\n", transform = PlainTextSerializer::serialize))
        val (message, messagePairs) = Processing.preprocessing(MiniMessage.serializeList(dialogue))

        val dialogRegex = Regex("(?:\\{progress}|)(?: |)(.+?): (.+)")
        val groupValues = dialogRegex.find(plain)?.groupValues
        var speaker: String? = null
        var quest: String? = null
        val target = Minecraft.getInstance().languageManager.selected
        val progress = messagePairs["{progress}"]?.ifBlank { null }?.ifEmpty { null }

        if(groupValues != null) {
            speaker = groupValues[1]
            plain = groupValues[2]
        }
        if(Models.Activity.isTracking) {
            quest = Models.Activity.trackedName
        }

        val cacheId = "${target}:${"dialog"}:${speaker?:"none"}:${progress?:"none"}:${sha256(message)}"

        val cached = this.caches[cacheId]

        if(cached != null) {
            return cached
        } else {
            this.caches[cacheId] = completableFuture
        }

        val light = message.endsWith(plain)

        val key = "dialogue:${progress?.lowercase()?:"none"}:${speaker?.lowercase()?:"none"}:${sha256(if(light) plain else message).lowercase()}"

        val filterValue = FilterValue(
            mapOf(
                "type" to net.kyori.adventure.text.Component.text("#wynnscribe.dialog"),
                "dialog_key" to net.kyori.adventure.text.Component.text(key)
            )
        )
        var translated = this.translate(if(light) plain else message, filterValue, struct = StructMode.StructCategory)
        if(translated != if(light) plain else message) {
            if (light) {
                translated = message.replace(plain, translated)
            }
            val translatedComponent = Processing.postprocessing(translated, messagePairs).split("\n").map(MiniMessage::deserialize)
            val result = translatedComponent.map(MinecraftClientAudiences.of()::asNative).map(StyledText::fromComponent)
            completableFuture.complete(result)
            return completableFuture
        } else {

            if(!Config.AI_ENABLED) {
                completableFuture.complete(styledTexts)
                return completableFuture
            }

            CompletableFuture.runAsync {
                try {
                    val historyElement: String = if (light) { speaker?.let { "${it}: $plain" } ?: plain } else { plain }
                    this.history = history.take(19) + historyElement

                    val body = API.Gemini.TranslateDialogueRequest(
                        text = if(light) plain else message,
                        plain = plain,
                        speaker = speaker,
                        quest = quest,
                        target = target,
                        progress = progress,
                        history = this.history.takeLast(20)
                    )

                    var received = ""
                    var translatedComponent: List<net.kyori.adventure.text.Component>
                    var result: List<StyledText>

                    API.Gemini.dialogue(body) { text, done ->
                        if(text != null) { received += text }
                        var translated = if(received.count { it == '>' } == received.count { it == '<' }) { received } else { received.take(received.lastIndexOf(">") + 1) }
                        if (light) {
                            translated = message.replace(plain, translated)
                        }
                        translatedComponent = Processing.postprocessing(translated, messagePairs).split("\n").map(MiniMessage::deserialize)
                        result = translatedComponent.map(MinecraftClientAudiences.of()::asNative).map(StyledText::fromComponent)
                        if(done) {
                            this.history = this.history.dropLast(1)
                            this.history = this.history.takeLast(19) + translatedComponent.joinToString("\n", transform = PlainTextSerializer::serialize)
                            completableFuture.complete(result)
                        } else {
                            completableFuture.obtrudeValue(result)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    completableFuture.complete(styledTexts)
                }
            }
        }

        return completableFuture
    }

    fun translateActivity(components: MutableList<Component>) {
        val activity = components.map(MinecraftClientAudiences.of()::asAdventure)
        val (plain,plainPairs) = Processing.preprocessing(activity.joinToString("\n", transform = PlainTextSerializer::serialize))
        val (message, messagePairs) = Processing.preprocessing(MiniMessage.serializeList(activity))
        var quest: String? = null
        val light = message.endsWith(plain)
        val target = Minecraft.getInstance().languageManager.selected
        if(Models.Activity.isTracking) {
            quest = Models.Activity.trackedName
        }

        val key = "dialogue:${"none"}:${"activity"}:${sha256(if(light) plain else message).lowercase()}"

        val filterValue = FilterValue(
            mapOf(
                "type" to net.kyori.adventure.text.Component.text("#wynnscribe.dialog"),
                "dialog_key" to net.kyori.adventure.text.Component.text(key)
            )
        )
        var translated = this.translate(if(light) plain else message, filterValue, struct = StructMode.StructCategory)

        if(translated != if(light) plain else message) {
            if (light) {
                translated = message.replace(plain, translated)
            }
            val translatedComponent = Processing.postprocessing(translated, messagePairs).split("\n").map(MiniMessage::deserialize)
            val result = translatedComponent.map(MinecraftClientAudiences.of()::asNative)
            components.clear()
            components.addAll(result)
        } else {
            if(!Config.AI_ENABLED) { return }
            val body = API.Gemini.TranslateDialogueRequest(
                text = if(light) plain else message,
                plain = plain,
                speaker = "Activity",
                quest = quest,
                target = target,
                progress = null,
                history = emptyList()
            )

            var received = ""
            var translatedComponent: List<net.kyori.adventure.text.Component>
            var result: List<Component>

            API.Gemini.dialogue(body) { text, _ ->
                if(text != null) { received += text }
                var translated = if(received.count { it == '>' } == received.count { it == '<' }) received else received.take(received.lastIndexOf(">") + 1)
                if (light) {
                    translated = message.replace(plain, translated)
                }
                translatedComponent = Processing.postprocessing(translated, messagePairs).split("\n").map(MiniMessage::deserialize)
                result = translatedComponent.map(MinecraftClientAudiences.of()::asNative)
                components.clear()
                components.addAll(result)
            }
        }
    }

    fun translateTextDisplay(component: Component): Component {
        if(!API.isLoaded()) { return component }
        val text = MiniMessage.serialize(MinecraftClientAudiences.of().asAdventure(component))
        if(text.isEmpty()) return component
//        println("テキストディスプレイ！！！")
//        println(text)
        val translated = this.translate(text, TEXT_DISPLAY_TYPE_MAP, struct = StructMode.StructCategory)
        return MinecraftClientAudiences.of().asNative(MiniMessage.deserialize(translated))
    }

    fun translateChat(component: Component): Component {
        if(!API.isLoaded()) { return component }
        val (text, tags) = Processing.preprocessing(MiniMessage.serialize(MinecraftClientAudiences.of().asAdventure(component)), resetPerLines = false, newLineCode = "\n")
        if(text.isEmpty()) return component
//        println("チャット！！！ ==== ")
//        println(text)
        val translated = this.translate(text, CHAT_TYPE_MAP, struct = StructMode.StructCategory)
        return MinecraftClientAudiences.of().asNative(MiniMessage.deserialize(Processing.postprocessing(translated, tags)))
    }

    @Synchronized
    fun translateAbilityOrCached(itemStack: ItemStack, source: List<Component>): List<Component> {
        val content = MiniMessage.serializeList(source.map(MinecraftClientAudiences.of()::asAdventure))
        val cached = itemStack.cachedTranslation(content, refreshed = Translation!!.at.epochSeconds)
        if(cached != null) {
            return cached
        }
        if(content.length < 10) {
            return source
        }
        itemStack.setCacheTranslation(content, Translation!!.at.epochSeconds, source + Component.empty() + Component.literal("翻訳中..."))
        var (rawAbilityDescription, rawDescriptionHolders) = extractAbilityDescription(content)
        var (abilityDescription, descriptionHolders) = Processing.preprocessing(rawAbilityDescription.split("\n").joinToString("\n") { if(it.startsWith("<!italic><dark_purple>")) { it.replaceFirst("<!italic><dark_purple>", "") } else it }, resetPerLines = true, newLineCode = "\n", replaceNumbers = false)
        descriptionHolders = descriptionHolders + rawDescriptionHolders
        var translatedDescription = this.translate(abilityDescription, ABILITY_TYPE_MAP, struct = StructMode.StructCategory)
        if(abilityDescription != translatedDescription) {
            descriptionHolders.forEach { (t, u) ->
                rawAbilityDescription = rawAbilityDescription.replace(t, u)
                translatedDescription = translatedDescription.replace(t, u)
            }
            translatedDescription = translatedDescription.split("\n").joinToString("\n") { "<!italic><gray>${it}" }
            val translated = this.translate(content.replace(rawAbilityDescription, translatedDescription), ABILITY_TYPE_MAP, struct = StructMode.StructCategory)
            itemStack.setCacheTranslation(content, Translation!!.at.epochSeconds, translated.split("\n").map(MiniMessage::deserialize).map(MinecraftClientAudiences.of()::asNative))
        } else {
            if(!Config.AI_ENABLED) {
                itemStack.setCacheTranslation(content, Translation!!.at.epochSeconds, source)
                return source
            }

            val body = API.Gemini.TranslateAbilityRequest(
                text = abilityDescription,
                plain = null,
                target = Minecraft.getInstance().languageManager.selected,
                characterClass = Models.Character.classType.getActualName(false)
            )

            var received = ""

            thread(start = true) {
                API.Gemini.ability(body) { text, _ ->
                    if(text != null) { received += text }
                    translatedDescription = if(received.count { it == '>' } == received.count { it == '<' }) { received } else { received.take(received.lastIndexOf(">") + 1) }

                    translatedDescription = Processing.postprocessing(translatedDescription, descriptionHolders)
                    descriptionHolders.forEach { (t, u) ->
                        rawAbilityDescription = rawAbilityDescription.replace(t, u)
                        translatedDescription = translatedDescription.replace(t, u)
                    }
                    translatedDescription = translatedDescription.split("\n").joinToString("\n") { "<!italic><gray>${it}" }
                    val translated = this.translate(content.replace(rawAbilityDescription, translatedDescription), ABILITY_TYPE_MAP, struct = StructMode.StructCategory)
                    itemStack.setCacheTranslation(content, Translation!!.at.epochSeconds, translated.split("\n").map(MiniMessage::deserialize).map(MinecraftClientAudiences.of()::asNative))
                }
            }
        }
        return source
    }

    fun translateItemStackOrCached(itemStack: ItemStack, source: List<Component>, type: String?): List<Component> {
        if(!API.isLoaded()) { return source }
        val content = MiniMessage.serializeList(source.map(MinecraftClientAudiences.of()::asAdventure))
        val cached = itemStack.cachedTranslation(content, refreshed = Translation!!.at.epochSeconds)
        if(cached != null) {
            // キャッシュがあった場合はそのItemStackを使う
            return cached
        }
        val fields = mutableMapOf<String, net.kyori.adventure.text.Component>()
        val inventoryName = Minecraft.getInstance().screen?.title
        if(inventoryName != null) {
            fields["inventoryName"] = MinecraftClientAudiences.of().asAdventure(inventoryName)
        }
        val displayName = source.firstOrNull()
        if(displayName != null) {
            fields["displayName"] = MinecraftClientAudiences.of().asAdventure(displayName)
        }
        if(type != null) {
            fields["type"] = net.kyori.adventure.text.Component.text(type)
        }

        val filterValue = FilterValue(fields)
        val translated =  this.translate(content, filterValue, struct = StructMode.None).split("\n").map(MiniMessage::deserialize).map(MinecraftClientAudiences.of()::asNative)
        // ItemStackに翻訳のキャッシュを残す
        itemStack.setCacheTranslation(content, refreshed = Translation!!.at.epochSeconds, translated)
        return translated
    }

    /**
     * @struct trueの場合filterが厳格になります。
     */
    fun filtered(categories: List<ExportedTranslationSchema.Category>, filterValue: FilterValue, struct: StructMode): List<ExportedTranslationSchema.Category.Source> {
        return categories
            .filter { it.properties.filter?.match(filterValue.fields)?:struct.category }
            .map { it.sourcesWithoutChild.filter { it.properties.filter?.match(filterValue.fields)?:struct.source } }
            .flatten().sortedByDescending { it.properties.priority }
    }

    fun translate(originalText: String, sources: List<ExportedTranslationSchema.Category.Source>, translationData: API.TranslationData, struct: StructMode): String {
        var translated = originalText

        for(source in sources) {
            // 翻訳前の文章を残しておく
            val old = translated
            // 一番投票数が多い翻訳を取得
            val translationText = source.best?.text?:source.text
            // 文字列を置換する
            try {
                translated = Placeholders.pattern(source, translationData.categories, struct = struct)?.replace(translated, translationText)?:if(source.text.isBlank()) translated  else translated.replace(source.text, translationText)
            } catch (e: Exception) {
                println("===")
                println(translated)
                println(source)
                println(Placeholders.pattern(source, translationData.categories, struct = struct))
                e.printStackTrace()
            } catch (e: OutOfMemoryError) {
                println("=== いいい ===")
                println(translated)
                println("=== ううう ===")
                println(source)
                println("=== えええ ===")
                println(Placeholders.pattern(source, translationData.categories, struct = struct))
                e.printStackTrace()
            }
            // replaceが動作したかどうか
            if(old != translated) {
                // プレースホルダ関連を処理
                translated = Placeholders.on(translated, originalText, source, translationData.categories, struct = struct)
                // children関連を処理
                translated = translate(translated, translationData.sourcesByParentId[source.id]?:listOf(), translationData, struct = struct)
            }
            if(source.properties.stopOnMatch && translated != old) {
                break
            }
        }

        return translated
    }

    fun translate(originalText: String, filterValue: FilterValue, struct: StructMode): String {
        val translationData = Translation?:return originalText
        var translated = originalText
        val sources = filtered(Translation?.categories?:return originalText, filterValue, struct = struct)
        translated = translate(translated, sources, translationData, struct = struct)
        return translated
    }

    /**
     * フィルターにかける値を保管したクラスです。
     */
    data class FilterValue(
        val fields: Map<String, net.kyori.adventure.text.Component>
    )

    enum class StructMode(val category: Boolean, val source: Boolean) {
        None(true, true),
        StructCategory(false, true),
        StructSource(true, false),
        StructBoth(false, false)
    }
}