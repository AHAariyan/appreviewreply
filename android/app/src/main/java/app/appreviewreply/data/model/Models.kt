package app.appreviewreply.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackedApp(
    val packageName: String,
    val name: String = packageName,
    val description: String = "",
    val tone: String = "friendly", // friendly | formal | concise
    val supportEmail: String? = null,
    val developerName: String? = null,
    val exampleReplies: List<String> = emptyList(),
)

@Serializable
data class Review(
    val id: String,
    val packageName: String,
    val author: String,
    val stars: Int,
    val text: String,
    val lastModified: Long, // epoch millis
    val language: String? = null,
    val device: String? = null,
    val androidVersion: String? = null,
    val appVersion: String? = null,
    val developerReply: String? = null,
    val developerReplyAt: Long? = null,
    // Local state
    val draft: String? = null,
    val category: String? = null,
    val summary: String? = null,
    val needsFollowup: Boolean = false,
    val skipped: Boolean = false,
    val firstSeen: Long = System.currentTimeMillis(),
) {
    val answered: Boolean get() = !developerReply.isNullOrBlank()
    val isIssue: Boolean get() = category == "bug" || category == "crash" || category == "feature_request"
}

@Serializable
data class AppState(
    val apps: List<TrackedApp> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val lastSync: Long = 0,
    val userId: String? = null, // opaque id for the proxy's monthly cap
    val byoApiKey: String? = null,
    val accountName: String? = null, // Google account used for Play Console access
)

@Serializable
data class DraftResult(
    val reply: String,
    val category: String,
    val summary: String,
    val needs_followup: Boolean = false,
    val language: String = "en",
)
