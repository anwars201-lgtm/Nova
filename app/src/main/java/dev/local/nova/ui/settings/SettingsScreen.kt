package dev.local.nova.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.local.nova.data.AppSettings
import dev.local.nova.BuildConfig
import dev.local.nova.ui.home.DiagnosticsCard
import dev.local.nova.net.ApiClient
import dev.local.nova.net.ChatParams
import dev.local.nova.net.Endpoint
import dev.local.nova.net.EndpointResult
import dev.local.nova.net.ModelsResult
import dev.local.nova.net.ModelList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenMemory: () -> Unit = {}) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val apiClient = remember { ApiClient() }
    val scope = rememberCoroutineScope()

    var baseUrl by remember { mutableStateOf(settings.baseUrl.orEmpty()) }
    var apiKey by remember { mutableStateOf(settings.apiKey.orEmpty()) }
    var model by remember { mutableStateOf(settings.defaultModel.orEmpty()) }
    var systemPrompt by remember { mutableStateOf(settings.systemPrompt.orEmpty()) }
    var effort by remember { mutableStateOf(settings.reasoningEffort.orEmpty()) }
    var customEffort by remember {
        mutableStateOf(effort.isNotEmpty() && effort !in ChatParams.EFFORT_PRESETS)
    }
    var showEffortDialog by remember { mutableStateOf(false) }
    var maxTokensText by remember {
        mutableStateOf(settings.maxTokens.takeIf { it > 0 }?.toString().orEmpty())
    }
    var maxTokensError by remember { mutableStateOf<String?>(null) }
    var keyVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingModels by remember { mutableStateOf(false) }
    var modelQuery by remember { mutableStateOf("") }
    var modelError by remember { mutableStateOf<String?>(null) }

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

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Model") },
                placeholder = { Text("gpt-4o-mini") },
                singleLine = true,
                supportingText = {
                    Text("Diketik manual, atau muat daftar dari provider lalu pilih.")
                },
            )

            OutlinedButton(
                onClick = {
                    val url = validated() ?: return@OutlinedButton
                    loadingModels = true
                    modelError = null
                    scope.launch {
                        when (val result = apiClient.listModels(url, apiKey)) {
                            is ModelsResult.Success -> {
                                availableModels = ModelList.normalize(result.models)
                                showModelDialog = true
                            }

                            is ModelsResult.Failure -> {
                                modelError = result.message
                                showModelDialog = true
                            }
                        }
                        loadingModels = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loadingModels,
            ) {
                Text(if (loadingModels) "Memuat model…" else "Muat daftar model dari provider")
            }

            if (showEffortDialog) {
                AlertDialog(
                    onDismissRequest = { showEffortDialog = false },
                    title = { Text("Reasoning effort") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val options = listOf("Tidak dipakai") + ChatParams.EFFORT_PRESETS + "Nilai lain…"
                            options.forEach { option ->
                                TextButton(
                                    onClick = {
                                        when (option) {
                                            "Tidak dipakai" -> {
                                                effort = ""
                                                customEffort = false
                                            }

                                            "Nilai lain…" -> {
                                                customEffort = true
                                            }

                                            else -> {
                                                effort = option
                                                customEffort = false
                                            }
                                        }
                                        showEffortDialog = false
                                    },
                                ) { Text(option) }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showEffortDialog = false }) { Text("Tutup") }
                    },
                )
            }

            if (showModelDialog) {
                AlertDialog(
                    onDismissRequest = { showModelDialog = false },
                    title = { Text("Pilih model") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = modelQuery,
                                onValueChange = { modelQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Cari") },
                                singleLine = true,
                            )

                            modelError?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }

                            if (loadingModels) {
                                CircularProgressIndicator()
                            }

                            Column(
                                modifier = Modifier
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                val visible = ModelList.filter(availableModels, modelQuery)
                                if (visible.isEmpty() && modelError == null) {
                                    Text(
                                        text = if (availableModels.isEmpty()) {
                                            "Provider tidak mengembalikan daftar model. Ketik nama model manual."
                                        } else {
                                            "Tidak ada yang cocok."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                visible.forEach { name ->
                                    TextButton(
                                        onClick = {
                                            model = name
                                            showModelDialog = false
                                        },
                                    ) { Text(name) }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showModelDialog = false }) { Text("Tutup") }
                    },
                )
            }

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("System prompt") },
                placeholder = { Text("Kamu adalah asisten yang menjawab ringkas.") },
                minLines = 3,
                maxLines = 6,
                supportingText = {
                    Text("Dikirim sebelum blok memori pada setiap permintaan.")
                },
            )

            OutlinedButton(
                onClick = onOpenMemory,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Kelola memori")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reasoning effort", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = effort.ifBlank { "Tidak dipakai" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showEffortDialog = true }) { Text("Ubah") }
            }

            if (customEffort) {
                OutlinedTextField(
                    value = effort,
                    onValueChange = { effort = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nilai effort khusus") },
                    placeholder = { Text("minimal") },
                    singleLine = true,
                    supportingText = {
                        Text("Dikirim apa adanya sebagai reasoning_effort.")
                    },
                )
            }

            OutlinedTextField(
                value = maxTokensText,
                onValueChange = {
                    maxTokensText = it
                    maxTokensError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Max tokens (opsional)") },
                placeholder = { Text("kosong = tidak dibatasi") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = maxTokensError != null,
                supportingText = {
                    Text(
                        maxTokensError ?: "Batas panjang balasan. Kosongkan bila tidak ingin mengirim parameter ini.",
                    )
                },
            )

            Button(
                onClick = {
                    val url = validated() ?: return@Button
                    val tokens = when (val parsed = ChatParams.parseMaxTokens(maxTokensText)) {
                        is ChatParams.TokensResult.Invalid -> {
                            maxTokensError = parsed.reason
                            return@Button
                        }

                        is ChatParams.TokensResult.Value -> parsed.tokens
                        ChatParams.TokensResult.Off -> 0
                    }
                    settings.baseUrl = url
                    settings.apiKey = apiKey
                    settings.defaultModel = model.trim().ifBlank { null }
                    settings.systemPrompt = systemPrompt
                    settings.reasoningEffort = effort.trim().ifBlank { null }
                    settings.maxTokens = tokens
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

            if (BuildConfig.DEBUG) {
                DiagnosticsCard()
            }
        }
    }
}
