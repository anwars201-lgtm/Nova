package dev.local.nova.net

import dev.local.nova.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

sealed interface ChatResult {
    data class Success(val content: String, val model: String?) : ChatResult
    data class Failure(val message: String) : ChatResult
}

/**
 * Pemanggil endpoint chat completions kompatibel OpenAI, tanpa streaming.
 *
 * Streaming sengaja belum ada: satu permintaan-tunggu-selesai jauh lebih mudah dibuktikan
 * benar daripada parser SSE. Streaming masuk siklus berikutnya.
 */
class ChatClient {

    suspend fun complete(
        baseUrl: String,
        apiKey: String,
        model: String,
        history: List<Message>,
    ): ChatResult = withContext(Dispatchers.IO) {
        val url = Endpoint.chatUrl(baseUrl)
        var connection: HttpURLConnection? = null

        try {
            val payload = buildPayload(model, history).toString().toByteArray(Charsets.UTF_8)

            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
                }
            }

            connection.outputStream.use { it.write(payload) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                return@withContext ChatResult.Failure(describeHttpError(code, body))
            }

            parseCompletion(body)
        } catch (e: UnknownHostException) {
            ChatResult.Failure("Host tidak ditemukan. Periksa endpoint di Pengaturan.")
        } catch (e: SocketTimeoutException) {
            ChatResult.Failure("Server tidak menjawab dalam batas waktu.")
        } catch (e: SSLException) {
            ChatResult.Failure("TLS gagal: ${e.message ?: "sertifikat tidak dipercaya"}")
        } catch (e: IOException) {
            ChatResult.Failure("Gagal terhubung: ${e.message ?: "kesalahan jaringan"}")
        } catch (e: Exception) {
            ChatResult.Failure("Kesalahan tak terduga: ${e.message ?: e::class.java.simpleName}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildPayload(model: String, history: List<Message>): JSONObject {
        val messages = JSONArray()
        history.forEach { message ->
            messages.put(
                JSONObject()
                    .put("role", message.role.wire)
                    .put("content", message.content)
            )
        }

        return JSONObject()
            .put("model", model)
            .put("stream", false)
            .put("messages", messages)
    }

    private fun parseCompletion(body: String): ChatResult {
        val root = JSONObject(body)
        val choices = root.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            val error = root.optJSONObject("error")?.optString("message")
            return ChatResult.Failure(error?.takeIf { it.isNotBlank() } ?: "Balasan tidak berisi pilihan")
        }

        val first = choices.getJSONObject(0)
        val content = first.optJSONObject("message")?.optString("content").orEmpty()
        if (content.isBlank()) {
            return ChatResult.Failure("Balasan kosong dari server")
        }

        return ChatResult.Success(content = content, model = root.optString("model").takeIf { it.isNotBlank() })
    }

    private fun describeHttpError(code: Int, body: String): String {
        val hint = when (code) {
            400 -> "Permintaan ditolak. Nama model mungkin salah."
            401, 403 -> "API key ditolak atau tidak berwenang"
            404 -> "Endpoint tidak ditemukan. Cek base URL."
            413 -> "Riwayat percakapan terlalu panjang untuk model ini"
            429 -> "Terlalu banyak permintaan atau kuota habis"
            in 500..599 -> "Server bermasalah"
            else -> "Permintaan ditolak"
        }
        val trimmed = body.trim().take(200)
        return if (trimmed.isEmpty()) "$hint (HTTP $code)" else "$hint (HTTP $code): $trimmed"
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 120_000
    }
}
