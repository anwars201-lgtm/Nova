package dev.local.nova.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.local.nova.diag.Check
import dev.local.nova.diag.SelfTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Menjalankan [SelfTest] sekali saat layar dibuka, lalu menampilkan hasilnya.
 * Hanya dipakai pada build debug.
 */
@Composable
fun DiagnosticsCard() {
    val context = LocalContext.current
    val checks by produceState<List<Check>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { SelfTest.run(context) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val list = checks
            val allPassed = list != null && list.all { it.passed }

            Text(
                text = when {
                    list == null -> "Diagnostik berjalan…"
                    allPassed -> "Diagnostik: ${list.size} pemeriksaan lulus"
                    else -> "Diagnostik: ${list.count { !it.passed }} gagal dari ${list.size}"
                },
                style = MaterialTheme.typography.titleSmall,
            )

            list?.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (item.passed) "✓" else "✗",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.passed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Column {
                        Text(text = item.name, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = item.detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
