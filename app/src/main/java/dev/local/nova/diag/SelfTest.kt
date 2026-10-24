package dev.local.nova.diag

import android.content.Context
import dev.local.nova.data.MemoryRepository
import dev.local.nova.data.NovaDatabase
import dev.local.nova.data.PromptBuilder
import dev.local.nova.data.ThreadRepository
import dev.local.nova.data.ThreadTitle
import dev.local.nova.data.model.MessageRole
import dev.local.nova.data.model.MessageStatus
import dev.local.nova.security.SecureStore

/** Satu butir pemeriksaan yang bisa ditampilkan di layar. */
data class Check(val name: String, val passed: Boolean, val detail: String)

/**
 * Pemeriksaan mandiri untuk lapisan data dan penyimpanan rahasia.
 *
 * Dijalankan di perangkat karena SQLite dan AndroidKeyStore tidak bisa diuji di JVM biasa
 * pada lingkungan ini. Hasilnya ditampilkan di layar beranda saat mode debug, sehingga
 * kebenaran skema, cascade, dan enkripsi dibuktikan oleh eksekusi nyata — bukan asumsi.
 */
object SelfTest {

    fun run(context: Context): List<Check> {
        val checks = mutableListOf<Check>()

        checks += check("Database terbuka & tabel ada") {
            val db = NovaDatabase(context)
            val names = db.readableDatabase.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'",
                null,
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            val required = listOf(
                NovaDatabase.TABLE_THREADS,
                NovaDatabase.TABLE_MESSAGES,
                NovaDatabase.TABLE_APP_META,
            )
            val missing = required.filterNot { it in names }
            if (missing.isEmpty()) "3 tabel ditemukan" else "hilang: $missing"
        }

        val repository = ThreadRepository(NovaDatabase(context))
        var threadId: String? = null

        checks += check("Simpan & baca thread") {
            val thread = repository.createThread(
                title = ThreadTitle.derive("Halo, ini pesan pertama untuk menguji judul otomatis"),
                model = "test-model",
            )
            threadId = thread.id
            val loaded = repository.thread(thread.id)
            when {
                loaded == null -> error("thread tidak ditemukan setelah disimpan")
                loaded.title != thread.title -> error("judul tidak sama: ${loaded.title}")
                loaded.model != "test-model" -> error("model tidak sama: ${loaded.model}")
                else -> "id ${thread.id.take(8)}… dibaca kembali"
            }
        }

        checks += check("Tambah 3 pesan") {
            val id = threadId ?: error("thread belum dibuat")
            repository.addMessage(id, MessageRole.USER, "satu")
            repository.addMessage(id, MessageRole.ASSISTANT, "dua", reasoning = "berpikir")
            repository.addMessage(id, MessageRole.ASSISTANT, "tiga", status = MessageStatus.COMPLETE)
            val count = repository.messageCount(id)
            if (count == 3) "3 pesan tersimpan" else error("terhitung $count, seharusnya 3")
        }

        checks += check("Urutan pesan stabil") {
            val id = threadId ?: error("thread belum dibuat")
            val contents = repository.messages(id).map { it.content }
            if (contents == listOf("satu", "dua", "tiga")) {
                contents.joinToString(" → ")
            } else {
                error("urutan salah: $contents")
            }
        }

        checks += check("Hapus thread menghapus pesan (cascade)") {
            val id = threadId ?: error("thread belum dibuat")
            repository.deleteThread(id)
            val remaining = repository.messageCount(id)
            val gone = repository.thread(id) == null
            if (remaining == 0 && gone) "thread dan 3 pesan terhapus" else {
                error("sisa $remaining pesan, thread hilang=$gone")
            }
        }

        checks += check("SecureStore enkripsi & dekripsi") {
            val store = SecureStore(context)
            val secret = "sk-uji-${System.currentTimeMillis()}"
            store.put(KEY_TEST, secret)
            val read = store.get(KEY_TEST)
            if (read == secret) "bolak-balik cocok (${secret.length} karakter)" else {
                error("nilai tidak cocok: $read")
            }
        }

        checks += check("SecureStore hapus") {
            val store = SecureStore(context)
            store.remove(KEY_TEST)
            if (store.get(KEY_TEST) == null) "nilai hilang setelah remove" else {
                error("nilai masih ada setelah remove")
            }
        }

        checks += check("Tabel memori ada") {
            val db = NovaDatabase(context)
            val names = db.readableDatabase.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(NovaDatabase.TABLE_MEMORIES),
            ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
            if (names.contains(NovaDatabase.TABLE_MEMORIES)) {
                "skema v${NovaDatabase.VERSION} siap"
            } else {
                error("tabel ${NovaDatabase.TABLE_MEMORIES} tidak ditemukan")
            }
        }

        val memories = MemoryRepository(NovaDatabase(context))
        var memoryId: Long? = null

        checks += check("Simpan & baca memori") {
            val id = memories.add("Pengguna suka jawaban ringkas")
            memoryId = id
            val loaded = memories.all().firstOrNull { it.id == id }
            when {
                loaded == null -> error("memori tidak ditemukan")
                loaded.content != "Pengguna suka jawaban ringkas" -> error("isi berbeda: ${loaded.content}")
                !loaded.active -> error("memori baru seharusnya aktif")
                else -> "id $id dibaca kembali"
            }
        }

        checks += check("Nonaktifkan memori") {
            val id = memoryId ?: error("memori belum dibuat")
            memories.setActive(id, false)
            val active = memories.active().any { it.id == id }
            if (!active) "tidak lagi masuk daftar aktif" else error("memori masih aktif")
        }

        checks += check("PromptBuilder urutkan manual lalu memori") {
            val prompt = PromptBuilder.build(
                manualPrompt = "Jawab selalu dalam bahasa Indonesia.",
                memories = listOf(
                    dev.local.nova.data.model.Memory(1, "Suka jawaban ringkas", 0, true),
                    dev.local.nova.data.model.Memory(2, "Tidak aktif", 0, false),
                ),
            )
            val system = prompt.system ?: error("system prompt kosong")
            val manualAt = system.indexOf("Jawab selalu")
            val memoryAt = system.indexOf("Suka jawaban ringkas")
            when {
                manualAt < 0 -> error("prompt manual hilang")
                memoryAt < 0 -> error("memori aktif tidak masuk")
                manualAt > memoryAt -> error("urutan terbalik")
                system.contains("Tidak aktif") -> error("memori nonaktif ikut terkirim")
                else -> "${prompt.included} entri, ${prompt.characters} karakter"
            }
        }

        checks += check("Hapus memori uji") {
            val id = memoryId ?: error("memori belum dibuat")
            memories.delete(id)
            if (memories.count() == 0) "tabel memori bersih" else error("sisa ${memories.count()} memori")
        }

        checks += check("Tidak ada sisa data uji") {
            val leftovers = repository.threads().size
            if (leftovers == 0) "database bersih" else error("$leftovers thread tersisa")
        }

        return checks
    }

    private fun check(name: String, block: () -> String): Check = try {
        Check(name = name, passed = true, detail = block())
    } catch (t: Throwable) {
        Check(name = name, passed = false, detail = t.message ?: t::class.java.simpleName)
    }

    private const val KEY_TEST = "selftest_token"
}
