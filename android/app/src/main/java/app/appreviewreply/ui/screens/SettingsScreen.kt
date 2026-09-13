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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.appreviewreply.BuildConfig
import app.appreviewreply.data.model.TrackedApp
import app.appreviewreply.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onAddApp: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    var byo by remember(data.byoApiKey) { mutableStateOf(data.byoApiKey ?: "") }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your apps", style = MaterialTheme.typography.titleMedium)
            data.apps.forEach { app -> AppCard(app, onSave = vm::updateApp, onRemove = { vm.removeApp(app.packageName) }) }
            OutlinedButton(onClick = onAddApp, modifier = Modifier.fillMaxWidth()) { Text("Add another app") }

            Text("AI drafts", style = MaterialTheme.typography.titleMedium)
            Text("Included: 500 drafts per month. Add your own Anthropic API key for unlimited drafts billed to you.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = byo, onValueChange = { byo = it }, label = { Text("Anthropic API key (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row { Button(onClick = { vm.setByoKey(byo) }) { Text("Save key") }; Spacer(Modifier.padding(4.dp)); if (byo.isNotBlank()) TextButton(onClick = { byo = ""; vm.setByoKey("") }) { Text("Remove") } }

            val subscribed by vm.billing.subscribed.collectAsStateWithLifecycle()
            val offers by vm.billing.offers.collectAsStateWithLifecycle()
            Text("Subscription", style = MaterialTheme.typography.titleMedium)
            if (!app.appreviewreply.data.billing.BillingManager.SUBSCRIPTION_REQUIRED) {
                Text("Free during the beta. Thank you for testing — please report anything odd.", style = MaterialTheme.typography.bodySmall)
            } else if (subscribed) {
                Text("Pro is active. Manage it in Google Play → Subscriptions.", style = MaterialTheme.typography.bodySmall)
            } else {
                val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    offers.forEach { o -> Button(onClick = { activity?.let { vm.billing.launch(it, o.offerToken) } }) { Text("${o.basePlanId} · ${o.price}") } }
                }
            }

            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("AppReviewReply ${BuildConfig.VERSION_NAME} · appreviewreply.app\nReviews are fetched with your Google account's Play Console permissions and stay on this device.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AppCard(app: TrackedApp, onSave: (TrackedApp) -> Unit, onRemove: () -> Unit) {
    var name by remember(app.packageName) { mutableStateOf(app.name) }
    var desc by remember(app.packageName) { mutableStateOf(app.description) }
    var email by remember(app.packageName) { mutableStateOf(app.supportEmail ?: "") }
    var dev by remember(app.packageName) { mutableStateOf(app.developerName ?: "") }
    var tone by remember(app.packageName) { mutableStateOf(app.tone) }
    var examples by remember(app.packageName) { mutableStateOf(app.exampleReplies.joinToString("\n\n")) }
    val dirty = name != app.name || desc != app.description || email != (app.supportEmail ?: "") || dev != (app.developerName ?: "") || tone != app.tone || examples != app.exampleReplies.joinToString("\n\n")

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(app.packageName, style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("App name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("What the app does (helps the drafts)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Support email (offered on bug reports)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = dev, onValueChange = { dev = it }, label = { Text("Sign replies as (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Tone", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("friendly", "formal", "concise").forEach { t -> FilterChip(selected = tone == t, onClick = { tone = t }, label = { Text(t) }) }
            }
            OutlinedTextField(value = examples, onValueChange = { examples = it }, label = { Text("Up to 5 example replies in your voice (blank line between)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Row {
                Button(enabled = dirty, onClick = {
                    onSave(app.copy(name = name.ifBlank { app.packageName }, description = desc, supportEmail = email.ifBlank { null }, developerName = dev.ifBlank { null }, tone = tone,
                        exampleReplies = examples.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }.take(5)))
                }) { Text("Save") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRemove) { Text("Remove app") }
            }
            Spacer(Modifier.height(0.dp))
        }
    }
}
