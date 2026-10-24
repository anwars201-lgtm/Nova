package dev.local.nova.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadTitleTest {

    @Test
    fun `memakai baris pertama yang tidak kosong`() {
        val text = "\n\n  Halo dunia  \nbaris kedua"
        assertEquals("Halo dunia", ThreadTitle.derive(text))
    }

    @Test
    fun `merapikan spasi berlebih`() {
        assertEquals("Halo   dunia".replace(Regex(" {3}"), " "), ThreadTitle.derive("Halo    dunia"))
    }

    @Test
    fun `teks kosong memakai fallback`() {
        assertEquals(ThreadTitle.FALLBACK, ThreadTitle.derive("   \n\t\n"))
    }

    @Test
    fun `teks pendek tidak dipotong`() {
        val text = "Pesan singkat"
        assertEquals(text, ThreadTitle.derive(text))
    }

    @Test
    fun `teks panjang dipotong di batas kata`() {
        val text = "Ini adalah kalimat yang sengaja dibuat panjang sekali agar melewati batas maksimum judul"
        val title = ThreadTitle.derive(text)

        assertTrue("harus berakhir dengan elipsis", title.endsWith("…"))
        assertTrue("tidak boleh melebihi batas", title.length <= ThreadTitle.MAX_LENGTH + 1)
        assertTrue("tidak boleh memotong kata", !title.removeSuffix("…").endsWith(" "))
    }

    @Test
    fun `kata yang lebih panjang dari batas tetap dipotong`() {
        val text = "a".repeat(120)
        val title = ThreadTitle.derive(text)
        assertTrue(title.endsWith("…"))
        assertTrue(title.length <= ThreadTitle.MAX_LENGTH + 1)
    }
}
