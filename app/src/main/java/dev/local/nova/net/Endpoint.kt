package dev.local.nova.net

import java.net.URI
import java.net.URISyntaxException

/** Hasil validasi endpoint. Hanya bentuk yang sudah dinormalisasi yang boleh disimpan. */
sealed interface EndpointResult {
    data class Valid(val baseUrl: String) : EndpointResult
    data class Invalid(val reason: String) : EndpointResult
}

/**
 * Aturan endpoint kompatibel OpenAI.
 *
 * Sengaja hanya menerima https (keputusan pengguna): API key dikirim pada setiap permintaan,
 * jadi transport tanpa enkripsi tidak diizinkan. Server lokal seperti LM Studio harus
 * diakses lewat proksi https atau tunnel.
 *
 * Base URL boleh diberikan dengan atau tanpa akhiran versi. Pengguna yang menempel
 * `https://api.openai.com/v1` dan `https://api.openai.com` harus sama-sama berhasil,
 * jadi penentuan path diserahkan ke [modelsUrl].
 */
object Endpoint {

    private const val SCHEME_HTTPS = "https"
    private const val API_PREFIX = "/v" + "1"

    fun parse(raw: String): EndpointResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return EndpointResult.Invalid("Endpoint belum diisi")
        if (trimmed.any { it.isWhitespace() }) {
            return EndpointResult.Invalid("Endpoint tidak boleh mengandung spasi")
        }

        val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"

        val uri = try {
            URI(candidate)
        } catch (e: URISyntaxException) {
            return EndpointResult.Invalid("Format URL tidak valid")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != SCHEME_HTTPS) {
            return EndpointResult.Invalid("Hanya https yang didukung")
        }
        if (uri.host.isNullOrBlank()) {
            return EndpointResult.Invalid("Nama host tidak ditemukan")
        }
        if (uri.userInfo != null) {
            return EndpointResult.Invalid("Jangan menyertakan user:pass di URL")
        }
        if (!uri.query.isNullOrEmpty() || !uri.fragment.isNullOrEmpty()) {
            return EndpointResult.Invalid("URL tidak boleh memuat query atau fragmen")
        }

        val path = uri.path.orEmpty().trimEnd('/')
        val normalized = buildString {
            append(SCHEME_HTTPS)
            append("://")
            append(uri.host)
            if (uri.port != -1) {
                append(':').append(uri.port)
            }
            append(path)
        }

        return EndpointResult.Valid(normalized)
    }

    /**
     * Menyusun URL daftar model dari base yang sudah divalidasi.
     * Kalau base sudah berakhiran versi (`/v1`), tidak ditambahkan lagi.
     */
    fun modelsUrl(baseUrl: String): String = "${apiRoot(baseUrl)}/models"

    /** Menyusun URL chat completions dari base yang sudah divalidasi. */
    fun chatUrl(baseUrl: String): String = "${apiRoot(baseUrl)}/chat/completions"

    private fun apiRoot(baseUrl: String): String {
        val base = baseUrl.trimEnd('/')
        return if (base.endsWith(API_PREFIX)) base else base + API_PREFIX
    }

    /** Menyamarkan API key untuk ditampilkan. Tidak pernah menampilkan utuh. */
    fun maskKey(key: String): String {
        if (key.isBlank()) return ""
        if (key.length <= 8) return "•".repeat(key.length)
        return key.take(3) + "…" + key.takeLast(4)
    }
}
