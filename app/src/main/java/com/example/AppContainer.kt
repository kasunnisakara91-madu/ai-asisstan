package com.example

import android.content.Context
import com.example.ai.AiService
import com.example.data.local.db.ChatDatabase
import com.example.data.local.preferences.SettingsRepository
import com.example.data.pdf.PdfProcessor
import com.example.data.remote.WebSearchRepository
import com.example.data.repository.ChatRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: ChatDatabase by lazy { ChatDatabase.getInstance(appContext) }
    val chatRepository: ChatRepository by lazy { ChatRepository(database.chatDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val aiService: AiService by lazy { AiService(appContext, settingsRepository) }
    val webSearchRepository: WebSearchRepository by lazy { WebSearchRepository(settingsRepository) }
    val pdfProcessor: PdfProcessor by lazy { PdfProcessor(appContext) }
}
