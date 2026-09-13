package app.appreviewreply

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import app.appreviewreply.data.store.ReviewStore
import app.appreviewreply.work.SyncWorker

class App : Application() {
    val store: ReviewStore by lazy { ReviewStore(this) }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                SyncWorker.CHANNEL_ID,
                getString(R.string.notification_channel_reviews),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        SyncWorker.schedule(this)
    }
}
