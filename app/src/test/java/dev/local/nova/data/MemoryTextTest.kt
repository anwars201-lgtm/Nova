package dev.local.nova.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryTextTest {

    @Test
    fun `spasi berlebih dirapikan`() {
        val result = MemoryText.prepare("  pengguna   suka   kopi  ")
        assertEquals(MemoryText.Result.Ready("pengguna suka kopi"), result)
    }

    @Test
    fun `kosong ditolak`() {
        assertTrue(MemoryText.prepare("   \n ") is MemoryText.Result.Rejected)
    }

    @Test
    fun `melebihi batas ditolak`() {
        val text = "a".repeat(MemoryText.MAX_LENGTH + 1)
        assertTrue(MemoryText.prepare(text) is MemoryText.Result.Rejected)
    }

    @Test
    fun `tepat di batas diterima`() {
        val text = "a".repeat(MemoryText.MAX_LENGTH)
        assertTrue(MemoryText.prepare(text) is MemoryText.Result.Ready)
    }
}
