package dev.local.nova.net

import org.json.JSONObject

/**
 * Pembaca potongan Server-Sent Events dari endpoint chat completions.
 *
 * Hanya menangani bentuk yang dipakai API kompatibel OpenAI:
 *
 *     data: {"choices":[{"delta":{"content":"Halo"}}]}
 *     data: [DONE]
 *
 * Baris lain (komentar, `event:`, baris kosong) diabaikan. Semua fungsi murni supaya
 * bisa diuji di JVM; pengujian memakai implementasi org.json untuk desktop.
 */
object SseParser {

    private const val PREFIX = "data:"
    private const val DONE = "[DONE]"

    fun isDone(line: String): Boolean = payloadOf(line) == DONE

    /** Mengembalikan potongan teks, atau null bila baris tidak membawa isi. */
    fun delta(line: String): String? {
        val payload = payloadOf(line) ?: return null
        if (payload == DONE) return null

        return try {
            val choices = JSONObject(payload).optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val content = choices.getJSONObject(0)
                .optJSONObject("delta")
                ?.optString("content")
                .orEmpty()
            content.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    /** Mengembalikan pesan galat bila server mengirim objek error di tengah stream. */
    fun errorMessage(line: String): String? {
        val payload = payloadOf(line) ?: return null
        if (payload == DONE) return null
        return try {
            JSONObject(payload).optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun payloadOf(line: String): String? {
        val trimmed = line.trim()
        if (!trimmed.startsWith(PREFIX)) return null
        return trimmed.removePrefix(PREFIX).trim()
    }
}
