package dev.local.nova.net

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelListTest {

    @Test
    fun `daftar dirapikan dan diurutkan`() {
        val input = listOf(" zeta ", "Alpha", "beta", "Alpha", "")
        assertEquals(listOf("Alpha", "beta", "zeta"), ModelList.normalize(input))
    }

    @Test
    fun `penyaringan tidak membedakan huruf besar kecil`() {
        val models = listOf("gpt-4o", "gpt-4o-mini", "claude-3")
        assertEquals(listOf("gpt-4o", "gpt-4o-mini"), ModelList.filter(models, "GPT"))
        assertEquals(listOf("gpt-4o-mini"), ModelList.filter(models, "mini"))
    }

    @Test
    fun `kueri kosong mengembalikan semua`() {
        val models = listOf("a", "b")
        assertEquals(models, ModelList.filter(models, "   "))
    }

    @Test
    fun `tidak ada yang cocok menghasilkan daftar kosong`() {
        assertEquals(emptyList<String>(), ModelList.filter(listOf("a"), "zzz"))
    }
}
