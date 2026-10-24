package dev.local.nova.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseParserTest {

    private fun deltaLine(json: String) = "data: $json"

    @Test
    fun `mengambil potongan teks`() {
        val line = deltaLine("""{"choices":[{"delta":{"content":"Halo"}}]}""")
        assertEquals("Halo", SseParser.delta(line))
    }

    @Test
    fun `potongan kosong diabaikan`() {
        val line = deltaLine("""{"choices":[{"delta":{"content":""}}]}""")
        assertNull(SseParser.delta(line))
    }

    @Test
    fun `delta peran tanpa isi diabaikan`() {
        val line = deltaLine("""{"choices":[{"delta":{"role":"assistant"}}]}""")
        assertNull(SseParser.delta(line))
    }

    @Test
    fun `penanda selesai dikenali`() {
        assertTrue(SseParser.isDone("data: [DONE]"))
        assertTrue(SseParser.isDone("data:[DONE]"))
        assertNull(SseParser.delta("data: [DONE]"))
    }

    @Test
    fun `baris bukan data diabaikan`() {
        assertNull(SseParser.delta("event: message"))
        assertNull(SseParser.delta(""))
        assertNull(SseParser.delta(": komentar"))
    }

    @Test
    fun `json rusak tidak melempar`() {
        assertNull(SseParser.delta(deltaLine("{bukan json")))
        assertNull(SseParser.delta(deltaLine("""{"choices":[]}""")))
        assertNull(SseParser.delta(deltaLine("""{"id":"x"}""")))
    }

    @Test
    fun `pesan galat di tengah stream terbaca`() {
        val line = deltaLine("""{"error":{"message":"model tidak dikenal"}}""")
        assertEquals("model tidak dikenal", SseParser.errorMessage(line))
        assertNull(SseParser.errorMessage(deltaLine("""{"choices":[{"delta":{"content":"a"}}]}""")))
    }

    @Test
    fun `spasi setelah prefiks ditoleransi`() {
        assertEquals("a", SseParser.delta("data:{\"choices\":[{\"delta\":{\"content\":\"a\"}}]}"))
        assertEquals("b", SseParser.delta("data:   {\"choices\":[{\"delta\":{\"content\":\"b\"}}]}"))
    }
}
