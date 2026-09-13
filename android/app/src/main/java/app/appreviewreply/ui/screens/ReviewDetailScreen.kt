package app.appreviewreply.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.appreviewreply.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailScreen(vm: AppViewModel, reviewId: String, onBack: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val review = data.reviews.firstOrNull { it.id == reviewId }
    if (review == null) { LaunchedEffect(Unit) { onBack() }; return }
    val app = data.apps.firstOrNull { it.packageName == review.packageName }

    var text by remember(review.id) { mutableStateOf(review.draft ?: "") }
    LaunchedEffect(review.draft) { if (!review.draft.isNullOrBlank() && text.isBlank()) text = review.draft!! }
    // Auto-draft on first open if there is no reply and no draft yet.
    LaunchedEffect(review.id) { if (!review.answered && review.draft == null && review.id !in ui.drafting) vm.draft(review) }

    val drafting = review.id in ui.drafting
    val posting = review.id in ui.posting

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(app?.name ?: review.packageName, maxLines = 1) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row { Stars(review.stars); Spacer(Modifier.weight(1f)); Text(review.author, style = MaterialTheme.typography.labelLarge) }
                    Spacer(Modifier.height(8.dp))
                    Text(review.text.ifBlank { "(no text)" }, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        listOfNotNull(review.appVersion?.let { "v$it" }, review.device, review.androidVersion?.let { "Android $it" }, review.language).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    review.summary?.let { Spacer(Modifier.height(8.dp)); Text("Summary: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (review.answered) {
                Text("Your reply", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(review.developerReply!!, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Text("You can update a reply by posting a new one.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            Text(if (review.answered) "New reply" else "Reply draft", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            if (drafting && text.isBlank()) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { CircularProgressIndicator(Modifier.height(20.dp)); Spacer(Modifier.padding(6.dp)); Text("Drafting…") }
            }
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(350) },
                modifier = Modifier.fillMaxWidth(), minLines = 4,
                supportingText = { Text("${text.length}/350") },
                placeholder = { Text("Tap Draft to get a suggestion, or write your own.") },
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.draft(review) }, enabled = !drafting) { Text(if (text.isBlank()) "Draft" else "Redraft") }
                OutlinedButton(onClick = { vm.skip(review); onBack() }) { Text("Skip") }
                Spacer(Modifier.weight(1f))
                Button(onClick = { vm.setDraft(review, text); vm.post(review, text) }, enabled = text.isNotBlank() && !posting) {
                    Text(if (posting) "Posting…" else "Post reply")
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Replies are public on Google Play and sent from your developer account. Review the text before posting.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
