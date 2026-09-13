package app.appreviewreply.data.auth

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

/**
 * Obtains a Google OAuth access token with the Play Developer scope, using the
 * developer's own Google account. First call needs user consent (resolution
 * intent); later calls are silent, so the background worker can refresh tokens.
 *
 * Requires an OAuth "Android" client (package + SHA-1) in the Google Cloud project
 * that has the Google Play Android Developer API enabled.
 */
object GoogleAuth {
    const val SCOPE_PUBLISHER = "https://www.googleapis.com/auth/androidpublisher"

    sealed class Result {
        data class Token(val accessToken: String) : Result()
        data class NeedsConsent(val intentSender: IntentSender) : Result()
        data class Failed(val message: String) : Result()
    }

    private fun request(): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE_PUBLISHER)))
            .build()

    /** Silent when already granted; otherwise returns a consent intent to launch. */
    suspend fun authorize(context: Context): Result = try {
        val result = Identity.getAuthorizationClient(context).authorize(request()).await()
        when {
            result.hasResolution() -> Result.NeedsConsent(result.pendingIntent!!.intentSender)
            result.accessToken != null -> Result.Token(result.accessToken!!)
            else -> Result.Failed("No access token returned")
        }
    } catch (e: Exception) {
        Result.Failed(e.message ?: e.toString())
    }

    /** Call with the activity result data after the consent screen. */
    fun tokenFromConsent(context: Context, data: Intent?): Result = try {
        val res = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
        res.accessToken?.let { Result.Token(it) } ?: Result.Failed("Consent returned no token")
    } catch (e: Exception) {
        Result.Failed(e.message ?: e.toString())
    }
}
