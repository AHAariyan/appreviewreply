package app.appreviewreply.work

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.appreviewreply.App
import app.appreviewreply.MainActivity
import app.appreviewreply.R
import app.appreviewreply.data.auth.GoogleAuth
import app.appreviewreply.data.play.PlayDeveloperApi
import java.util.concurrent.TimeUnit

/** Fetches new reviews for every tracked app once a day and notifies. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val store = app.store
        val apps = store.state.value.apps.filter { it.packageName != app.appreviewreply.data.demo.DemoData.PACKAGE }
        if (apps.isEmpty()) return Result.success()

        val token = when (val r = GoogleAuth.authorize(applicationContext)) {
            is GoogleAuth.Result.Token -> r.accessToken
            else -> return Result.retry()
        }
        val api = PlayDeveloperApi()
        var newUnanswered = 0
        var failures = 0
        for (a in apps) {
            try {
                newUnanswered += store.mergeReviews(api.listReviews(token, a.packageName))
            } catch (_: Exception) {
                failures++
            }
        }
        if (newUnanswered > 0) notify(newUnanswered)
        return if (failures == apps.size) Result.retry() else Result.success()
    }

    private fun notify(count: Int) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = PendingIntent.getActivity(
            applicationContext, 0, Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (count == 1) "1 new review to answer" else "$count new reviews to answer")
            .setContentText("Open AppReviewReply to draft replies.")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, n)
    }

    companion object {
        const val CHANNEL_ID = "reviews"
        const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "daily-review-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
