package dev.local.nova.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Database lokal Nova.
 *
 * Skema sengaja dibuat kecil dan tanpa Room (D5). Tiga tabel cukup untuk siklus ini:
 * `threads`, `messages`, dan `app_meta` untuk menyimpan versi skema serta penanda sync
 * nanti.
 *
 * Catatan: foreign key harus diaktifkan per koneksi. Tanpa `onConfigure`, klausa
 * `ON DELETE CASCADE` diabaikan diam-diam dan pesan akan tertinggal setelah thread dihapus.
 */
class NovaDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    NAME,
    null,
    VERSION,
) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_THREADS)
        db.execSQL(SQL_CREATE_MESSAGES)
        db.execSQL(SQL_CREATE_APP_META)
        db.execSQL(SQL_INDEX_MESSAGES_THREAD)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Belum ada migrasi. Selama versi masih 1, perubahan skema dilakukan dengan
        // menaikkan VERSION dan menulis langkah migrasi di sini.
    }

    companion object {
        const val NAME = "nova.db"
        const val VERSION = 1

        const val TABLE_THREADS = "threads"
        const val TABLE_MESSAGES = "messages"
        const val TABLE_APP_META = "app_meta"

        val SQL_CREATE_THREADS = """
            CREATE TABLE $TABLE_THREADS (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                model TEXT,
                pinned INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent()

        val SQL_CREATE_MESSAGES = """
            CREATE TABLE $TABLE_MESSAGES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                thread_id TEXT NOT NULL REFERENCES $TABLE_THREADS(id) ON DELETE CASCADE,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                reasoning TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'complete',
                created_at INTEGER NOT NULL
            )
        """.trimIndent()

        val SQL_CREATE_APP_META = """
            CREATE TABLE $TABLE_APP_META (
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT NOT NULL
            )
        """.trimIndent()

        val SQL_INDEX_MESSAGES_THREAD =
            "CREATE INDEX idx_messages_thread ON $TABLE_MESSAGES(thread_id, created_at)"
    }
}
