package com.youtubeyar.app.youtube

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مستقل از UI — تحلیل کانال با YouTube Data API v3:
 * ۱) channels.list برای آمار کانال  ۲) playlistItems.list برای ۱۰ ویدیوی آخر
 * ۳) videos.list برای بازدید/لایک/کامنت‌ها  ۴) تولید بینش‌های خودکار
 */
@Singleton
class ChannelAnalyzer @Inject constructor(private val service: YouTubeService) {

    suspend fun analyze(handleOrId: String, apiKey: String): Result<ChannelAnalysis> {
        if (apiKey.isBlank()) return Result.failure(IllegalStateException("NO_API_KEY"))
        val input = handleOrId.trim().removePrefix("https://www.youtube.com/")
            .removePrefix("http://youtube.com/").removePrefix("www.")
            .removePrefix("youtube.com/").removePrefix("@").takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalArgumentException("EMPTY_HANDLE"))

        val isChannelId = input.startsWith("UC") && input.length >= 20

        val channelResp = try {
            service.channels(
                parts = "snippet,statistics,contentDetails",
                forHandle = if (isChannelId) null else "@$input",
                id = if (isChannelId) input else null,
                key = apiKey
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val channel = channelResp.items.firstOrNull()
            ?: return Result.failure(IllegalStateException("CHANNEL_NOT_FOUND"))

        val stats = channel.statistics
        val subs = stats.subscriberCount.toLongOrNull() ?: 0
        val totalViews = stats.viewCount.toLongOrNull() ?: 0
        val videoCount = stats.videoCount.toLongOrNull() ?: 0
        val uploadsPlaylist = channel.contentDetails?.relatedPlaylists?.uploads.orEmpty()

        // ویدیوهای اخیر
        var recent: List<VideoItem> = emptyList()
        if (uploadsPlaylist.isNotBlank()) {
            try {
                val pl = service.playlistItems("contentDetails", uploadsPlaylist, 10, apiKey)
                val ids = pl.items.map { it.contentDetails.videoId }.filter { it.isNotBlank() }
                if (ids.isNotEmpty()) {
                    recent = service.videos("snippet,statistics", ids.joinToString(","), apiKey).items
                }
            } catch (_: Exception) {
            }
        }

        val avgViews = if (recent.isNotEmpty()) recent.mapNotNull { it.statistics.viewCount.toLongOrNull() }.average().toLong() else 0
        val engagement = if (recent.isNotEmpty() && avgViews > 0) {
            val likes = recent.sumOf { (it.statistics.likeCount.toLongOrNull() ?: 0).toDouble() }
            val comments = recent.sumOf { (it.statistics.commentCount.toLongOrNull() ?: 0).toDouble() }
            ((likes + comments) / avgViews) * 100.0
        } else 0.0

        val ageDays = try {
            val published = OffsetDateTime.parse(channel.snippet.publishedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            Duration.between(published.toInstant(), Instant.now()).toDays()
        } catch (_: Exception) { 0 }

        val videosPerMonth = if (ageDays > 0 && videoCount > 0)
            videoCount / (ageDays / 30.4) else 0.0

        val topVideo = recent.maxByOrNull { it.statistics.viewCount.toLongOrNull() ?: 0 }

        val analysis = ChannelAnalysis(
            id = channel.id,
            title = channel.snippet.title,
            handle = channel.snippet.customUrl,
            subscribers = subs,
            totalViews = totalViews,
            videoCount = videoCount,
            publishedAt = channel.snippet.publishedAt,
            channelAgeDays = ageDays,
            avgViews = avgViews,
            videosPerMonth = videosPerMonth,
            engagementRate = engagement,
            viewSubRatio = if (subs > 0) avgViews.toDouble() / subs else 0.0,
            topVideo = topVideo,
            recentVideos = recent,
            insights = buildInsights(subs, avgViews, videosPerMonth, engagement, recent.size)
        )
        return Result.success(analysis)
    }

    private fun buildInsights(
        subs: Long, avgViews: Long, videosPerMonth: Double,
        engagement: Double, recentCount: Int
    ): List<String> {
        val out = mutableListOf<String>()
        if (recentCount == 0) out += "ویدیوی اخیری یافت نشد؛ کانال را فعال نگه دارید و ویدیوی جدید منتشر کنید."
        if (videosPerMonth in 0.1..2.9) out += "تناوب انتشار کم است؛ حداقل هفته‌ای ۱ ویدیو منتشر کنید تا الگوریتم شما را جدی بگیرد."
        if (videosPerMonth >= 3.0) out += "ریتم انتشار خوبی دارید؛ حالا روی کیفیت ۵۰ ثانیه اول ویدیوها تمرکز کنید."
        if (subs > 0 && avgViews > 0 && avgViews < subs * 0.05) out += "بازدید میانگین نسبت به سابسکرایبر پایین است؛ تامبنیل و عنوان را بازطراحی کنید (CTR را در Studio ببینید)."
        if (subs > 0 && avgViews > subs * 0.2) out += "بازدید میانگین از سابسکرایبر بیشتر است؛ یعنی یوتیوب شما را به تازه‌واردها پیشنهاد می‌دهد — همین مسیر را تقویت کنید."
        if (engagement > 3.0) out += "نرخ تعامل عالی است (٪${"%.1f".format(engagement)})؛ با مخاطب فعلی بیشتر کار کنید: پست گروه و Q&A."
        if (engagement in 0.01..1.0) out += "نرخ تعامل پایین است؛ پایان هر ویدیو یک سؤال مشخص بپرسید و به همه کامنت‌ها در ۴۸ ساعت پاسخ بدهید."
        if (out.isEmpty()) out += "با ثبت آمار هفتگی در ردیاب پیشرفت، روند رشد کانال را زیر نظر بگیرید."
        return out
    }
}
