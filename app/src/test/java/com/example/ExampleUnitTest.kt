package com.example

import com.example.ai.CodeSyntaxHighlighter
import com.example.data.local.preferences.AppSettings
import com.example.data.local.preferences.WebSearchProvider
import com.example.security.KeystoreCryptoManager
import com.example.ui.screens.CodingLanguage
import com.example.ui.screens.CodingTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun maskSecret_masksShortAndLongKeys() {
        assertEquals("Not configured", KeystoreCryptoManager.maskSecret(""))
        assertEquals("••••••••", KeystoreCryptoManager.maskSecret("short12"))
        assertEquals("AIza••••9xyz", KeystoreCryptoManager.maskSecret("AIzaSy1234569xyz"))
    }

    @Test
    fun codingAssistant_supportsAllNineRequestedLanguagesAndTasks() {
        val expected = setOf(
            "HTML", "CSS", "JavaScript", "Python",
            "Java", "Kotlin", "C++", "PHP", "JSON"
        )
        assertEquals(expected, CodingLanguage.entries.map { it.displayName }.toSet())
        assertEquals(5, CodingTask.entries.size)
    }

    @Test
    fun webSearchProviders_enforceConfigurationFlagsCorrectly() {
        assertFalse(WebSearchProvider.WIKIPEDIA_LIVE.requiresApiKey)
        assertTrue(WebSearchProvider.GOOGLE_CUSTOM_SEARCH.requiresApiKey)
        assertTrue(WebSearchProvider.SERPER_DEV.requiresApiKey)
        assertTrue(WebSearchProvider.TAVILY_AI.requiresApiKey)
    }

    @Test
    fun appSettings_defaultsStartCleanAndSecure() {
        val defaults = AppSettings()
        assertFalse(defaults.hasGeminiApiKey)
        assertFalse(defaults.hasImageGenApiKey)
        assertFalse(defaults.hasWebSearchApiKey)
        assertEquals("gemini-3.8-flash", defaults.geminiModelName)
        assertEquals(WebSearchProvider.WIKIPEDIA_LIVE, defaults.webSearchProvider)
    }

    @Test
    fun syntaxHighlighter_highlightsPythonAndJsonSafely() {
        val py = CodeSyntaxHighlighter.highlight("def greet():\n    return \"Ayubowan\"", "python")
        assertTrue(py.spanStyles.isNotEmpty())
        val json = CodeSyntaxHighlighter.highlight("{\"ok\": true, \"count\": 3}", "json")
        assertTrue(json.spanStyles.isNotEmpty())
    }
}
