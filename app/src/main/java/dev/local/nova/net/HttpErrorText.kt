package dev.local.nova.net

/**
 * Penerjemah galat HTTP menjadi kalimat yang bisa ditindaklanjuti.
 *
 * Banyak provider kompatibel OpenAI memakai kode 400 atau 422 untuk hal yang berbeda-beda:
 * nama model salah, parameter tidak dikenal, atau batas input. Isi balasan biasanya
 * menyebutkannya, jadi teks itu yang dipakai untuk memilih petunjuk yang tepat.
 *
 * Murni dan tanpa Android supaya bisa diuji di JVM.
 */
object HttpErrorText {

    fun describe(code: Int, body: String): String {
        val snippet = body.trim().take(MAX_BODY_CHARS)
        val lower = snippet.lowercase()

        val mentionsParameter = snippet.contains("parameter", ignoreCase = true) ||
            snippet.contains("unsupported", ignoreCase = true) ||
            snippet.contains("not supported", ignoreCase = true) ||
            snippet.contains("invalid inference", ignoreCase = true) ||
            snippet.contains("unknown field", ignoreCase = true)

        val hint = when {
            (code == 400 || code == 422) && mentionsParameter ->
                "Provider menolak salah satu parameter. Matikan reasoning effort dan max tokens, lalu coba lagi"

            code == 400 -> "Permintaan ditolak. Periksa nama model dan parameter yang aktif"
            code == 401 || code == 403 -> "API key ditolak atau tidak berwenang"
            code == 404 -> "Endpoint tidak ditemukan. Cek base URL; mungkin tidak perlu akhiran versi"
            code == 413 -> "Riwayat percakapan terlalu panjang untuk model ini"
            code == 422 -> "Permintaan tidak dapat diproses provider. Periksa model dan parameter"
            code == 429 -> "Terlalu banyak permintaan atau kuota habis"
            code in 500..599 -> "Server provider bermasalah"
            else -> "Permintaan ditolak"
        }

        return if (snippet.isEmpty()) "$hint (HTTP $code)" else "$hint (HTTP $code): $snippet"
    }

    private const val MAX_BODY_CHARS = 240
}
