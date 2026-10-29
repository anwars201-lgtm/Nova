package dev.local.nova.net

/**
 * Parameter opsional yang ikut dikirim ke endpoint chat completions.
 *
 * Keduanya boleh kosong. Kalau kosong, field-nya **tidak dikirim sama sekali** — bukan
 * dikirim sebagai null — karena sebagian provider menolak permintaan yang memuat field
 * tak dikenal.
 */
data class ChatOptions(
    val reasoningEffort: String? = null,
    val maxTokens: Int? = null,
) {
    val isEmpty: Boolean get() = reasoningEffort == null && maxTokens == null
}

/**
 * Aturan pengisian parameter. Dipisahkan dari UI dan dari klien HTTP supaya bisa diuji
 * di JVM tanpa perangkat.
 */
object ChatParams {

    const val MIN_TOKENS = 1
    const val MAX_TOKENS = 200_000

    /** Nilai yang ditawarkan sebagai pilihan cepat di layar Pengaturan. */
    val EFFORT_PRESETS = listOf("low", "medium", "high")

    sealed interface TokensResult {
        /** Kosong berarti batas tidak dipakai. */
        data object Off : TokensResult
        data class Value(val tokens: Int) : TokensResult
        data class Invalid(val reason: String) : TokensResult
    }

    fun parseMaxTokens(raw: String): TokensResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return TokensResult.Off

        val number = trimmed.toIntOrNull()
            ?: return TokensResult.Invalid("Isi dengan angka, atau kosongkan")
        if (number < MIN_TOKENS) return TokensResult.Invalid("Minimum $MIN_TOKENS token")
        if (number > MAX_TOKENS) return TokensResult.Invalid("Maksimum $MAX_TOKENS token")

        return TokensResult.Value(number)
    }

    /** Nilai yang benar-benar dikirim. Kosong berarti field dihilangkan. */
    fun extraFields(options: ChatOptions): Map<String, Any> = buildMap {
        options.reasoningEffort?.trim()?.takeIf { it.isNotEmpty() }?.let {
            put("reasoning_effort", it)
        }
        options.maxTokens?.let { put("max_tokens", it) }
    }
}
