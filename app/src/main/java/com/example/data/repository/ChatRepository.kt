package com.example.data.repository

import com.example.data.local.db.ChatDao
import com.example.data.local.db.ConversationEntity
import com.example.data.local.db.ConversationSummary
import com.example.data.local.db.MessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    fun observeConversations(query: String = ""): Flow<List<ConversationSummary>> =
        chatDao.observeConversationSummaries(query)

    fun observeConversation(id: Long): Flow<ConversationEntity?> =
        chatDao.observeConversation(id)

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        chatDao.observeMessages(conversationId)

    fun observeConversationCount(): Flow<Int> = chatDao.observeConversationCount()

    fun observeMessageCount(): Flow<Int> = chatDao.observeMessageCount()

    suspend fun createConversation(firstPrompt: String, languageTag: String): Long {
        val cleanTitle = firstPrompt.trim()
            .replace("\n", " ")
            .let { if (it.length > 42) "${it.take(40)}…" else it }
            .ifBlank { "New AI Conversation" }
        val now = System.currentTimeMillis()
        return chatDao.insertConversation(
            ConversationEntity(
                title = cleanTitle,
                languageTag = languageTag,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun getMessagesOnce(conversationId: Long): List<MessageEntity> =
        chatDao.getMessagesOnce(conversationId)

    suspend fun addMessage(
        conversationId: Long,
        role: String,
        content: String,
        isError: Boolean = false
    ): Long {
        val now = System.currentTimeMillis()
        val msgId = chatDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                content = content,
                isError = isError,
                timestamp = now
            )
        )
        chatDao.getConversationById(conversationId)?.let { conv ->
            chatDao.updateConversation(conv.copy(updatedAt = now))
        }
        return msgId
    }

    suspend fun updateMessage(message: MessageEntity) {
        chatDao.updateMessage(message)
        chatDao.getConversationById(message.conversationId)?.let { conv ->
            chatDao.updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteMessageById(messageId: Long) = chatDao.deleteMessageById(messageId)

    suspend fun deleteConversation(conversationId: Long) =
        chatDao.deleteConversationById(conversationId)

    suspend fun clearAllHistory() = chatDao.deleteAllConversations()
}
