package app.appreviewreply.data.draft

import app.appreviewreply.BuildConfig
import app.appreviewreply.data.model.DraftResult
import app.appreviewreply.data.model.Review
import app.appreviewreply.data.model.TrackedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Talks to the Cloudflare Worker in ../proxy. The Anthropic key never lives in the app. */
class DraftClient(
    private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    class DraftException(val code: Int, message: String) : IOException(message)

    suspend fun draft(userId: String, app: TrackedApp, review: Review, byoKey: String? = null): DraftResult = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            putJsonObject("app") {
                put("name", app.name)
                put("description", app.description)
                put("tone", app.tone)
                app.supportEmail?.let { put("support_email", it) }
                app.developerName?.let { put("developer_name", it) }
                putJsonArray("example_replies") { app.exampleReplies.take(5).forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
            }
            putJsonObject("review") {
                put("stars", review.stars.coerceIn(1, 5))
                put("text", review.text.ifBlank { "(no text)" })
                review.author.let { put("author", it) }
                review.device?.let { put("device", it) }
                review.androidVersion?.let { put("android_version", it) }
                review.appVersion?.let { put("app_version", it) }
                review.language?.let { put("language", it) }
            }
            put("max_chars", 350)
        }.toString()

        val req = Request.Builder()
            .url("${BuildConfig.PROXY_URL.trimEnd('/')}/draft")
            .header("X-App-Secret", BuildConfig.APP_SECRET)
            .header("X-User-Id", userId)
            .apply { if (!byoKey.isNullOrBlank()) header("X-Api-Key", byoKey) }
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { res ->
            val body = res.body.string()
            if (!res.isSuccessful) {
                val err = try { json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content } catch (_: Exception) { null }
                throw DraftException(res.code, err ?: "HTTP ${res.code}")
            }
            json.decodeFromString(DraftResult.serializer(), body)
        }
    }
}
