package app.appreviewreply.data.play

import app.appreviewreply.data.model.Review
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Minimal client for Google Play Developer API v3 — reviews only.
 * Docs: https://developers.google.com/android-publisher/reply-to-reviews
 * Only reviews with comments from the last 7 days are returned by Google.
 */
class PlayDeveloperApi(private val client: OkHttpClient = OkHttpClient()) {
    private val json = Json { ignoreUnknownKeys = true }
    private val base = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"

    class ApiException(val code: Int, message: String) : IOException(message)

    suspend fun listReviews(token: String, packageName: String): List<Review> = withContext(Dispatchers.IO) {
        val out = mutableListOf<Review>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append("$base/$packageName/reviews?maxResults=100")
                if (pageToken != null) append("&token=$pageToken")
            }
            val body = get(token, url)
            val (reviews, next) = parseReviewsPage(packageName, body)
            out += reviews
            pageToken = next
        } while (pageToken != null)
        out
    }

    suspend fun reply(token: String, packageName: String, reviewId: String, text: String): Long = withContext(Dispatchers.IO) {
        val payload = buildJsonObject { put("replyText", text.take(350)) }.toString()
        val req = Request.Builder()
            .url("$base/$packageName/reviews/$reviewId:reply")
            .header("Authorization", "Bearer $token")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { res ->
            val body = res.body.string()
            if (!res.isSuccessful) throw ApiException(res.code, errorMessage(body, res.code))
            val obj = json.parseToJsonElement(body).jsonObject
            obj["result"]?.jsonObject?.get("lastEdited")?.jsonObject?.get("seconds")?.jsonPrimitive?.content?.toLongOrNull()?.times(1000)
                ?: System.currentTimeMillis()
        }
    }

    private fun get(token: String, url: String): String {
        val req = Request.Builder().url(url).header("Authorization", "Bearer $token").get().build()
        client.newCall(req).execute().use { res ->
            val body = res.body.string()
            if (!res.isSuccessful) throw ApiException(res.code, errorMessage(body, res.code))
            return body
        }
    }

    private fun errorMessage(body: String, code: Int): String = try {
        val err = json.parseToJsonElement(body).jsonObject["error"]?.jsonObject
        val msg = err?.get("message")?.jsonPrimitive?.content
        val status = err?.get("status")?.jsonPrimitive?.content
        android.util.Log.w("AppReviewReply", "Play API $code: $body")
        listOfNotNull(status, msg).joinToString(" — ").ifBlank { "HTTP $code" }
    } catch (_: Exception) {
        android.util.Log.w("AppReviewReply", "Play API $code (unparsed): ${body.take(300)}")
        "HTTP $code"
    }

    /** Parses one page of the reviews.list response. Public for unit tests. */
    fun parseReviewsPage(packageName: String, body: String): Pair<List<Review>, String?> {
        val root = json.parseToJsonElement(body).jsonObject
        val reviews = root["reviews"]?.jsonArray?.mapNotNull { el -> parseReview(packageName, el.jsonObject) } ?: emptyList()
        val next = root["tokenPagination"]?.jsonObject?.get("nextPageToken")?.jsonPrimitive?.content
        return reviews to next
    }

    private fun parseReview(packageName: String, o: JsonObject): Review? {
        val id = o["reviewId"]?.jsonPrimitive?.content ?: return null
        val author = o["authorName"]?.jsonPrimitive?.content ?: "Anonymous"
        var user: JsonObject? = null
        var dev: JsonObject? = null
        o["comments"]?.jsonArray?.forEach { c ->
            val co = c.jsonObject
            co["userComment"]?.jsonObject?.let { user = it }
            co["developerComment"]?.jsonObject?.let { dev = it }
        }
        val u = user ?: return null
        fun JsonObject.str(k: String) = this[k]?.jsonPrimitive?.content
        fun JsonObject.secs(k: String) = this[k]?.jsonObject?.get("seconds")?.jsonPrimitive?.content?.toLongOrNull()?.times(1000)
        return Review(
            id = id,
            packageName = packageName,
            author = author,
            stars = u.str("starRating")?.toIntOrNull() ?: 0,
            text = u.str("text")?.trim().orEmpty(),
            lastModified = u.secs("lastModified") ?: System.currentTimeMillis(),
            language = u.str("reviewerLanguage"),
            device = u["deviceMetadata"]?.jsonObject?.str("productName") ?: u.str("device"),
            androidVersion = u.str("androidOsVersion"),
            appVersion = u.str("appVersionName"),
            developerReply = dev?.str("text"),
            developerReplyAt = dev?.secs("lastModified"),
        )
    }
}
