package com.youtubeyar.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAiEngineTest {

    @Test
    fun seoScoreAlwaysStaysWithinDocumentedRange() {
        val samples = listOf(
            "",
            "رشد کانال",
            "بهترین ۵ روش برای رشد کانال یوتیوب؟",
            "A very long title ".repeat(12),
            "How to learn Kotlin"
        )

        samples.forEach { title ->
            assertTrue(
                "Score should be in 1..10 for title: $title",
                OfflineAiEngine.seoScore(title) in 1..10
            )
        }
    }

    @Test
    fun seoScoreIsDeterministicForSameInput() {
        val title = "بهترین ۵ روش برای رشد کانال یوتیوب؟"
        assertEquals(OfflineAiEngine.seoScore(title), OfflineAiEngine.seoScore(title))
    }

    @Test
    fun relevantTitleSignalsCanImproveScore() {
        val plainTitle = "رشد کانال"
        val descriptiveTitle = "بهترین ۵ روش برای رشد کانال یوتیوب؟"

        assertTrue(
            OfflineAiEngine.seoScore(descriptiveTitle) > OfflineAiEngine.seoScore(plainTitle)
        )
    }
}
