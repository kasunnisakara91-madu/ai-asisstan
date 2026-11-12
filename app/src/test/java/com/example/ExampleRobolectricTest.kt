package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.CodeSyntaxHighlighter
import com.example.data.local.db.ChatDatabase
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: ChatDatabase
    private lateinit var repository: ChatRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ChatRepository(database.chatDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `app name matches AI ASISSTANT`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI ASISSTANT", appName)
    }

    @Test
    fun `room conversation create search and delete flow works`() = runTest {
        val convId = repository.createConversation("Sinhala AI tutorial in Kotlin", "AUTO")
        repository.addMessage(convId, "user", "Explain Kotlin flows")
        repository.addMessage(convId, "model", "Kotlin Flow is a cold asynchronous stream.")

        val all = repository.observeConversations("").first()
        assertEquals(1, all.size)
        assertEquals(2, all.first().messageCount)

        val matched = repository.observeConversations("asynchronous").first()
        assertEquals(1, matched.size)

        val unmatched = repository.observeConversations("nonexistent-topic").first()
        assertTrue(unmatched.isEmpty())

        repository.deleteConversation(convId)
        val afterDelete = repository.observeConversations("").first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `syntax highlighter applies span styles to code`() {
        val highlighted = CodeSyntaxHighlighter.highlight(
            code = "fun greet() { val msg = \"Ayubowan\" }",
            language = "kotlin"
        )
        assertTrue(highlighted.spanStyles.isNotEmpty())
        assertEquals("fun greet() { val msg = \"Ayubowan\" }", highlighted.text)
    }
}
