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
        val first = OfflineAiEngine.seoScore(title)
        val second = OfflineAiEngine.seoScore(title)

        assertEquals(first, second)
        assertTrue("Score should be within 1..10", first in 1..10)
        assertEquals(1, OfflineAiEngine.seoScore(""))
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
