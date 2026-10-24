package dev.local.nova.data

import dev.local.nova.data.model.MessageRole
import dev.local.nova.data.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WireValueTest {

    @Test
    fun `peran dikenali tanpa peduli huruf besar kecil`() {
        assertEquals(MessageRole.USER, MessageRole.fromWire("user"))
        assertEquals(MessageRole.ASSISTANT, MessageRole.fromWire("ASSISTANT"))
        assertEquals(MessageRole.TOOL, MessageRole.fromWire("Tool"))
    }

    @Test
    fun `peran tak dikenal menghasilkan null`() {
        assertNull(MessageRole.fromWire("narrator"))
        assertNull(MessageRole.fromWire(null))
    }

    @Test
    fun `nilai wire unik`() {
        val wires = MessageRole.entries.map { it.wire }
        assertEquals(wires.size, wires.toSet().size)
    }

    @Test
    fun `status tak dikenal jatuh ke complete`() {
        assertEquals(MessageStatus.COMPLETE, MessageStatus.fromWire("entah"))
        assertEquals(MessageStatus.STREAMING, MessageStatus.fromWire("streaming"))
        assertEquals(MessageStatus.FAILED, MessageStatus.fromWire("failed"))
    }
}
