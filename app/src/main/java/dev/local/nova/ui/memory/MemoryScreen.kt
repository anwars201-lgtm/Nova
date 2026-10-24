package dev.local.nova.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.local.nova.data.AppSettings
import dev.local.nova.data.MemoryRepository
import dev.local.nova.data.MemoryText
import dev.local.nova.data.NovaDatabase
import dev.local.nova.data.PromptBuilder
import dev.local.nova.data.model.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pengelolaan memori.
 *
 * Semua perubahan langsung ditulis ke SQLite; tidak ada tombol "simpan semua". Dengan
 * begitu yang terlihat di layar selalu sama dengan yang dikirim ke model pada permintaan
 * berikutnya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { MemoryRepository(NovaDatabase(context)) }
    val settings = remember { AppSettings(context) }

    var reload by remember { mutableIntStateOf(0) }
    var memoryEnabled by remember { mutableStateOf(settings.memoryEnabled) }
    var editing by remember { mutableStateOf<Memory?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var draftError by remember { mutableStateOf<String?>(null) }

    val memories by produceState<List<Memory>>(initialValue = emptyList(), reload) {
        value = withContext(Dispatchers.IO) { repository.all() }
    }

    val active = memories.filter { it.active }
    val preview = PromptBuilder.build(
        manualPrompt = settings.systemPrompt,
        memories = if (memoryEnabled) active else emptyList(),
    )
    val overEntryLimit = active.size > PromptBuilder.MAX_ENTRIES

    fun openEditor(memory: Memory?) {
        editing = memory
        draft = memory?.content.orEmpty()
        draftError = null
        showEditor = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memori") },
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
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sisipkan memori", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (memoryEnabled) {
                                "Memori aktif dikirim pada setiap permintaan."
                            } else {
                                "Dimatikan: tidak ada memori yang dikirim."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = memoryEnabled,
                        onCheckedChange = {
                            memoryEnabled = it
                            settings.memoryEnabled = it
                        },
                    )
                }

                Text(
                    text = "${active.size} entri aktif · ${preview.characters}/${PromptBuilder.MAX_CHARS} karakter",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )

                if (memoryEnabled && (preview.hitLimit || overEntryLimit)) {
                    Text(
                        text = buildString {
                            append("Batas tercapai")
                            if (preview.dropped > 0) append(": ${preview.dropped} entri tidak dikirim")
                            if (overEntryLimit) append(" (maksimum ${PromptBuilder.MAX_ENTRIES} entri)")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                TextButton(onClick = { openEditor(null) }) { Text("Tambah memori") }
            }

            if (memories.isEmpty()) {
                Text(
                    text = "Belum ada memori. Tambahkan hal yang ingin diingat model, misalnya nama panggilan atau preferensi jawaban.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(memories, key = { it.id }) { memory ->
                        MemoryRow(
                            memory = memory,
                            onToggle = { value ->
                                repository.setActive(memory.id, value)
                                reload++
                            },
                            onEdit = { openEditor(memory) },
                            onDelete = {
                                repository.delete(memory.id)
                                reload++
                            },
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) "Memori baru" else "Ubah memori") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = {
                            draft = it
                            draftError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Isi memori") },
                        minLines = 3,
                        maxLines = 6,
                    )
                    draftError?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        text = "Maksimum ${MemoryText.MAX_LENGTH} karakter.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when (val prepared = MemoryText.prepare(draft)) {
                        is MemoryText.Result.Rejected -> draftError = prepared.reason
                        is MemoryText.Result.Ready -> {
                            val target = editing
                            if (target == null) {
                                repository.add(prepared.text)
                            } else {
                                repository.updateContent(target.id, prepared.text)
                            }
                            showEditor = false
                            reload++
                        }
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun MemoryRow(
    memory: Memory,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (memory.active) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = memory.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = memory.active, onCheckedChange = onToggle)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Ubah") }
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}
