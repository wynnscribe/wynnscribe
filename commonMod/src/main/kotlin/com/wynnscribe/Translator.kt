package com.wynnscribe

import com.wynnscribe.mixins.CachedItemStackTranslation.Companion.cachedTranslation
import com.wynnscribe.mixins.CachedItemStackTranslation.Companion.setCacheTranslation
import com.wynnscribe.schemas.TranslationRepository
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

object Translator {
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
        val target = Minecraft.getInstance().languageManager.selected
        val progress = messagePairs["{progress}"]?.ifBlank { null }?.ifEmpty { null }

        if(groupValues != null) {
            speaker = groupValues[1]
            plain = groupValues[2]
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
            completableFuture.complete(styledTexts)
            return completableFuture
        }
    }

    fun translateActivity(components: MutableList<Component>) {
        val activity = components.map(MinecraftClientAudiences.of()::asAdventure)
        val (plain,plainPairs) = Processing.preprocessing(activity.joinToString("\n", transform = PlainTextSerializer::serialize))
        val (message, messagePairs) = Processing.preprocessing(MiniMessage.serializeList(activity))
        val light = message.endsWith(plain)

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
        }
    }

    fun translateTextDisplay(component: Component): Component {
        if(!TranslationRepository.isLoaded()) { return component }
        val text = MiniMessage.serialize(MinecraftClientAudiences.of().asAdventure(component))
        if(text.isEmpty()) return component
//        println("テキストディスプレイ！！！")
//        println(text)
        val translated = this.translate(text, TEXT_DISPLAY_TYPE_MAP, struct = StructMode.StructCategory)
        return MinecraftClientAudiences.of().asNative(MiniMessage.deserialize(translated))
    }

    fun translateChat(component: Component): Component {
        if(!TranslationRepository.isLoaded()) { return component }
        val (text, tags) = Processing.preprocessing(MiniMessage.serialize(MinecraftClientAudiences.of().asAdventure(component)), resetPerLines = false, newLineCode = "\n")
        if(text.isEmpty()) return component
//        println("チャット！！！ ==== ")
//        println(text)
        val translated = this.translate(text, CHAT_TYPE_MAP, struct = StructMode.StructCategory)
        return MinecraftClientAudiences.of().asNative(MiniMessage.deserialize(Processing.postprocessing(translated, tags)))
    }

    @Synchronized
    fun translateAbilityOrCached(itemStack: ItemStack, source: List<Component>): List<Component> {
        val translationRepository = TranslationRepository.getOrNull()?:return source
        val content = MiniMessage.serializeList(source.map(MinecraftClientAudiences.of()::asAdventure))
        val cached = itemStack.cachedTranslation(content, refreshed = translationRepository.version)
        if(cached != null) {
            return cached
        }
        if(content.length < 10) {
            return source
        }
        itemStack.setCacheTranslation(content, translationRepository.version, source + Component.empty() + Component.literal("翻訳中..."))
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
            itemStack.setCacheTranslation(content, translationRepository.version, translated.split("\n").map(MiniMessage::deserialize).map(MinecraftClientAudiences.of()::asNative))
        } else {
            itemStack.setCacheTranslation(content, translationRepository.version, source)
            return source
        }
        return source
    }

    fun translateItemStackOrCached(itemStack: ItemStack, source: List<Component>, type: String?): List<Component> {
        val translationRepository = TranslationRepository.getOrNull()?:return source
        val content = MiniMessage.serializeList(source.map(MinecraftClientAudiences.of()::asAdventure))
        val cached = itemStack.cachedTranslation(content, refreshed = translationRepository.version)
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
        itemStack.setCacheTranslation(content, refreshed = translationRepository.version, translated)
        return translated
    }

    /**
     * @struct trueの場合filterが厳格になります。
     */
    fun filtered(categories: List<TranslationRepository.Translations.Category>, filterValue: FilterValue, struct: StructMode): List<TranslationRepository.Translations.Category.Source> {
        return categories
            .filter { it.properties.filter?.match(filterValue.fields)?:struct.category }
            .map { it.sourcesWithoutChild.filter { it.properties.filter?.match(filterValue.fields)?:struct.source } }
            .flatten().sortedByDescending { it.properties.priority }
    }

    fun translate(originalText: String, sources: List<TranslationRepository.Translations.Category.Source>, translationRepository: TranslationRepository, struct: StructMode): String {
        var translated = originalText

        for(source in sources) {
            // 翻訳前の文章を残しておく
            val old = translated
            // 一番投票数が多い翻訳を取得
            val translationText = source.best?.text?:source.text
            // 文字列を置換する
            try {
                translated = Placeholders.pattern(source, translationRepository.categories, struct = struct)?.replace(translated, translationText)?:if(source.text.isBlank()) translated  else translated.replace(source.text, translationText)
            } catch (e: Exception) {
                e.printStackTrace()
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            }
            // replaceが動作したかどうか
            if(old != translated) {
                // プレースホルダ関連を処理
                translated = Placeholders.on(translated, originalText, source, translationRepository.categories, struct = struct)
                // children関連を処理
                translated = translate(translated, translationRepository.sourcesByParentId[source.id]?:listOf(), translationRepository, struct = struct)
            }
            if(source.properties.stopOnMatch && translated != old) {
                break
            }
        }

        return translated
    }

    fun translate(originalText: String, filterValue: FilterValue, struct: StructMode): String {
        val translationRepository = TranslationRepository.getOrNull()?:return originalText
        var translated = originalText
        val sources = filtered(translationRepository.categories, filterValue, struct = struct)
        translated = translate(translated, sources, translationRepository, struct = struct)
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