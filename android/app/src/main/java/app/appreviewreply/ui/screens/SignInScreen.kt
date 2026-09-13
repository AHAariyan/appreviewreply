package app.appreviewreply.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.appreviewreply.ui.AppViewModel

@Composable
fun SignInScreen(vm: AppViewModel, onDone: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val data by vm.data.collectAsStateWithLifecycle()
    var pkg by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    LaunchedEffect(data.apps.size) { if (data.apps.isNotEmpty() && ui.token != null) onDone() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("AppReviewReply", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Reply to every Play Store review in two minutes a day, in your own voice. Sign in with the Google account you use for Play Console.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        if (ui.token == null) {
            Button(onClick = { vm.signIn() }, modifier = Modifier.fillMaxWidth()) { Text("Sign in with Google") }
        } else {
            Text("Signed in. Add the package name of an app you publish:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = pkg, onValueChange = { pkg = it.trim() }, label = { Text("Package name, e.g. com.example.myapp") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("App name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            if (ui.syncing) CircularProgressIndicator() else Button(onClick = { vm.addApp(pkg, name) }, enabled = pkg.contains('.'), modifier = Modifier.fillMaxWidth()) { Text("Add app") }
            if (data.apps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Go to inbox") }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Reviews are read and answered with your own Google permissions. Nothing is stored outside your phone except a monthly draft counter.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}
