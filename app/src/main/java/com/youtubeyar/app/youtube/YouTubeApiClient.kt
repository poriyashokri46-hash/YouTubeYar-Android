package com.youtubeyar.app.youtube

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/* ------------------------------- مدل‌های API ------------------------------- */

data class ChannelResponse(val items: List<ChannelItem> = emptyList())

data class ChannelItem(
    val id: String = "",
    val snippet: ChannelSnippet = ChannelSnippet(),
    val statistics: ChannelStats = ChannelStats(),
    @SerializedName("contentDetails") val contentDetails: ChannelContent? = null
)

data class ChannelSnippet(
    val title: String = "",
    val description: String = "",
    @SerializedName("customUrl") val customUrl: String = "",
    @SerializedName("publishedAt") val publishedAt: String = "",
    val country: String? = null,
    val thumbnails: ThumbHolder = ThumbHolder()
)

data class ChannelStats(
    @SerializedName("viewCount") val viewCount: String = "0",
    @SerializedName("subscriberCount") val subscriberCount: String = "0",
    @SerializedName("hiddenSubscriberCount") val hidden: Boolean = false,
    @SerializedName("videoCount") val videoCount: String = "0"
)

data class ChannelContent(
    @SerializedName("relatedPlaylists") val relatedPlaylists: RelatedPlaylists = RelatedPlaylists()
)

data class RelatedPlaylists(val uploads: String = "")

data class ThumbHolder(val medium: Thumb = Thumb())
data class Thumb(val url: String = "")

data class PlaylistItemsResponse(val items: List<PlaylistItem> = emptyList())
data class PlaylistItem(
    @SerializedName("contentDetails") val contentDetails: PContent = PContent()
)
data class PContent(@SerializedName("videoId") val videoId: String = "")

data class VideosResponse(val items: List<VideoItem> = emptyList())
data class VideoItem(
    val id: String = "",
    val snippet: VideoSnippet = VideoSnippet(),
    val statistics: VideoStats = VideoStats()
)
data class VideoSnippet(
    val title: String = "",
    @SerializedName("publishedAt") val publishedAt: String = ""
)
data class VideoStats(
    @SerializedName("viewCount") val viewCount: String = "0",
    @SerializedName("likeCount") val likeCount: String = "0",
    @SerializedName("commentCount") val commentCount: String = "0"
)

/* --------------------------------- Service --------------------------------- */

interface YouTubeService {

    @GET("youtube/v3/channels")
    suspend fun channels(
        @Query("part") parts: String,
        @Query("forHandle") forHandle: String?,
        @Query("id") id: String?,
        @Query("key") key: String
    ): ChannelResponse

    @GET("youtube/v3/playlistItems")
    suspend fun playlistItems(
        @Query("part") part: String,
        @Query("playlistId") playlistId: String,
        @Query("maxResults") max: Int,
        @Query("key") key: String
    ): PlaylistItemsResponse

    @GET("youtube/v3/videos")
    suspend fun videos(
        @Query("part") part: String,
        @Query("id") ids: String,
        @Query("key") key: String
    ): VideosResponse
}

/* ------------------------------ مدل تحلیلی ------------------------------ */

/** خلاصه تحلیل یک کانال برای نمایش در UI */
data class ChannelAnalysis(
    val id: String,
    val title: String,
    val handle: String,
    val subscribers: Long,
    val totalViews: Long,
    val videoCount: Long,
    val publishedAt: String,
    val channelAgeDays: Long,
    val avgViews: Long,
    val videosPerMonth: Double,
    val engagementRate: Double,     // (لایک + کامنت) / بازدید
    val viewSubRatio: Double,       // بازدید میانگین / سابسکرایبر
    val topVideo: VideoItem?,
    val recentVideos: List<VideoItem>,
    val insights: List<String>      // بینش‌های خودکار
)
