package app.appreviewreply

import app.appreviewreply.data.model.Review
import app.appreviewreply.data.store.ReviewStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewMergeTest {
    private fun review(id: String, modified: Long, reply: String? = null) =
        Review(id = id, packageName = "p", author = "a", stars = 3, text = "t", lastModified = modified, developerReply = reply)

    @Test
    fun newUnansweredCounted_answeredNot() {
        val (merged, n) = ReviewStore.mergeReviewLists(emptyList(), listOf(review("1", 10), review("2", 20, reply = "thanks")))
        assertEquals(2, merged.size)
        assertEquals(1, n)
        assertEquals("2", merged.first().id) // sorted newest first
    }

    @Test
    fun localFieldsSurviveRefetch() {
        val existing = listOf(review("1", 10).copy(draft = "my draft", category = "bug", summary = "s", needsFollowup = true, skipped = true, firstSeen = 5))
        val refetched = listOf(review("1", 10).copy(text = "edited by user", developerReply = "posted"))
        val (merged, n) = ReviewStore.mergeReviewLists(existing, refetched)
        assertEquals(0, n)
        val r = merged.single()
        assertEquals("edited by user", r.text)          // server field updated
        assertEquals("posted", r.developerReply)          // server field updated
        assertEquals("my draft", r.draft)                 // local kept
        assertEquals("bug", r.category)
        assertTrue(r.needsFollowup)
        assertTrue(r.skipped)
        assertEquals(5L, r.firstSeen)
    }

    @Test
    fun reviewsOlderThanWindowAreKept() {
        // Google only returns the last 7 days; older reviews must not vanish from local history.
        val existing = listOf(review("old", 1))
        val (merged, _) = ReviewStore.mergeReviewLists(existing, listOf(review("new", 100)))
        assertEquals(listOf("new", "old"), merged.map { it.id })
    }
}
