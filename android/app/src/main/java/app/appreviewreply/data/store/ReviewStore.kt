package app.appreviewreply.data.store

import android.content.Context
import app.appreviewreply.data.model.AppState
import app.appreviewreply.data.model.Review
import app.appreviewreply.data.model.TrackedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Single JSON file on disk + in-memory StateFlow. Enough for a few thousand
 * reviews; swap for Room if it ever gets slow.
 */
class ReviewStore(context: Context) {
    private val file = File(context.filesDir, "state.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
    private val mutex = Mutex()
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state

    private fun load(): AppState = try {
        if (file.exists()) json.decodeFromString(AppState.serializer(), file.readText()) else AppState()
    } catch (_: Exception) {
        AppState()
    }.let { s -> if (s.userId == null) s.copy(userId = UUID.randomUUID().toString().replace("-", "")) else s }

    suspend fun update(transform: (AppState) -> AppState) = mutex.withLock {
        val next = transform(_state.value)
        _state.value = next
        withContext(Dispatchers.IO) {
            val tmp = File(file.parentFile, "state.json.tmp")
            tmp.writeText(json.encodeToString(AppState.serializer(), next))
            tmp.renameTo(file)
        }
    }

    suspend fun addApp(app: TrackedApp) = update { s ->
        if (s.apps.any { it.packageName == app.packageName }) s else s.copy(apps = s.apps + app)
    }

    suspend fun updateApp(app: TrackedApp) = update { s ->
        s.copy(apps = s.apps.map { if (it.packageName == app.packageName) app else it })
    }

    suspend fun removeApp(packageName: String) = update { s ->
        s.copy(apps = s.apps.filter { it.packageName != packageName }, reviews = s.reviews.filter { it.packageName != packageName })
    }

    /** Merge freshly fetched reviews, keeping local fields (draft, category, skipped). Returns count of new unanswered reviews. */
    suspend fun mergeReviews(fetched: List<Review>): Int {
        var newUnanswered = 0
        update { s ->
            val (merged, n) = mergeReviewLists(s.reviews, fetched)
            newUnanswered = n
            s.copy(reviews = merged, lastSync = System.currentTimeMillis())
        }
        return newUnanswered
    }

    suspend fun updateReview(id: String, transform: (Review) -> Review) = update { s ->
        s.copy(reviews = s.reviews.map { if (it.id == id) transform(it) else it })
    }

    suspend fun setByoKey(key: String?) = update { it.copy(byoApiKey = key?.takeIf { k -> k.isNotBlank() }) }

    companion object {
        /** Pure merge: fetched reviews overwrite server fields, local fields survive. Returns (merged sorted list, new unanswered count). */
        fun mergeReviewLists(existing: List<Review>, fetched: List<Review>): Pair<List<Review>, Int> {
            var newUnanswered = 0
            val byId = existing.associateBy { it.id }.toMutableMap()
            for (r in fetched) {
                val old = byId[r.id]
                if (old == null) {
                    if (!r.answered) newUnanswered++
                    byId[r.id] = r
                } else {
                    byId[r.id] = r.copy(
                        draft = old.draft, category = old.category, summary = old.summary,
                        needsFollowup = old.needsFollowup, skipped = old.skipped, firstSeen = old.firstSeen,
                    )
                }
            }
            return byId.values.sortedByDescending { it.lastModified } to newUnanswered
        }
    }
}
