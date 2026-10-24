package dev.local.nova.data

import android.content.ContentValues
import android.database.Cursor
import dev.local.nova.data.model.Message
import dev.local.nova.data.model.MessageRole
import dev.local.nova.data.model.MessageStatus
import dev.local.nova.data.model.Thread
import java.util.UUID

/**
 * Akses baca-tulis ke [NovaDatabase].
 *
 * Semua operasi memakai SQL mentah dan `ContentValues`. Tidak ada state di dalam kelas ini,
 * jadi aman dibuat ulang selama database-nya sama.
 */
class ThreadRepository(private val database: NovaDatabase) {

    fun createThread(title: String = ThreadTitle.FALLBACK, model: String? = null): Thread {
        val now = System.currentTimeMillis()
        val thread = Thread(
            id = UUID.randomUUID().toString(),
            title = title,
            model = model,
            pinned = false,
            createdAt = now,
            updatedAt = now,
        )

        val values = ContentValues().apply {
            put("id", thread.id)
            put("title", thread.title)
            put("model", thread.model)
            put("pinned", 0)
            put("created_at", thread.createdAt)
            put("updated_at", thread.updatedAt)
        }
        database.writableDatabase.insertOrThrow(NovaDatabase.TABLE_THREADS, null, values)
        return thread
    }

    fun threads(): List<Thread> {
        val sql = """
            SELECT id, title, model, pinned, created_at, updated_at
            FROM ${NovaDatabase.TABLE_THREADS}
            ORDER BY pinned DESC, updated_at DESC
        """.trimIndent()

        return database.readableDatabase.rawQuery(sql, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toThread())
            }
        }
    }

    fun thread(id: String): Thread? {
        val sql = "SELECT id, title, model, pinned, created_at, updated_at FROM ${NovaDatabase.TABLE_THREADS} WHERE id = ?"
        return database.readableDatabase.rawQuery(sql, arrayOf(id)).use { cursor ->
            if (cursor.moveToFirst()) cursor.toThread() else null
        }
    }

    fun rename(id: String, title: String) {
        val values = ContentValues().apply {
            put("title", title)
            put("updated_at", System.currentTimeMillis())
        }
        database.writableDatabase.update(NovaDatabase.TABLE_THREADS, values, "id = ?", arrayOf(id))
    }

    fun setPinned(id: String, pinned: Boolean) {
        val values = ContentValues().apply { put("pinned", if (pinned) 1 else 0) }
        database.writableDatabase.update(NovaDatabase.TABLE_THREADS, values, "id = ?", arrayOf(id))
    }

    /** Menghapus thread beserta seluruh pesannya lewat ON DELETE CASCADE. */
    fun deleteThread(id: String) {
        database.writableDatabase.delete(NovaDatabase.TABLE_THREADS, "id = ?", arrayOf(id))
    }

    fun addMessage(
        threadId: String,
        role: MessageRole,
        content: String,
        reasoning: String = "",
        status: MessageStatus = MessageStatus.COMPLETE,
    ): Long {
        val values = ContentValues().apply {
            put("thread_id", threadId)
            put("role", role.wire)
            put("content", content)
            put("reasoning", reasoning)
            put("status", status.wire)
            put("created_at", System.currentTimeMillis())
        }
        val id = database.writableDatabase.insertOrThrow(NovaDatabase.TABLE_MESSAGES, null, values)
        touchThread(threadId)
        return id
    }

    fun updateMessageContent(id: Long, content: String, status: MessageStatus) {
        val values = ContentValues().apply {
            put("content", content)
            put("status", status.wire)
        }
        database.writableDatabase.update(NovaDatabase.TABLE_MESSAGES, values, "id = ?", arrayOf(id.toString()))
    }

    fun messages(threadId: String): List<Message> {
        val sql = """
            SELECT id, thread_id, role, content, reasoning, status, created_at
            FROM ${NovaDatabase.TABLE_MESSAGES}
            WHERE thread_id = ?
            ORDER BY created_at ASC, id ASC
        """.trimIndent()

        return database.readableDatabase.rawQuery(sql, arrayOf(threadId)).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toMessage())
            }
        }
    }

    fun messageCount(threadId: String): Int {
        val sql = "SELECT COUNT(*) FROM ${NovaDatabase.TABLE_MESSAGES} WHERE thread_id = ?"
        return database.readableDatabase.rawQuery(sql, arrayOf(threadId)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun touchThread(id: String) {
        val values = ContentValues().apply { put("updated_at", System.currentTimeMillis()) }
        database.writableDatabase.update(NovaDatabase.TABLE_THREADS, values, "id = ?", arrayOf(id))
    }

    private fun Cursor.toThread(): Thread = Thread(
        id = getString(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        model = if (isNull(getColumnIndexOrThrow("model"))) null else getString(getColumnIndexOrThrow("model")),
        pinned = getInt(getColumnIndexOrThrow("pinned")) != 0,
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
    )

    private fun Cursor.toMessage(): Message = Message(
        id = getLong(getColumnIndexOrThrow("id")),
        threadId = getString(getColumnIndexOrThrow("thread_id")),
        role = MessageRole.fromWire(getString(getColumnIndexOrThrow("role"))) ?: MessageRole.USER,
        content = getString(getColumnIndexOrThrow("content")),
        reasoning = getString(getColumnIndexOrThrow("reasoning")),
        status = MessageStatus.fromWire(getString(getColumnIndexOrThrow("status"))),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
    )
}
