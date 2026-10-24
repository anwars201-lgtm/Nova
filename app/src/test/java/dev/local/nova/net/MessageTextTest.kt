package dev.local.nova.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTextTest {

    @Test
    fun `spasi di ujung dibuang`() {
        val result = MessageText.prepare("  halo  ")
        assertEquals(MessageText.Result.Ready("halo"), result)
    }

    @Test
    fun `kosong ditolak`() {
        assertTrue(MessageText.prepare("   \n ") is MessageText.Result.Rejected)
    }

    @Test
    fun `terlalu panjang ditolak`() {
        val text = "a".repeat(MessageText.MAX_LENGTH + 1)
        assertTrue(MessageText.prepare(text) is MessageText.Result.Rejected)
    }

    @Test
    fun `batas maksimum masih diterima`() {
        val text = "a".repeat(MessageText.MAX_LENGTH)
        assertTrue(MessageText.prepare(text) is MessageText.Result.Ready)
    }
}
