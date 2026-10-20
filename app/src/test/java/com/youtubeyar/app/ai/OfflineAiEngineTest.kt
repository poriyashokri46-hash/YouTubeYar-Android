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
    @Test
    fun scriptTemplatesAvoidUnsupportedStatisticsAndPromises() {
        val fa = OfflineAiEngine.generate(
            tool = AiTool.SCRIPT,
            topic = "رشد کانال",
            niche = "یوتیوب",
            tone = AiTone.EDUCATIONAL,
            isEnglish = false,
            seed = 8L
        ).sections.joinToString(" ") { it.content }
        val en = OfflineAiEngine.generate(
            tool = AiTool.SCRIPT,
            topic = "channel growth",
            niche = "YouTube",
            tone = AiTone.EDUCATIONAL,
            isEnglish = true,
            seed = 8L
        ).sections.joinToString(" ") { it.content }
        assertFalse(fa.contains("۹۰٪"))
        assertFalse(fa.contains("کسی به شما نمی‌گوید"))
        assertFalse(en.contains("nobody tells you"))
    }

    @Test
    fun generatedHashtagsDoNotContainSpacesInsideTags() {
        val result = OfflineAiEngine.generate(
            tool = AiTool.DESCRIPTION,
            topic = "آموزش تدوین ویدیو",
            niche = "تولید محتوا",
            tone = AiTone.EDUCATIONAL,
            isEnglish = false,
            seed = 9L
        )
        val content = result.sections.first().content
        assertTrue(content.contains("#آموزش_تدوین_ویدیو"))
        assertTrue(content.contains("#تولید_محتوا"))
        assertFalse(content.contains("#آموزش تدوین"))
    }

    @Test
    fun titleScoresAreDisclosedAsHeuristicInBothLanguages() {
        val fa = OfflineAiEngine.generate(
            tool = AiTool.TITLE,
            topic = "رشد کانال",
            niche = "یوتیوب",
            tone = AiTone.EDUCATIONAL,
            isEnglish = false,
            seed = 10L
        )
        val en = OfflineAiEngine.generate(
            tool = AiTool.TITLE,
            topic = "channel growth",
            niche = "YouTube",
            tone = AiTone.EDUCATIONAL,
            isEnglish = true,
            seed = 10L
        )
        assertTrue(fa.sections.first().title.contains("غیررسمی"))
        assertTrue(en.sections.first().title.contains("not an official YouTube metric"))
    }

}
