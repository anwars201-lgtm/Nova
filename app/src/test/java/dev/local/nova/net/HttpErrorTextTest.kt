package dev.local.nova.net

import org.junit.Assert.assertTrue
import org.junit.Test

class HttpErrorTextTest {

    private fun describe(code: Int, body: String) = HttpErrorText.describe(code, body)

    @Test
    fun `400 dengan kata parameter menunjuk effort dan max tokens`() {
        val text = describe(400, """{"error":"Invalid inference request. Check the model's supported parameters and input limits."}""")
        assertTrue(text, text.contains("reasoning effort"))
        assertTrue(text, text.contains("max tokens"))
        assertTrue("isi asli harus ikut", text.contains("Invalid inference"))
    }

    @Test
    fun `422 dengan unsupported juga menunjuk parameter`() {
        val text = describe(422, "unsupported parameter: reasoning_effort")
        assertTrue(text, text.contains("parameter"))
    }

    @Test
    fun `400 tanpa kata parameter tetap menyebut model`() {
        val text = describe(400, """{"error":"model not found"}""")
        assertTrue(text, text.contains("model"))
        assertTrue(text, !text.contains("reasoning effort"))
    }

    @Test
    fun `401 menyebut api key`() {
        assertTrue(describe(401, "").contains("API key"))
    }

    @Test
    fun `404 menyebut base url`() {
        assertTrue(describe(404, "").contains("base URL"))
    }

    @Test
    fun `429 menyebut kuota`() {
        assertTrue(describe(429, "").contains("kuota"))
    }

    @Test
    fun `5xx dianggap masalah server`() {
        assertTrue(describe(503, "").contains("Server provider bermasalah"))
    }

    @Test
    fun `badan panjang dipotong`() {
        val text = describe(400, "x".repeat(1000))
        assertTrue("panjang ${text.length}", text.length < 400)
    }

    @Test
    fun `badan kosong tetap memberi kode`() {
        assertTrue(describe(418, "").contains("HTTP 418"))
    }
}
