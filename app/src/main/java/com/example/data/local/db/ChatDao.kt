package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query(
        """
        SELECT c.id, c.title, c.languageTag, c.createdAt, c.updatedAt,
            (SELECT m.content FROM messages m WHERE m.conversationId = c.id ORDER BY m.timestamp DESC, m.id DESC LIMIT 1) AS lastMessagePreview,
            (SELECT COUNT(*) FROM messages m WHERE m.conversationId = c.id) AS messageCount
        FROM conversations c
        WHERE (:query = '' OR c.title LIKE '%' || :query || '%' OR EXISTS (
            SELECT 1 FROM messages m WHERE m.conversationId = c.id AND m.content LIKE '%' || :query || '%'
        ))
        ORDER BY c.updatedAt DESC
        """
    )
    fun observeConversationSummaries(query: String = ""): Flow<List<ConversationSummary>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversation(conversationId: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: Long): ConversationEntity?

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    suspend fun getMessagesOnce(conversationId: Long): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM conversations")
    fun observeConversationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages")
    fun observeMessageCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}
