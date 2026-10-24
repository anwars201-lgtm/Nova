package dev.local.nova.data

import dev.local.nova.data.model.Memory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private fun memory(id: Long, text: String, active: Boolean = true) =
        Memory(id = id, content = text, createdAt = id, active = active)

    @Test
    fun `tanpa apa pun menghasilkan null`() {
        assertNull(PromptBuilder.build(null, emptyList()).system)
        assertNull(PromptBuilder.build("   ", emptyList()).system)
    }

    @Test
    fun `hanya prompt manual`() {
        val prompt = PromptBuilder.build("Jawab ringkas.", emptyList())
        assertEquals("Jawab ringkas.", prompt.system)
        assertEquals(0, prompt.included)
        assertEquals(0, prompt.dropped)
    }

    @Test
    fun `prompt manual di depan memori`() {
        val prompt = PromptBuilder.build(
            "Jawab ringkas.",
            listOf(memory(1, "Suka kopi"), memory(2, "Tinggal di Bandung")),
        )
        val system = prompt.system!!
        assertTrue(system.startsWith("Jawab ringkas."))
        assertTrue(system.indexOf("Suka kopi") > system.indexOf("Jawab ringkas."))
        assertTrue(system.contains("- Suka kopi"))
        assertEquals(2, prompt.included)
        assertTrue(!prompt.hitLimit)
    }

    @Test
    fun `memori nonaktif tidak dikirim`() {
        val prompt = PromptBuilder.build(
            null,
            listOf(memory(1, "Aktif", true), memory(2, "Mati", false)),
        )
        val system = prompt.system!!
        assertTrue(system.contains("Aktif"))
        assertTrue(!system.contains("Mati"))
        assertEquals(1, prompt.included)
    }

    @Test
    fun `batas entri dipatuhi`() {
        val many = (1..60).map { memory(it.toLong(), "memori $it") }
        val prompt = PromptBuilder.build(null, many)
        assertEquals(PromptBuilder.MAX_ENTRIES, prompt.included)
        assertEquals(10, prompt.dropped)
        assertTrue(prompt.hitLimit)
    }

    @Test
    fun `batas karakter dipatuhi`() {
        val big = (1..40).map { memory(it.toLong(), "x".repeat(200)) }
        val prompt = PromptBuilder.build(null, big)
        assertTrue("karakter ${prompt.characters}", prompt.characters <= PromptBuilder.MAX_CHARS)
        assertTrue(prompt.dropped > 0)
        assertTrue(prompt.hitLimit)
    }

    @Test
    fun `memori kosong diabaikan`() {
        val prompt = PromptBuilder.build(null, listOf(memory(1, "   ")))
        assertNull(prompt.system)
        assertEquals(0, prompt.included)
    }
}
