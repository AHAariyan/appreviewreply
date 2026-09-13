package app.appreviewreply.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.appreviewreply.data.model.Review
import app.appreviewreply.ui.AppViewModel
import java.text.DateFormat
import java.util.Date

enum class InboxFilter(val label: String) { UNANSWERED("Unanswered"), LOW("1–2★"), ISSUES("Issues"), ALL("All") }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InboxScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(InboxFilter.UNANSWERED) }
    var pkg by remember { mutableStateOf<String?>(null) }

    val reviews = data.reviews
        .filter { pkg == null || it.packageName == pkg }
        .filter {
            when (filter) {
                InboxFilter.UNANSWERED -> !it.answered && !it.skipped
                InboxFilter.LOW -> it.stars <= 2
                InboxFilter.ISSUES -> it.isIssue
                InboxFilter.ALL -> true
            }
        }
    val unanswered = data.reviews.count { !it.answered && !it.skipped }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (unanswered == 0) "Inbox — all caught up" else "Inbox — $unanswered to answer") },
            actions = { IconButton(onClick = { vm.sync() }) { Icon(Icons.Filled.Refresh, "Sync") } },
        )
        if (ui.syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
        FlowRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InboxFilter.entries.forEach { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label, maxLines = 1) })
            }
        }
        if (data.apps.size > 1) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = pkg == null, onClick = { pkg = null }, label = { Text("All apps") })
                data.apps.forEach { a -> FilterChip(selected = pkg == a.packageName, onClick = { pkg = a.packageName }, label = { Text(a.name, maxLines = 1) }) }
            }
        }
        if (reviews.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
                Text(
                    when {
                        data.reviews.isEmpty() -> "No reviews yet. Google only returns reviews with text from the last 7 days — tap refresh, or wait for tomorrow's sync."
                        else -> "Nothing here for this filter."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reviews, key = { it.id }) { r -> ReviewCard(r, appName = data.apps.firstOrNull { it.packageName == r.packageName }?.name, onClick = { onOpen(r.id) }) }
            }
        }
    }
}

@Composable
fun Stars(stars: Int) {
    Text("★".repeat(stars.coerceIn(0, 5)) + "☆".repeat(5 - stars.coerceIn(0, 5)), color = MaterialTheme.colorScheme.secondary)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewCard(r: Review, appName: String?, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Stars(r.stars)
                Spacer(Modifier.width(8.dp))
                Text(r.author, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(DateFormat.getDateInstance(DateFormat.SHORT).format(Date(r.lastModified)), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(6.dp))
            Text(r.text.ifBlank { "(no text)" }, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (appName != null) AssistChip(onClick = onClick, label = { Text(appName, maxLines = 1) })
                r.appVersion?.let { AssistChip(onClick = onClick, label = { Text("v$it", maxLines = 1) }) }
                r.category?.let { AssistChip(onClick = onClick, label = { Text(it.replace('_', ' '), maxLines = 1) }) }
                if (r.answered) AssistChip(onClick = onClick, label = { Text("Answered", maxLines = 1) })
                else if (r.draft != null) AssistChip(onClick = onClick, label = { Text("Draft", maxLines = 1) })
            }
        }
    }
}
