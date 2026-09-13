package app.appreviewreply.data.demo

import app.appreviewreply.data.model.Review
import app.appreviewreply.data.model.TrackedApp

/** Sample data so the app can be explored (and screenshotted) without a Play Console account. */
object DemoData {
    const val PACKAGE = "demo.sample.app"

    val app = TrackedApp(
        packageName = PACKAGE,
        name = "Sample App (demo)",
        description = "A note-taking app with cloud sync and a dark theme.",
        tone = "friendly",
        supportEmail = "support@example.com",
        exampleReplies = listOf(
            "Thanks so much for the kind words! If you ever want a feature, just tell us.",
            "Sorry about that — we're on it. Could you email support@example.com with your device model so we can fix it fast?",
        ),
    )

    private val day = 24L * 60 * 60 * 1000
    private val now = System.currentTimeMillis()

    val reviews: List<Review> = listOf(
        Review("d1", PACKAGE, "Maya K.", 2, "Crashes every time I open the settings page since the last update. Pixel 8.", now - 1 * day, "en", "Pixel 8", "14", "3.2.1"),
        Review("d2", PACKAGE, "Jonas", 5, "Best notes app I've used. Sync is instant and the dark theme is perfect.", now - 1 * day, "en", "Galaxy S23", "14", "3.2.1"),
        Review("d3", PACKAGE, "Priya R.", 3, "Good app but please add a widget for quick notes. I'd use it every day.", now - 2 * day, "en", "OnePlus 11", "13", "3.2.0"),
        Review("d4", PACKAGE, "Tom", 1, "Lost all my notes after reinstalling. Where is the backup??", now - 2 * day, "en", "Pixel 6a", "13", "3.1.9"),
        Review("d5", PACKAGE, "Lucía", 4, "Muy buena, pero el precio anual es alto para lo que ofrece.", now - 3 * day, "es", "Xiaomi 13", "13", "3.2.1"),
        Review("d6", PACKAGE, "Anonymous", 5, "👍", now - 3 * day, "en", null, "12", "3.2.1"),
        Review("d7", PACKAGE, "Dev W.", 2, "Ads everywhere now. Uninstalling.", now - 4 * day, "en", "Pixel 7", "14", "3.2.1"),
        Review("d8", PACKAGE, "Hanna", 4, "Love it. Answered before: thank you for adding folders!", now - 5 * day, "en", "Galaxy A54", "13", "3.2.0",
            developerReply = "Thanks Hanna — folders were the most requested feature, glad they help!", developerReplyAt = now - 4 * day),
    )
}
