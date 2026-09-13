package app.appreviewreply.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.appreviewreply.ui.AppViewModel

/** Reviews the AI classified as bug / crash / feature request, grouped by category and app version. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuesScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val issues = data.reviews.filter { it.isIssue }
    val groups = issues.groupBy { it.category ?: "other" }.toSortedMap()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Issues from reviews") })
        if (issues.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
                Text("Nothing yet. When a draft is generated, the review is classified; bugs, crashes and feature requests collect here.", style = MaterialTheme.typography.bodyLarge)
            }
            return
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            groups.forEach { (category, list) ->
                item(key = "h-$category") {
                    Text("${category.replace('_', ' ').replaceFirstChar { it.uppercase() }} · ${list.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                }
                items(list, key = { it.id }) { r ->
                    val appName = data.apps.firstOrNull { it.packageName == r.packageName }?.name ?: r.packageName
                    Card(Modifier.fillMaxWidth().clickable { onOpen(r.id) }) {
                        Column(Modifier.padding(12.dp)) {
                            Row { Stars(r.stars); Spacer(Modifier.weight(1f)); Text(listOfNotNull(appName, r.appVersion?.let { "v$it" }).joinToString(" · "), style = MaterialTheme.typography.labelSmall) }
                            Spacer(Modifier.height(4.dp))
                            Text(r.summary ?: r.text, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                            if (!r.answered) { Spacer(Modifier.height(4.dp)); Text("Not answered yet", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}
