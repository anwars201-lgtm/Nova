package dev.local.nova.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.local.nova.BuildConfig
import dev.local.nova.data.NovaDatabase
import dev.local.nova.data.ThreadRepository
import dev.local.nova.data.model.Thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/**
 * Daftar percakapan.
 *
 * Thread dibuat saat tombol "Chat baru" ditekan, bukan saat pesan pertama dikirim:
 * pengguna langsung masuk ke layar percakapan kosong, dan judul diambil dari pesan
 * pertama begitu ada isinya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenThread: (String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { ThreadRepository(NovaDatabase(context)) }

    var reloadKey by remember { mutableIntStateOf(0) }
    val threads by produceState<List<Thread>>(initialValue = emptyList(), reloadKey) {
        value = withContext(Dispatchers.IO) { repository.threads() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova") },
                actions = {
                    TextButton(onClick = onOpenSettings) { Text("Pengaturan") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val thread = repository.createThread()
                    reloadKey++
                    onOpenThread(thread.id)
                },
            ) {
                Text("Chat baru")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (threads.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Belum ada percakapan",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Atur endpoint dan API key di Pengaturan, lalu mulai chat baru.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    if (BuildConfig.DEBUG) {
                        Box(modifier = Modifier.padding(top = 24.dp)) { DiagnosticsCard() }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(threads, key = { it.id }) { thread ->
                        ThreadRow(thread = thread, onClick = { onOpenThread(thread.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadRow(thread: Thread, onClick: () -> Unit) {
    val formatter = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = thread.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = buildString {
                    thread.model?.let { append(it).append(" · ") }
                    append(formatter.format(Date(thread.updatedAt)))
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
