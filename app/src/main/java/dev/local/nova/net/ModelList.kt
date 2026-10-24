package dev.local.nova.net

/**
 * Pengurutan dan penyaringan daftar model dari provider.
 *
 * Model sering dikembalikan tidak berurutan dan bisa berisi duplikat, jadi daftar dirapikan
 * sebelum ditampilkan. Pencarian tidak membedakan huruf besar kecil karena nama model
 * panjang dan sulit diingat persis.
 */
object ModelList {

    fun normalize(models: List<String>): List<String> =
        models.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedBy { it.lowercase() }
            .toList()

    fun filter(models: List<String>, query: String): List<String> {
        val needle = query.trim()
        if (needle.isEmpty()) return models
        return models.filter { it.contains(needle, ignoreCase = true) }
    }
}
