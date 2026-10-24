package dev.local.nova.data.model

/**
 * Satu butir memori yang dikirim ke model pada setiap permintaan.
 *
 * `active = false` menyimpan entri tetapi tidak mengirimkannya. Ini berguna untuk
 * menonaktifkan sementara tanpa kehilangan isi.
 */
data class Memory(
    val id: Long,
    val content: String,
    val createdAt: Long,
    val active: Boolean,
)
