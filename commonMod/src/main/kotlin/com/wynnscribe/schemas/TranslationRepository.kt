package com.wynnscribe.schemas

import com.wynnscribe.models.Placeholder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@Serializable
data class TranslationRepository(
    val version: Long = 0,
    val data: Translations
) {

    val sourcesMap: MutableMap<String, Translations.Category.Source> = mutableMapOf()

    val sources = mutableListOf<Translations.Category.Source>()

    val categoriesMap: MutableMap<String, Translations.Category> = mutableMapOf()

    val categories = mutableListOf<Translations.Category>()

    val sourcesByParentId = mutableMapOf<String, MutableList<Translations.Category.Source>>()

    init {
        this.data.categories.forEach { category ->
            this.categoriesMap[category.id] = category
            this.categories.add(category)
            category.sources.sortedByDescending { it.properties.priority }.forEach { source ->
                this.sourcesMap[source.id] = source
                this.sources.add(source)
                if(source.parentId != null) {
                    sourcesByParentId.getOrPut(source.parentId) { mutableListOf() }.add(source)
                }
            }
        }
    }

    companion object {

        var loaded: TranslationRepository? = null

        @JvmStatic
        fun getOrNull(): TranslationRepository? {
            return this.loaded
        }

        @JvmStatic
        fun isLoaded(): Boolean {
            return this.loaded != null
        }

        @OptIn(ExperimentalSerializationApi::class)
        @JvmStatic
        fun read(language: String): TranslationRepository? {
            try {
                @Suppress("JAVA_CLASS_ON_COMPANION")
                val stream = javaClass.classLoader.getResourceAsStream("translations/$language.json")
                println("あああ:${"translations/$language.json"}: ${stream}")

                val json = Json { ignoreUnknownKeys = true }

                val translations = json.decodeFromStream<Translations>(stream?:return null)

                return TranslationRepository(data = translations)
            } catch (e: Exception) {
                println("おおお")
                e.printStackTrace()
                return null
            }
        }

        @JvmStatic
        fun load(translationRepository: TranslationRepository) {
            this.loaded = translationRepository
        }
    }

    @Serializable
    data class Translations(
        val languageId: String,
        val name: String,
        val id: String,
        val iconUrl: String? = null,
        val categories: List<Category>
    ) {
        @Serializable
        data class Category(
            val id: String,
            val projectId: String,
            val name: String,
            val properties: Properties = Properties(),
            val sources: List<Source>
        ) {

            val sourcesWithoutChild: List<Source> by lazy { sources.filter { it.parentId == null } }

            @Serializable
            data class Properties(
                val filter: Filter? = null
            )

            @Serializable
            data class Source(
                val id: String,
                val categoryId: String,
                val type: Type,
                val parentId: String? = null,
                val text: String,
                val properties: Properties = Properties(),
                val best: Translation? = null
            ) {

                val regexes: Regexes? by lazy { this.properties.matcher?.let { Regexes(it) } }

                class Regexes(val matcher: String) {
                    private var placeholders = mutableMapOf<Placeholder<*>, Placeholder.Compiled<*>>()

                    fun <T> placeholder(placeholder: Placeholder<T>, holders: List<Placeholder.Compiled.Holder<*>>, source: Source): Placeholder.Compiled<T> {
                        return this.placeholders.getOrPut(placeholder) { placeholder.compile(holders, source) } as Placeholder.Compiled<T>
                    }

                    fun <T> placeholder(placeholder: Placeholder<T>, block: () -> Placeholder.Compiled<T>): Placeholder.Compiled<T> {
                        return this.placeholders.getOrPut(placeholder, block) as Placeholder.Compiled<T>
                    }
                }

                @Serializable
                data class Translation(
                    val id: String,
                    val sourceId: String,
                    val text: String,
                    val status: Status,
                    val score: Int,
                ) {
                    @Serializable
                    enum class Status {
                        Suggested,
                        Approved,
                        NeedsReview,
                        Rejected
                    }
                }

                @Serializable
                data class Properties(
                    val filter: Filter? = null,
                    val matcher: String? = null,
                    val stopOnMatch: Boolean = false,
                    val priority: Int = 0,
                )

                @Serializable
                enum class Type {
                    Text,
                    Image,
                    Audio
                }
            }
        }
    }
}