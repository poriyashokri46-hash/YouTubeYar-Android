package com.youtubeyar.app.youtube

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit client for public YouTube Data API v3 endpoints.
 * Responses use defaults so missing/hidden public fields do not crash JSON parsing.
 */
interface YouTubeService {
    @GET("youtube/v3/channels")
    suspend fun channels(
        @Query("part") parts: String,
        @Query("forHandle") forHandle: String?,
        @Query("id") id: String?,
        @Query("key") key: String
    ): ChannelListResponse

    @GET("youtube/v3/playlistItems")
    suspend fun playlistItems(
        @Query("part") parts: String,
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int,
        @Query("key") key: String
    ): PlaylistItemsResponse

    @GET("youtube/v3/videos")
    suspend fun videos(
        @Query("part") parts: String,
        @Query("id") ids: String,
        @Query("key") key: String
    ): VideoListResponse
}

data class ChannelListResponse(
    val items: List<YouTubeChannel> = emptyList()
)

data class YouTubeChannel(
    val id: String = "",
    val snippet: ChannelSnippet = ChannelSnippet(),
    val statistics: ChannelStatistics = ChannelStatistics(),
    val contentDetails: ChannelContentDetails? = null
)

data class ChannelSnippet(
    val title: String = "",
    val customUrl: String = "",
    val publishedAt: String = ""
)

data class ChannelStatistics(
    val subscriberCount: String = "",
    val viewCount: String = "",
    val videoCount: String = ""
)

data class ChannelContentDetails(
    val relatedPlaylists: RelatedPlaylists? = null
)

data class RelatedPlaylists(
    val uploads: String = ""
)

data class PlaylistItemsResponse(
    val items: List<PlaylistItem> = emptyList()
)

data class PlaylistItem(
    val contentDetails: PlaylistItemContentDetails = PlaylistItemContentDetails()
)

data class PlaylistItemContentDetails(
    val videoId: String = ""
)

data class VideoListResponse(
    val items: List<VideoItem> = emptyList()
)

data class VideoItem(
    val id: String = "",
    val snippet: VideoSnippet = VideoSnippet(),
    val statistics: VideoStatistics = VideoStatistics()
)

data class VideoSnippet(
    val title: String = "",
    val publishedAt: String = "",
    val channelTitle: String = ""
)

data class VideoStatistics(
    val viewCount: String = "",
    val likeCount: String = "",
    val commentCount: String = ""
)

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
    val engagementRate: Double,
    val viewSubRatio: Double,
    val topVideo: VideoItem?,
    val recentVideos: List<VideoItem>,
    val insights: List<String>
)
