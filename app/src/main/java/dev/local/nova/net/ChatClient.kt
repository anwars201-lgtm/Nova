package dev.local.nova.net

import dev.local.nova.data.model.Message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
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
        options: ChatOptions = ChatOptions(),
    ): ChatResult = withContext(Dispatchers.IO) {
        val url = Endpoint.chatUrl(baseUrl)
        var connection: HttpURLConnection? = null

        try {
            val payload = buildPayload(model, history, stream = false, options = options)
                .toString().toByteArray(Charsets.UTF_8)

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
                return@withContext ChatResult.Failure(HttpErrorText.describe(code, body))
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

    private fun buildPayload(
        model: String,
        history: List<Message>,
        stream: Boolean = false,
        options: ChatOptions = ChatOptions(),
    ): JSONObject {
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
            .put("stream", stream)
            .put("messages", messages)
            .apply {
                ChatParams.extraFields(options).forEach { (key, value) -> put(key, value) }
            }
    }

    /**
     * Mengirim percakapan dan memanggil [onDelta] untuk setiap potongan teks yang masuk.
     *
     * Kalau server mengabaikan `stream: true` dan membalas JSON biasa, balasan tetap
     * diterima lewat jalur non-streaming. Ini sering terjadi pada proksi yang tidak
     * meneruskan SSE dengan benar.
     *
     * Pembatalan dilakukan lewat `Job.cancel()`: `ensureActive()` akan melempar
     * [CancellationException] dan koneksi ditutup di blok `finally`.
     */
    suspend fun stream(
        baseUrl: String,
        apiKey: String,
        model: String,
        history: List<Message>,
        options: ChatOptions = ChatOptions(),
        onDelta: suspend (String) -> Unit,
    ): ChatResult = withContext(Dispatchers.IO) {
        val url = Endpoint.chatUrl(baseUrl)
        var connection: HttpURLConnection? = null

        try {
            val payload = buildPayload(model, history, stream = true, options = options)
                .toString().toByteArray(Charsets.UTF_8)

            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = STREAM_READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "text/event-stream")
                if (apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
                }
            }

            connection.outputStream.use { it.write(payload) }

            val code = connection.responseCode
            if (code !in 200..299) {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                return@withContext ChatResult.Failure(HttpErrorText.describe(code, body))
            }

            val contentType = connection.contentType.orEmpty()
            if (!contentType.contains("event-stream", ignoreCase = true)) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                return@withContext parseCompletion(body)
            }

            val accumulator = StringBuilder()
            val rawSample = StringBuilder()
            connection.inputStream.bufferedReader().use { reader ->
                while (true) {
                    coroutineContext.ensureActive()
                    val line = reader.readLine() ?: break
                    if (rawSample.length < RAW_SAMPLE_CHARS) {
                        if (rawSample.isNotEmpty()) rawSample.append('\n')
                        rawSample.append(line)
                    }
                    if (SseParser.isDone(line)) break

                    SseParser.errorMessage(line)?.let { message ->
                        return@withContext ChatResult.Failure(message)
                    }

                    val delta = SseParser.delta(line) ?: continue
                    accumulator.append(delta)
                    onDelta(delta)
                }
            }

            if (accumulator.isEmpty()) {
                // Provider kadang menolak dengan HTTP 200 lalu menutup stream tanpa data.
                // Tampilkan cuplikan mentahnya supaya pengguna melihat alasan aslinya.
                val sample = rawSample.toString().trim()
                ChatResult.Failure(
                    if (sample.isEmpty()) {
                        "Server menutup stream tanpa isi"
                    } else {
                        "Server menutup stream tanpa isi. Balasan mentah: ${sample.take(300)}"
                    }
                )
            } else {
                ChatResult.Success(content = accumulator.toString(), model = null)
            }
        } catch (e: CancellationException) {
            throw e
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


    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 120_000
        private const val STREAM_READ_TIMEOUT_MS = 180_000
        private const val RAW_SAMPLE_CHARS = 400
    }
}
