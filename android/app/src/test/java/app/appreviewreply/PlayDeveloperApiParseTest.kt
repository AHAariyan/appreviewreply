package app.appreviewreply

import app.appreviewreply.data.play.PlayDeveloperApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayDeveloperApiParseTest {
    private val sample = """
    {
      "reviews": [
        {
          "reviewId": "gp:AAAA",
          "authorName": "Maya K.",
          "comments": [
            { "userComment": { "text": "\tCrashes on settings", "lastModified": { "seconds": "1757700000", "nanos": 0 },
              "starRating": 2, "reviewerLanguage": "en", "device": "panther", "androidOsVersion": 34,
              "appVersionCode": 321, "appVersionName": "3.2.1",
              "deviceMetadata": { "productName": "Pixel 8 (panther)", "manufacturer": "Google" } } },
            { "developerComment": { "text": "Sorry! Fix coming.", "lastModified": { "seconds": "1757710000", "nanos": 0 } } }
          ]
        },
        {
          "reviewId": "gp:BBBB",
          "authorName": "Jonas",
          "comments": [
            { "userComment": { "text": "Great app", "lastModified": { "seconds": "1757600000", "nanos": 0 }, "starRating": 5, "reviewerLanguage": "de" } }
          ]
        }
      ],
      "tokenPagination": { "nextPageToken": "NEXT123" }
    }
    """.trimIndent()

    @Test
    fun parsesReviewsAndPagination() {
        val (reviews, next) = PlayDeveloperApi().parseReviewsPage("com.example", sample)
        assertEquals(2, reviews.size)
        assertEquals("NEXT123", next)

        val r = reviews.first { it.id == "gp:AAAA" }
        assertEquals("com.example", r.packageName)
        assertEquals("Maya K.", r.author)
        assertEquals(2, r.stars)
        assertEquals("Crashes on settings", r.text)
        assertEquals(1757700000L * 1000, r.lastModified)
        assertEquals("en", r.language)
        assertEquals("Pixel 8 (panther)", r.device)
        assertEquals("34", r.androidVersion)
        assertEquals("3.2.1", r.appVersion)
        assertEquals("Sorry! Fix coming.", r.developerReply)
        assertEquals(1757710000L * 1000, r.developerReplyAt)
        assertTrue(r.answered)

        val j = reviews.first { it.id == "gp:BBBB" }
        assertNull(j.developerReply)
        assertEquals("panther".let { null }, j.device) // no device fields at all
        assertEquals(5, j.stars)
    }

    @Test
    fun emptyPage() {
        val (reviews, next) = PlayDeveloperApi().parseReviewsPage("com.example", "{}")
        assertTrue(reviews.isEmpty())
        assertNull(next)
    }
}
