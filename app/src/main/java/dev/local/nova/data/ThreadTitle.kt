package dev.local.nova.data

/**
 * Penurunan judul thread dari pesan pertama.
 *
 * Dipisahkan dari database supaya bisa diuji di JVM tanpa Android. Aturannya sederhana
 * dan disengaja: ambil baris non-kosong pertama, rapikan spasi, potong di batas kata
 * agar tidak memotong kata di tengah.
 */
object ThreadTitle {
    const val MAX_LENGTH = 48
    const val FALLBACK = "Chat baru"

    fun derive(text: String): String {
        val firstLine = text.lineSequence()
            .map { it.trim().replace(WHITESPACE, " ") }
            .firstOrNull { it.isNotEmpty() }
            ?: return FALLBACK

        if (firstLine.length <= MAX_LENGTH) return firstLine

        val cut = firstLine.take(MAX_LENGTH).substringBeforeLast(' ', missingDelimiterValue = "")
        val base = cut.ifBlank { firstLine.take(MAX_LENGTH) }
        return base.trimEnd(' ', ',', '.', ';', ':') + "…"
    }

    private val WHITESPACE = Regex("\\s+")
}
