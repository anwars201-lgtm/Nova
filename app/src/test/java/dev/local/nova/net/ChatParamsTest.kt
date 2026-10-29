package dev.local.nova.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatParamsTest {

    @Test
    fun `max tokens kosong berarti tidak dipakai`() {
        assertEquals(ChatParams.TokensResult.Off, ChatParams.parseMaxTokens(""))
        assertEquals(ChatParams.TokensResult.Off, ChatParams.parseMaxTokens("   "))
    }

    @Test
    fun `max tokens valid`() {
        assertEquals(ChatParams.TokensResult.Value(1024), ChatParams.parseMaxTokens(" 1024 "))
    }

    @Test
    fun `max tokens nol dan negatif ditolak`() {
        assertTrue(ChatParams.parseMaxTokens("0") is ChatParams.TokensResult.Invalid)
        assertTrue(ChatParams.parseMaxTokens("-5") is ChatParams.TokensResult.Invalid)
    }

    @Test
    fun `max tokens bukan angka ditolak`() {
        assertTrue(ChatParams.parseMaxTokens("banyak") is ChatParams.TokensResult.Invalid)
        assertTrue(ChatParams.parseMaxTokens("1.5") is ChatParams.TokensResult.Invalid)
    }

    @Test
    fun `max tokens melebihi batas ditolak`() {
        val tooBig = (ChatParams.MAX_TOKENS + 1).toString()
        assertTrue(ChatParams.parseMaxTokens(tooBig) is ChatParams.TokensResult.Invalid)
    }

    @Test
    fun `tanpa opsi tidak ada field tambahan`() {
        assertTrue(ChatParams.extraFields(ChatOptions()).isEmpty())
    }

    @Test
    fun `hanya effort yang diisi`() {
        val fields = ChatParams.extraFields(ChatOptions(reasoningEffort = "high"))
        assertEquals(mapOf("reasoning_effort" to "high"), fields)
    }

    @Test
    fun `hanya max tokens yang diisi`() {
        val fields = ChatParams.extraFields(ChatOptions(maxTokens = 512))
        assertEquals(mapOf("max_tokens" to 512), fields)
    }

    @Test
    fun `effort kosong atau spasi tidak dikirim`() {
        assertTrue(ChatParams.extraFields(ChatOptions(reasoningEffort = "  ")).isEmpty())
    }

    @Test
    fun `effort dirapikan spasinya`() {
        val fields = ChatParams.extraFields(ChatOptions(reasoningEffort = " minimal "))
        assertEquals(mapOf("reasoning_effort" to "minimal"), fields)
    }

    @Test
    fun `keduanya terisi`() {
        val fields = ChatParams.extraFields(ChatOptions(reasoningEffort = "low", maxTokens = 256))
        assertEquals(2, fields.size)
        assertEquals("low", fields["reasoning_effort"])
        assertEquals(256, fields["max_tokens"])
    }
}
