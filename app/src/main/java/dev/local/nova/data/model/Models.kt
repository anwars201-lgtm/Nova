package dev.local.nova.data.model

data class Thread(
    val id: String,
    val title: String,
    val model: String?,
    val pinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Message(
    val id: Long,
    val threadId: String,
    val role: MessageRole,
    val content: String,
    val reasoning: String,
    val status: MessageStatus,
    val createdAt: Long,
)
