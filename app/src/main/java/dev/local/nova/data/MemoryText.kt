package dev.local.nova.data

/**
 * Validasi isi satu butir memori sebelum disimpan.
 *
 * Batas per entri dijaga di sini supaya satu memori tidak bisa menghabiskan seluruh
 * anggaran karakter yang tersedia untuk semua memori sekaligus.
 */
object MemoryText {
    const val MAX_LENGTH = 500

    sealed interface Result {
        data class Ready(val text: String) : Result
        data class Rejected(val reason: String) : Result
    }

    fun prepare(raw: String): Result {
        val trimmed = raw.trim().replace(Regex("\\s+"), " ")
        return when {
            trimmed.isEmpty() -> Result.Rejected("Memori masih kosong")
            trimmed.length > MAX_LENGTH -> Result.Rejected("Maksimum $MAX_LENGTH karakter")
            else -> Result.Ready(trimmed)
        }
    }
}
