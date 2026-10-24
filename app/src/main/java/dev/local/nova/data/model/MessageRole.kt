package dev.local.nova.data.model

/** Peran pengirim pesan. Nilai `wire` adalah yang disimpan di database dan dikirim ke API. */
enum class MessageRole(val wire: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    TOOL("tool"),
    ;

    companion object {
        /** Mengembalikan null untuk nilai tak dikenal, supaya pemanggil bisa memilih perilaku. */
        fun fromWire(raw: String?): MessageRole? =
            entries.firstOrNull { it.wire.equals(raw, ignoreCase = true) }
    }
}
