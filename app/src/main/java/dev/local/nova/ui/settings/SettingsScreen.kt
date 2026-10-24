package dev.local.nova.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.local.nova.data.AppSettings
import dev.local.nova.net.ApiClient
import dev.local.nova.net.Endpoint
import dev.local.nova.net.EndpointResult
import dev.local.nova.net.ModelsResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val apiClient = remember { ApiClient() }
    val scope = rememberCoroutineScope()

    var baseUrl by remember { mutableStateOf(settings.baseUrl.orEmpty()) }
    var apiKey by remember { mutableStateOf(settings.apiKey.orEmpty()) }
    var keyVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    fun report(message: String, isError: Boolean) {
        status = message
        statusIsError = isError
    }

    fun validated(): String? {
        when (val result = Endpoint.parse(baseUrl)) {
            is EndpointResult.Invalid -> {
                report(result.reason, isError = true)
                return null
            }

            is EndpointResult.Valid -> {
                baseUrl = result.baseUrl
                return result.baseUrl
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Koneksi",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Hanya endpoint https yang didukung. API key dikirim pada setiap " +
                    "permintaan, jadi sambungan tanpa enkripsi tidak diizinkan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = if (keyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { keyVisible = !keyVisible }) {
                        Text(if (keyVisible) "Sembunyikan" else "Lihat")
                    }
                },
            )

            Button(
                onClick = {
                    val url = validated() ?: return@Button
                    settings.baseUrl = url
                    settings.apiKey = apiKey
                    report("Tersimpan. ${Endpoint.maskKey(apiKey)}", isError = false)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Simpan")
            }

            OutlinedButton(
                onClick = {
                    val url = validated() ?: return@OutlinedButton
                    busy = true
                    status = null
                    scope.launch {
                        when (val result = apiClient.listModels(url, apiKey)) {
                            is ModelsResult.Success -> {
                                val sample = result.models.take(5).joinToString(", ")
                                report(
                                    "${result.models.size} model tersedia: $sample",
                                    isError = false,
                                )
                            }

                            is ModelsResult.Failure -> report(result.message, isError = true)
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            ) {
                Text(if (busy) "Menguji…" else "Uji koneksi")
            }

            if (busy) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 4.dp))
            }

            status?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}
