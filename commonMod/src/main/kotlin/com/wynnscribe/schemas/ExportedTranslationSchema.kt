package com.wynnscribe.schemas

import com.wynnscribe.models.Placeholder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ExportedTranslationSchema(
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

fun main() {
    Json { ignoreUnknownKeys = true }.decodeFromString<ExportedTranslationSchema>(File("test.json").readText())
}