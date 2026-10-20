package com.youtubeyar.app.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAiEngineTest {

    @Test
    fun seoScoreAlwaysStaysWithinItsDocumentedRange() {
        listOf("", "عنوان کوتاه", "x".repeat(200), "آموزش حرفه‌ای 2026؟").forEach { title ->
            assertTrue(OfflineAiEngine.seoScore(title) in 1..10)
        }
    }

    @Test
    fun everyToolReturnsEditableNonEmptyOfflineSuggestions() {
        AiTool.values().forEach { tool ->
            val result = OfflineAiEngine.generate(
                tool = tool,
                topic = "آموزش تدوین ویدیو",
                niche = "آموزش",
                tone = AiTone.EDUCATIONAL,
                isEnglish = false,
                seed = 1234L
            )
            assertNotNull(result)
            assertTrue(result.sections.isNotEmpty())
            assertTrue(result.sections.all { it.title.isNotBlank() && it.content.isNotBlank() })
            assertTrue(result.engine == "offline")
        }
    }

    @Test
    fun offlineDescriptionsMarkTimestampsAsSuggestions() {
        val result = OfflineAiEngine.generate(
            tool = AiTool.DESCRIPTION,
            topic = "آموزش تدوین ویدیو",
            niche = "آموزش",
            tone = AiTone.EDUCATIONAL,
            isEnglish = false,
            seed = 7L
        )
        assertTrue(result.sections.first().content.contains("زمان‌بندی پیشنهادی"))
    }

    @Test
    fun offlineTitlesAvoidUnsupportedSpecificStatistics() {
        val result = OfflineAiEngine.generate(
            tool = AiTool.TITLE,
            topic = "رشد کانال",
            niche = "یوتیوب",
            tone = AiTone.EDUCATIONAL,
            isEnglish = false,
            seed = 3L
        )
        val output = result.sections.joinToString(" ") { it.content }
        assertFalse(output.contains("۹۰٪"))
        assertFalse(output.contains("۱۰ برابر"))
    }
}
