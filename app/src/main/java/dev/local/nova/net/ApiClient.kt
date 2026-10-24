package dev.local.nova.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

sealed interface ModelsResult {
    data class Success(val models: List<String>) : ModelsResult
    data class Failure(val message: String) : ModelsResult
}

/**
 * Klien HTTP minimal untuk API kompatibel OpenAI.
 *
 * Memakai `HttpURLConnection` bawaan Android, tanpa pustaka tambahan. Alasannya: satu
 * permintaan GET belum sepadan dengan biaya menambah OkHttp beserta dependensinya, dan
 * build di perangkat ini mahal.
 *
 * Semua pemanggilan harus dari IO dispatcher; kelas ini tidak mengatur thread sendiri.
 */
class ApiClient {

    suspend fun listModels(baseUrl: String, apiKey: String): ModelsResult = withContext(Dispatchers.IO) {
        val url = Endpoint.modelsUrl(baseUrl)
        var connection: HttpURLConnection? = null

        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                if (apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
                }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                return@withContext ModelsResult.Failure(describeHttpError(code, body))
            }

            val models = parseModels(body)
            if (models.isEmpty()) {
                ModelsResult.Failure("Terhubung, tetapi daftar model kosong")
            } else {
                ModelsResult.Success(models)
            }
        } catch (e: UnknownHostException) {
            ModelsResult.Failure("Host tidak ditemukan. Periksa alamat endpoint.")
        } catch (e: SocketTimeoutException) {
            ModelsResult.Failure("Koneksi habis waktu. Server tidak menjawab.")
        } catch (e: SSLException) {
            ModelsResult.Failure("TLS gagal: ${e.message ?: "sertifikat tidak dipercaya"}")
        } catch (e: IOException) {
            ModelsResult.Failure("Gagal terhubung: ${e.message ?: "kesalahan jaringan"}")
        } catch (e: Exception) {
            ModelsResult.Failure("Kesalahan tak terduga: ${e.message ?: e::class.java.simpleName}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseModels(body: String): List<String> {
        val array = JSONObject(body).optJSONArray("data") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val id = array.optJSONObject(i)?.optString("id").orEmpty()
                if (id.isNotBlank()) add(id)
            }
        }
    }

    private fun describeHttpError(code: Int, body: String): String {
        val hint = when (code) {
            401, 403 -> "API key ditolak atau tidak berwenang"
            404 -> "Endpoint tidak ditemukan. Cek base URL; mungkin tidak perlu akhiran versi."
            429 -> "Terlalu banyak permintaan atau kuota habis"
            in 500..599 -> "Server bermasalah"
            else -> "Permintaan ditolak"
        }
        val trimmed = body.trim().take(200)
        return if (trimmed.isEmpty()) "$hint (HTTP $code)" else "$hint (HTTP $code): $trimmed"
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 20_000
    }
}
