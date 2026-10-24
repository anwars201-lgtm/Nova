package dev.local.nova.data.model

/**
 * Status pesan. `STREAMING` dipakai saat token masih masuk, dan harus diubah menjadi
 * `COMPLETE` atau `FAILED` ketika stream berhenti — kalau tidak, pesan akan tertinggal
 * di keadaan setengah jadi setelah app mati.
 */
enum class MessageStatus(val wire: String) {
    STREAMING("streaming"),
    COMPLETE("complete"),
    FAILED("failed"),
    ;

    companion object {
        fun fromWire(raw: String?): MessageStatus =
            entries.firstOrNull { it.wire.equals(raw, ignoreCase = true) } ?: COMPLETE
    }
}
