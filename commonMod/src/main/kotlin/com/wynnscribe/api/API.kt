package com.wynnscribe.api

import com.wynnscribe.Config
import com.wynnscribe.Translator
import com.wynnscribe.schemas.ExportedTranslationSchema
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.client.Minecraft
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

object API {
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(30))
        .readTimeout(Duration.ofSeconds(30))
        .writeTimeout(Duration.ofSeconds(30))
        .callTimeout(Duration.ofSeconds(30))
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-Wynnscribe-Requester", "Mod")
                    .build()
            )
        }
        .build()

    val translationsDir = File("translations")

    val json = Json { ignoreUnknownKeys = true }

    private var accountToken: String? = null
    private var expiresAt: Instant = Clock.System.now()

    fun isLoaded(): Boolean {
        return Translator.Translation != null
    }

    fun loadOrDownloadTranslations(language: String): TranslationData? {
        val file = translationsDir.resolve("${language}.json")
        if(file.exists()) {
            val cached = json.decodeFromString<TranslationData>(file.readText())
            if(cached.at > Clock.System.now().minus(3.hours)) { return cached }
        }
        val downloaded = downloadTranslations(language) ?: return null
        if(!translationsDir.exists()) { translationsDir.mkdirs() }
        val translationData = TranslationData(data = downloaded)
        file.writeText(json.encodeToString(translationData))
        return translationData
    }

    val JsonType = "application/json".toMediaType()

    fun generateAccountToken(accessToken: String) {
        val request = Request.Builder().url("https://api.wynnscribe.com/api/v1/verify/minecraft")
            .post(json.encodeToString(AccountToken.Request(accessToken = accessToken)).toRequestBody(JsonType)).build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string()
        if(body != null) {
            this.accountToken = json.decodeFromString<AccountToken.Response>(body).token
            this.expiresAt = Clock.System.now().plus(23.hours)
        }
    }

    fun getAccountToken(): String? {
        if(this.accountToken == null || this.expiresAt < Clock.System.now()) {
            this.generateAccountToken(Minecraft.getInstance().user?.accessToken?:return null)
            return this.accountToken
        }
        return this.accountToken
    }

    object Gemini {

        @Serializable
        data class TranslateDialogueRequest(
            val text: String,
            val plain: String?,
            val speaker: String?,
            val quest: String?,
            val target: String,
            val progress: String?,
            val history: List<String>,
        )

        fun dialogue(body: TranslateDialogueRequest, on: (data: String?, done: Boolean) -> Unit) {
            val request = Request.Builder().url(URLs.QUEST_AI_TRANSLATIONS).header("x-minecraft-authorization", getAccountToken()?:return).post(json.encodeToString(body).toRequestBody(JsonType)).build()
            requestStream(request, on)
        }

        @Serializable
        data class TranslateAbilityRequest(
            val text: String,
            val plain: String?,
            val target: String,
            val characterClass: String
        )

        fun ability(body: TranslateAbilityRequest, on: (data: String?, done: Boolean) -> Unit) {
            val request = Request.Builder().url(URLs.ABILITY_AI_TRANSLATIONS).header("x-minecraft-authorization", getAccountToken()?:return).post(json.encodeToString(body).toRequestBody(JsonType)).build()
            requestStream(request, on)
        }

        @Serializable
        data class TranslateGeneralsRequest(
            val text: String,
            val plain: String?,
            val target: String,
        )

        fun generals(body: TranslateGeneralsRequest, on: (data: String?, done: Boolean) -> Unit) {
            val request = Request.Builder().url(URLs.GENERALS_AI_TRANSLATIONS).header("x-minecraft-authorization", getAccountToken()?:return).post(json.encodeToString(body).toRequestBody(JsonType)).build()
            requestStream(request, on)
        }

        @Serializable
        data class SSEBody(val text: String? = null, val done: Boolean = false)

        fun requestStream(request: Request, on: (data: String?, done: Boolean) -> Unit) {
            val start = System.currentTimeMillis()
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    if(!response.isSuccessful) {
                        response.close()
                        return
                    }
                    val contentType = response.header("Content-Type")
                    if(contentType != null && contentType.startsWith("text/event-stream")) {
                        response.use { response ->
                            val body = response.body ?: return
                            val source = body.source()
                            try {
                                while (!source.exhausted()) {
                                    val line = source.readUtf8Line()
                                    if(line.isNullOrEmpty()) { continue }
                                    when {
                                        line.startsWith("data:") -> {
                                            val data = line.removePrefix("data: ")
                                            val serialized = json.decodeFromString<SSEBody>(data)
                                            on(serialized.text, serialized.done)
                                            if(serialized.done) break
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else {
                        response.use { response ->
                            val body = response.body?.string()
                            if(body != null) {
                                on(body, true)
                            }
                        }
                    }
                }

            })
        }
    }

    fun downloadTranslations(language: String): ExportedTranslationSchema? {
        val request = Request.Builder().url("https://api.wynnscribe.com/api/v1/downloads/${language}.json").header("x-minecraft-authorization", getAccountToken()?:return null).get().build()
        val response = httpClient.newCall(request).execute()
        if(response.isSuccessful) {
            val body = response.body?.string()
            if(body != null) {
                return json.decodeFromString(body)
            }
        }
        return null
    }

    class AccountToken() {
        @Serializable
        data class Request(
            @SerialName("accessToken")
            val accessToken: String
        )

        @Serializable
        data class Response(
            val token: String
        )
    }

    class AITranslation() {
        @Serializable
        data class Request(
            val text: String,
            val plain: String?,
            val speaker: String?,
            val quest: String?,
            val type: Type,
            val history: List<String>?,
            val target: String,
            val progress: String?,
            val characterClass: String?
        )

        @Serializable
        enum class Type {
            @SerialName("dialog")
            DIALOG,
            @SerialName("lore")
            LORE,
            @SerialName("ability")
            ABILITY
        }

        @Serializable
        data class Response(
            val translated: String
        )
    }

    @Serializable
    data class TranslationData(
        val data: ExportedTranslationSchema,
        val at: Instant = Clock.System.now()
    ) {
        val sourcesMap: MutableMap<String, ExportedTranslationSchema.Category.Source> = mutableMapOf()

        val sources = mutableListOf<ExportedTranslationSchema.Category.Source>()

        val categoriesMap: MutableMap<String, ExportedTranslationSchema.Category> = mutableMapOf()

        val categories = mutableListOf<ExportedTranslationSchema.Category>()

        val sourcesByParentId = mutableMapOf<String, MutableList<ExportedTranslationSchema.Category.Source>>()

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
    }
}