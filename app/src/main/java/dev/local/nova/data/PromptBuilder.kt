package dev.local.nova.data

import dev.local.nova.data.model.Memory

/**
 * Penyusun system prompt yang dikirim pada setiap permintaan.
 *
 * Urutan baku (keputusan siklus 6):
 * 1. system prompt manual pengguna,
 * 2. blok memori berisi semua memori yang aktif.
 *
 * Batas dibuat keras supaya biaya token tidak meledak tanpa terasa: paling banyak
 * [MAX_ENTRIES] entri dan [MAX_CHARS] karakter untuk seluruh system prompt. Entri yang
 * tidak kebagian tempat dilewati, bukan dipotong di tengah kalimat, dan jumlahnya
 * dilaporkan lewat [Prompt.dropped] agar UI bisa memperingatkan pengguna.
 *
 * Tidak ada embedding atau pencarian relevansi: semua memori aktif dikirim apa adanya.
 */
object PromptBuilder {

    const val MAX_ENTRIES = 50
    const val MAX_CHARS = 4_000

    private const val MEMORY_HEADER = "Yang perlu diingat tentang pengguna:"

    data class Prompt(
        val system: String?,
        val included: Int,
        val dropped: Int,
        val characters: Int,
    ) {
        val hitLimit: Boolean get() = dropped > 0
    }

    fun build(manualPrompt: String?, memories: List<Memory>): Prompt {
        val manual = manualPrompt?.trim().orEmpty()
        val candidates = memories.filter { it.active && it.content.isNotBlank() }

        val lines = mutableListOf<String>()
        var used = manual.length
        var dropped = 0

        for (memory in candidates) {
            val line = "- " + memory.content.trim()
            if (lines.size >= MAX_ENTRIES) {
                dropped++
                continue
            }
            val cost = line.length + 1 // +1 untuk pemisah baris
            if (used + cost > MAX_CHARS) {
                dropped++
                continue
            }
            lines += line
            used += cost
        }

        val sections = buildList {
            if (manual.isNotEmpty()) add(manual)
            if (lines.isNotEmpty()) add(MEMORY_HEADER + "\n" + lines.joinToString("\n"))
        }

        val system = sections.joinToString("\n\n").ifBlank { null }

        return Prompt(
            system = system,
            included = lines.size,
            dropped = dropped,
            characters = system?.length ?: 0,
        )
    }
}
