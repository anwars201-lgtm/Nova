package dev.local.nova.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointTest {

    private fun valid(raw: String): String {
        val result = Endpoint.parse(raw)
        assertTrue("harus valid: $raw, dapat $result", result is EndpointResult.Valid)
        return (result as EndpointResult.Valid).baseUrl
    }

    private fun invalid(raw: String): String {
        val result = Endpoint.parse(raw)
        assertTrue("harus tidak valid: $raw, dapat $result", result is EndpointResult.Invalid)
        return (result as EndpointResult.Invalid).reason
    }

    @Test
    fun `https dengan trailing slash dinormalisasi`() {
        assertEquals("https://api.openai.com/v1", valid("https://api.openai.com/v1/"))
    }

    @Test
    fun `tanpa skema dianggap https`() {
        assertEquals("https://api.openai.com", valid("api.openai.com"))
    }

    @Test
    fun `http ditolak`() {
        val reason = invalid("http://localhost:1234/v1")
        assertTrue(reason.contains("https", ignoreCase = true))
    }

    @Test
    fun `kosong ditolak`() {
        invalid("   ")
    }

    @Test
    fun `spasi di tengah ditolak`() {
        invalid("https://api.openai.com /v1")
    }

    @Test
    fun `user dan password di URL ditolak`() {
        invalid("https://user:pass@api.openai.com/v1")
    }

    @Test
    fun `query ditolak`() {
        invalid("https://api.openai.com/v1?key=abc")
    }

    @Test
    fun `port dipertahankan`() {
        assertEquals("https://example.com:8443/v1", valid("https://example.com:8443/v1"))
    }

    @Test
    fun `modelsUrl menambah versi sekali saja`() {
        assertEquals(
            "https://api.openai.com/v1/models",
            Endpoint.modelsUrl("https://api.openai.com"),
        )
        assertEquals(
            "https://api.openai.com/v1/models",
            Endpoint.modelsUrl("https://api.openai.com/v1"),
        )
        assertEquals(
            "https://api.openai.com/v1/models",
            Endpoint.modelsUrl("https://api.openai.com/v1/"),
        )
    }

    @Test
    fun `chatUrl menambah versi sekali saja`() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            Endpoint.chatUrl("https://api.openai.com"),
        )
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            Endpoint.chatUrl("https://api.openai.com/v1/"),
        )
    }

    @Test
    fun `maskKey tidak membocorkan isi`() {
        val key = "sk-proj-abcdefghijklmnop"
        val masked = Endpoint.maskKey(key)
        assertTrue(masked.startsWith("sk-"))
        assertTrue(masked.endsWith("mnop"))
        assertTrue(masked.contains("…"))
        assertTrue("tidak boleh memuat key utuh", !masked.contains(key))
    }

    @Test
    fun `maskKey untuk kunci pendek`() {
        assertEquals("••••", Endpoint.maskKey("abcd"))
        assertEquals("", Endpoint.maskKey(""))
    }
}
