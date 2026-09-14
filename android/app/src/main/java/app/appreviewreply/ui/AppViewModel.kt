package app.appreviewreply.ui

import android.app.Application
import android.content.Intent
import android.content.IntentSender
import android.accounts.AccountManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.appreviewreply.App
import app.appreviewreply.data.auth.GoogleAuth
import app.appreviewreply.data.billing.BillingManager
import app.appreviewreply.data.demo.DemoData
import app.appreviewreply.data.draft.DraftClient
import app.appreviewreply.data.model.AppState
import app.appreviewreply.data.model.Review
import app.appreviewreply.data.model.TrackedApp
import app.appreviewreply.data.play.PlayDeveloperApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val token: String? = null,
    val consentIntent: IntentSender? = null,
    val accountPickerIntent: Intent? = null,
    val syncing: Boolean = false,
    val drafting: Set<String> = emptySet(),
    val posting: Set<String> = emptySet(),
    val message: String? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = (application as App).store
    private val api = PlayDeveloperApi()
    private val drafts = DraftClient()
    val billing = BillingManager(application).also { it.connect() }

    val data: StateFlow<AppState> = store.state
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    val signedIn: Boolean get() = _ui.value.token != null

    /** Opens the system Google-account picker; the chosen account is remembered and used for all API calls. */
    fun chooseAccount() {
        val intent = AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
        _ui.update { it.copy(accountPickerIntent = intent) }
    }

    fun onAccountPicked(data: Intent?) = viewModelScope.launch {
        _ui.update { it.copy(accountPickerIntent = null) }
        val name = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) ?: return@launch
        store.setAccountName(name)
        _ui.update { it.copy(token = null) }
        signIn()
    }

    fun accountPickerDismissed() = _ui.update { it.copy(accountPickerIntent = null) }

    fun signIn() = viewModelScope.launch {
        when (val r = GoogleAuth.authorize(getApplication(), data.value.accountName)) {
            is GoogleAuth.Result.Token -> { _ui.update { it.copy(token = r.accessToken, consentIntent = null) }; sync() }
            is GoogleAuth.Result.NeedsConsent -> _ui.update { it.copy(consentIntent = r.intentSender) }
            is GoogleAuth.Result.Failed -> toast("Sign-in failed: ${r.message}")
        }
    }

    fun onConsentResult(data: Intent?) {
        _ui.update { it.copy(consentIntent = null) }
        when (val r = GoogleAuth.tokenFromConsent(getApplication(), data)) {
            is GoogleAuth.Result.Token -> { _ui.update { it.copy(token = r.accessToken) }; sync() }
            is GoogleAuth.Result.Failed -> toast("Sign-in failed: ${r.message}")
            else -> Unit
        }
    }

    fun consentDismissed() = _ui.update { it.copy(consentIntent = null) }

    /** Re-authorize silently if a call returns 401. */
    private suspend fun freshToken(): String? {
        return when (val r = GoogleAuth.authorize(getApplication(), data.value.accountName)) {
            is GoogleAuth.Result.Token -> { _ui.update { it.copy(token = r.accessToken) }; r.accessToken }
            else -> null
        }
    }

    /** Explore the app with sample reviews; no Google account needed. */
    fun loadDemo() = viewModelScope.launch {
        store.addApp(DemoData.app)
        store.mergeReviews(DemoData.reviews)
        toast("Sample app added. Drafts work; posting is simulated for the sample app.")
    }

    fun addApp(packageName: String, name: String) = viewModelScope.launch {
        val pkg = packageName.trim()
        if (pkg.isEmpty()) return@launch
        val token = _ui.value.token ?: freshToken() ?: run { toast("Sign in first"); return@launch }
        _ui.update { it.copy(syncing = true) }
        try {
            val reviews = api.listReviews(token, pkg) // validates access to this package
            store.addApp(TrackedApp(packageName = pkg, name = name.ifBlank { pkg }))
            val n = store.mergeReviews(reviews)
            toast("Added $pkg — ${reviews.size} reviews in the last 7 days, $n unanswered")
        } catch (e: PlayDeveloperApi.ApiException) {
            toast("Google said (${e.code}): ${e.message}")
        } catch (e: Exception) {
            toast("Error: ${e.message}")
        } finally {
            _ui.update { it.copy(syncing = false) }
        }
    }

    fun updateApp(app: TrackedApp) = viewModelScope.launch { store.updateApp(app) }
    fun removeApp(pkg: String) = viewModelScope.launch { store.removeApp(pkg) }

    fun sync() = viewModelScope.launch {
        val apps = data.value.apps.filter { it.packageName != DemoData.PACKAGE }
        if (apps.isEmpty()) return@launch
        val token = _ui.value.token ?: freshToken() ?: return@launch
        _ui.update { it.copy(syncing = true) }
        var total = 0
        var failed = 0
        for (a in apps) {
            if (a.packageName == DemoData.PACKAGE) continue
            try { total += store.mergeReviews(api.listReviews(token, a.packageName)) } catch (_: Exception) { failed++ }
        }
        _ui.update { it.copy(syncing = false) }
        if (failed > 0) toast("$failed app(s) failed to sync") else if (total > 0) toast("$total new reviews")
    }

    fun draft(review: Review) = viewModelScope.launch {
        val app = data.value.apps.firstOrNull { it.packageName == review.packageName } ?: return@launch
        val userId = data.value.userId ?: return@launch
        _ui.update { it.copy(drafting = it.drafting + review.id) }
        try {
            val d = drafts.draft(userId, app, review, data.value.byoApiKey)
            store.updateReview(review.id) { it.copy(draft = d.reply, category = d.category, summary = d.summary, needsFollowup = d.needs_followup) }
        } catch (e: DraftClient.DraftException) {
            toast(when (e.message) {
                "monthly_cap_reached" -> "Monthly draft limit reached. Add your own API key in Settings for unlimited drafts."
                "unauthorised" -> "App not authorised with the draft service."
                else -> "Draft failed: ${e.message}"
            })
        } catch (e: Exception) {
            toast("Draft failed: ${e.message}")
        } finally {
            _ui.update { it.copy(drafting = it.drafting - review.id) }
        }
    }

    fun setDraft(review: Review, text: String) = viewModelScope.launch { store.updateReview(review.id) { it.copy(draft = text) } }

    fun post(review: Review, text: String) = viewModelScope.launch {
        val body = text.trim().take(350)
        if (body.isEmpty()) return@launch
        if (review.packageName == DemoData.PACKAGE) {
            store.updateReview(review.id) { it.copy(developerReply = body, developerReplyAt = System.currentTimeMillis(), draft = null) }
            toast("Reply saved (sample app — nothing was sent to Google)")
            return@launch
        }
        var token = _ui.value.token ?: freshToken() ?: run { toast("Sign in first"); return@launch }
        _ui.update { it.copy(posting = it.posting + review.id) }
        try {
            val at = try {
                api.reply(token, review.packageName, review.id, body)
            } catch (e: PlayDeveloperApi.ApiException) {
                if (e.code == 401) { token = freshToken() ?: throw e; api.reply(token, review.packageName, review.id, body) } else throw e
            }
            store.updateReview(review.id) { it.copy(developerReply = body, developerReplyAt = at, draft = null) }
            toast("Reply posted")
        } catch (e: PlayDeveloperApi.ApiException) {
            toast(if (e.code == 403) "Not allowed: your Play Console account needs the 'Reply to reviews' permission." else "Post failed: ${e.message}")
        } catch (e: Exception) {
            toast("Post failed: ${e.message}")
        } finally {
            _ui.update { it.copy(posting = it.posting - review.id) }
        }
    }

    fun skip(review: Review, skipped: Boolean = true) = viewModelScope.launch { store.updateReview(review.id) { it.copy(skipped = skipped) } }

    fun setByoKey(key: String) = viewModelScope.launch { store.setByoKey(key) }

    fun toast(msg: String) = _ui.update { it.copy(message = msg) }
    fun consumeMessage() = _ui.update { it.copy(message = null) }
}
