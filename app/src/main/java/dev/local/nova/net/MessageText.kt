package dev.local.nova.net

/**
 * Normalisasi teks yang akan dikirim sebagai pesan.
 *
 * Dipisahkan dari UI supaya aturannya bisa diuji tanpa perangkat: tidak mengirim
 * spasi kosong, membuang spasi di ujung, dan menolak pesan yang melebihi batas.
 */
object MessageText {
    const val MAX_LENGTH = 32_000

    sealed interface Result {
        data class Ready(val text: String) : Result
        data class Rejected(val reason: String) : Result
    }

    fun prepare(raw: String): Result {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> Result.Rejected("Pesan masih kosong")
            trimmed.length > MAX_LENGTH -> Result.Rejected("Pesan terlalu panjang (maks $MAX_LENGTH karakter)")
            else -> Result.Ready(trimmed)
        }
    }
}
