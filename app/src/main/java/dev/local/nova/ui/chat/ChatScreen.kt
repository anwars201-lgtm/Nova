package dev.local.nova.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.local.nova.data.AppSettings
import dev.local.nova.data.NovaDatabase
import dev.local.nova.data.ThreadRepository
import dev.local.nova.data.ThreadTitle
import dev.local.nova.data.model.Message
import dev.local.nova.data.model.MessageRole
import dev.local.nova.data.model.Thread
import dev.local.nova.net.ChatClient
import dev.local.nova.net.ChatResult
import dev.local.nova.net.MessageText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Layar percakapan.
 *
 * Alur kirim pesan: simpan pesan pengguna → ambil seluruh riwayat dari database → kirim ke
 * server → simpan balasan. Riwayat selalu dibaca dari database, bukan dari state di memori,
 * supaya yang terlihat sama dengan yang tersimpan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    threadId: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { ThreadRepository(NovaDatabase(context)) }
    val settings = remember { AppSettings(context) }
    val chatClient = remember { ChatClient() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var reload by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val thread by produceState<Thread?>(initialValue = null, threadId, reload) {
        value = withContext(Dispatchers.IO) { repository.thread(threadId) }
    }
    val messages by produceState<List<Message>>(initialValue = emptyList(), threadId, reload) {
        value = withContext(Dispatchers.IO) { repository.messages(threadId) }
    }

    val baseUrl = settings.baseUrl
    val apiKey = settings.apiKey
    val model = settings.defaultModel
    val configured = !baseUrl.isNullOrBlank() && !apiKey.isNullOrBlank() && !model.isNullOrBlank()

    LaunchedEffect(messages.size, busy) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun send() {
        if (busy) return
        val prepared = MessageText.prepare(input)
        if (prepared is MessageText.Result.Rejected) {
            error = prepared.reason
            return
        }
        val text = (prepared as MessageText.Result.Ready).text
        val url = baseUrl ?: return
        val key = apiKey ?: return
        val selectedModel = model ?: return

        input = ""
        error = null
        busy = true

        scope.launch {
            withContext(Dispatchers.IO) {
                repository.addMessage(threadId, MessageRole.USER, text)
                val current = repository.thread(threadId)
                if (current != null && current.title == ThreadTitle.FALLBACK) {
                    repository.rename(threadId, ThreadTitle.derive(text))
                }
            }
            reload++

            val history = withContext(Dispatchers.IO) { repository.messages(threadId) }
            when (val result = chatClient.complete(url, key, selectedModel, history)) {
                is ChatResult.Success -> {
                    withContext(Dispatchers.IO) {
                        repository.addMessage(threadId, MessageRole.ASSISTANT, result.content)
                    }
                }

                is ChatResult.Failure -> error = result.message
            }
            busy = false
            reload++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = thread?.title ?: "Percakapan", maxLines = 1)
                        model?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } },
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
                .imePadding(),
        ) {
            if (!configured) {
                SetupNotice(onOpenSettings)
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }

            if (busy) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                    Text("Menunggu balasan…", style = MaterialTheme.typography.labelSmall)
                }
            }

            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Tulis pesan") },
                    maxLines = 5,
                )
                Button(onClick = { send() }, enabled = !busy && configured) {
                    Text("Kirim")
                }
            }
        }
    }
}

@Composable
private fun SetupNotice(onOpenSettings: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Koneksi belum lengkap",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Isi base URL, API key, dan nama model di Pengaturan.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onOpenSettings) { Text("Buka Pengaturan") }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == MessageRole.USER
    val container = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = container,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}
