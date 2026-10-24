package dev.local.nova.data

import android.content.ContentValues
import android.database.Cursor
import dev.local.nova.data.model.Memory

/**
 * Penyimpanan memori di SQLite.
 *
 * Tabel `memories` adalah sumber kebenaran: UI membaca dari sini, dan setiap permintaan
 * chat menyusun ulang system prompt dari isi tabel, bukan dari state di memori aplikasi.
 */
class MemoryRepository(private val database: NovaDatabase) {

    fun add(content: String): Long {
        val values = ContentValues().apply {
            put("content", content)
            put("created_at", System.currentTimeMillis())
            put("active", 1)
        }
        return database.writableDatabase.insertOrThrow(NovaDatabase.TABLE_MEMORIES, null, values)
    }

    fun all(): List<Memory> = query("SELECT id, content, created_at, active FROM ${NovaDatabase.TABLE_MEMORIES} ORDER BY created_at ASC, id ASC")

    fun active(): List<Memory> = query("SELECT id, content, created_at, active FROM ${NovaDatabase.TABLE_MEMORIES} WHERE active = 1 ORDER BY created_at ASC, id ASC")

    fun updateContent(id: Long, content: String) {
        val values = ContentValues().apply { put("content", content) }
        database.writableDatabase.update(NovaDatabase.TABLE_MEMORIES, values, "id = ?", arrayOf(id.toString()))
    }

    fun setActive(id: Long, active: Boolean) {
        val values = ContentValues().apply { put("active", if (active) 1 else 0) }
        database.writableDatabase.update(NovaDatabase.TABLE_MEMORIES, values, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        database.writableDatabase.delete(NovaDatabase.TABLE_MEMORIES, "id = ?", arrayOf(id.toString()))
    }

    fun count(activeOnly: Boolean = false): Int {
        val sql = if (activeOnly) {
            "SELECT COUNT(*) FROM ${NovaDatabase.TABLE_MEMORIES} WHERE active = 1"
        } else {
            "SELECT COUNT(*) FROM ${NovaDatabase.TABLE_MEMORIES}"
        }
        return database.readableDatabase.rawQuery(sql, null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun query(sql: String): List<Memory> =
        database.readableDatabase.rawQuery(sql, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toMemory())
            }
        }

    private fun Cursor.toMemory(): Memory = Memory(
        id = getLong(getColumnIndexOrThrow("id")),
        content = getString(getColumnIndexOrThrow("content")),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        active = getInt(getColumnIndexOrThrow("active")) != 0,
    )
}
