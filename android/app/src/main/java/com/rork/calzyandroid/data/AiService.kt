package com.rork.calzyandroid.data

import com.rork.calzyandroid.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Structured nutrition estimate returned by the model. */
@Serializable
data class AnalysisItem(
    val name: String,
    val quantity: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

@Serializable
data class AnalysisResult(
    val title: String,
    val isFood: Boolean,
    val healthScore: Int,
    val items: List<AnalysisItem>,
    val quip: String? = null,
)

enum class AiErrorKind(val message: String) {
    NotConfigured("AI scanning isn't available in this build yet."),
    ImageTooLarge("That photo is too large. Try taking a new one."),
    AuthError("AI features are currently unavailable. Please restart the app."),
    InsufficientBalance("AI features are temporarily unavailable. Please try again later."),
    RateLimited("Too many scans at once. Wait a moment and try again."),
    NotFood("We couldn't find any food in that photo. Try again with better lighting."),
    BadResponse("We couldn't read that result. Please try again."),
    ServerError("Something went wrong. Please try again."),
}

class NutritionAiException(val kind: AiErrorKind) : Exception(kind.message)

/** Meal recognition via the Rork Toolkit AI gateway (Vercel AI Gateway proxy). */
object AiService {

    private const val MODEL = "google/gemini-3-flash"
    private val FALLBACK_MODELS = listOf("anthropic/claude-haiku-4.5", "openai/gpt-5-mini")

    private val json = Json { ignoreUnknownKeys = true }

    private val client: HttpClient by lazy {
        HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = 90_000
                connectTimeoutMillis = 20_000
            }
        }
    }

    private fun toolkitUrl(): String {
        val configured = (Config.allValues["EXPO_PUBLIC_TOOLKIT_URL"] ?: "").trim()
        val base = configured.ifEmpty { "https://toolkit.rork.com" }
        return base.trimEnd('/') + "/v2/vercel/v1/chat/completions"
    }

    private fun toolkitKey(): String =
        (Config.allValues["EXPO_PUBLIC_RORK_TOOLKIT_SECRET_KEY"] ?: "").trim()

    private fun systemPrompt(jester: Boolean, languageName: String): String {
        val base = listOf(
            "You are ModernBody, a precise nutrition estimator. Given a meal photo or description,",
            "identify each distinct food component and estimate its nutrition for the portion shown.",
            "",
            "Rules:",
            "- Estimate realistic portion sizes from visual cues (plate size, utensils, hands).",
            "- Break composite dishes into their main components when clearly separable, otherwise return the dish as one item.",
            "- Quantities must be human readable, e.g. \"1 medium bowl\", \"150 g\", \"2 slices\".",
            "- healthScore is 1-10 where 10 is an exceptionally nutritious, whole-food meal.",
            "- title is a short, appetising name for the whole meal (max 4 words).",
            "- If the input clearly contains no edible food, set isFood to false and return an empty items array.",
            "",
            "Respond with ONLY raw JSON matching exactly this shape, no markdown fences:",
            "{\"title\":string,\"isFood\":boolean,\"healthScore\":number,\"items\":[{\"name\":string,\"quantity\":string,\"calories\":number,\"protein\":number,\"carbs\":number,\"fat\":number}],\"quip\":string}",
            "",
            "Write title, every item name, quantity and quip in $languageName. Keep the JSON keys in English.",
        ).joinToString("\n")

        return if (jester) {
            "$base\n\nSet quip to one savage, funny one-line roast of this meal (max 14 words)."
        } else {
            "$base\n\nSet quip to one short, warm, encouraging note about this meal (max 12 words)."
        }
    }

    /** Pulls the first balanced JSON object out of a possibly fenced model reply. */
    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        if (start == -1) return null
        var depth = 0
        for (index in start until text.length) {
            when (text[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return text.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private suspend fun send(
        userContent: List<Pair<String, String>>,
        jesterMode: Boolean,
        languageName: String,
    ): AnalysisResult {
        // toolkitUrl() falls back to the public gateway, so without this a build
        // with no key looked configured, sent an empty bearer token, collected a
        // 401 and told the user analysis was temporarily unavailable and to try
        // again later - for a missing build-time credential no retry can supply.
        // iOS carried the identical bug and was fixed in unit 17.
        if (toolkitKey().isEmpty()) throw NutritionAiException(AiErrorKind.NotConfigured)

        val body = buildJsonObject {
            put("model", MODEL)
            put("temperature", 0.2)
            putJsonArray("messages") {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt(jesterMode, languageName))
                    },
                )
                add(
                    buildJsonObject {
                        put("role", "user")
                        putJsonArray("content") {
                            userContent.forEach { (type, value) ->
                                add(
                                    buildJsonObject {
                                        put("type", type)
                                        if (type == "text") {
                                            put("text", value)
                                        } else {
                                            putJsonObject("image_url") { put("url", value) }
                                        }
                                    },
                                )
                            }
                        }
                    },
                )
            }
            putJsonObject("providerOptions") {
                putJsonObject("gateway") {
                    putJsonArray("models") { FALLBACK_MODELS.forEach { add(it) } }
                }
            }
        }

        val response = try {
            client.post(toolkitUrl()) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${toolkitKey()}")
                setBody(body.toString())
            }
        } catch (error: CancellationException) {
            // Structured concurrency: a cancelled coroutine must be allowed to
            // unwind. Converting this to a ServerError told the user something
            // had gone wrong when in fact they had navigated away, and left the
            // parent scope believing the child completed normally.
            throw error
        } catch (error: Exception) {
            throw NutritionAiException(AiErrorKind.ServerError)
        }

        when (response.status.value) {
            in 200..299 -> Unit
            401, 403 -> throw NutritionAiException(AiErrorKind.AuthError)
            402 -> throw NutritionAiException(AiErrorKind.InsufficientBalance)
            413 -> throw NutritionAiException(AiErrorKind.ImageTooLarge)
            429 -> throw NutritionAiException(AiErrorKind.RateLimited)
            else -> throw NutritionAiException(AiErrorKind.ServerError)
        }

        val text = try {
            val payload = json.parseToJsonElement(response.body<String>())
            payload
                .let { it as? kotlinx.serialization.json.JsonObject }
                ?.get("choices")
                ?.let { it as? kotlinx.serialization.json.JsonArray }
                ?.firstOrNull()
                ?.let { it as? kotlinx.serialization.json.JsonObject }
                ?.get("message")
                ?.let { it as? kotlinx.serialization.json.JsonObject }
                ?.get("content")
                ?.let { it as? kotlinx.serialization.json.JsonPrimitive }
                ?.content
        } catch (error: Exception) {
            throw NutritionAiException(AiErrorKind.BadResponse)
        } ?: throw NutritionAiException(AiErrorKind.BadResponse)

        val extracted = extractJson(text) ?: throw NutritionAiException(AiErrorKind.BadResponse)
        val parsed = try {
            json.decodeFromString<AnalysisResult>(extracted)
        } catch (error: Exception) {
            throw NutritionAiException(AiErrorKind.BadResponse)
        }

        if (!parsed.isFood || parsed.items.isEmpty()) {
            throw NutritionAiException(AiErrorKind.NotFood)
        }
        return parsed
    }

    suspend fun analyzeImage(
        dataUrl: String,
        jesterMode: Boolean,
        languageName: String = "English",
    ): AnalysisResult = send(
        listOf(
            "text" to "Analyse this meal photo and return the JSON.",
            "image_url" to dataUrl,
        ),
        jesterMode,
        languageName,
    )

    suspend fun analyzeText(
        description: String,
        jesterMode: Boolean,
        languageName: String = "English",
    ): AnalysisResult = send(
        listOf("text" to "Meal description: \"$description\". Return the JSON."),
        jesterMode,
        languageName,
    )

    fun resultToItems(result: AnalysisResult): List<FoodItem> = result.items.map { item ->
        FoodItem(
            name = item.name,
            quantity = item.quantity,
            calories = maxOf(0, item.calories.roundToInt()),
            protein = maxOf(0.0, item.protein),
            carbs = maxOf(0.0, item.carbs),
            fat = maxOf(0.0, item.fat),
        )
    }

    fun messageFor(error: Throwable): String =
        (error as? NutritionAiException)?.kind?.message ?: AiErrorKind.ServerError.message
}
