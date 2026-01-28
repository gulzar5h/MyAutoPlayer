package com.example.myautoplayer

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

// 1. Define what the Internet data looks like
data class PipedSearchResponse(val items: List_PipedVideo)
data class PipedVideo(val title: String, val url: String, val thumbnail: String, val uploaderName: String)
data class PipedStreamResponse(val audioStreams: List<PipedAudioStream>)
data class PipedAudioStream(val url: String, val format: String)

// 2. Define the commands we can send
interface PipedService {
    @GET("search")
    suspend fun searchVideos(@Query("q") query: String, @Query("filter") filter: String = "videos"): PipedSearchResponse

    @GET("streams/{videoId}")
    suspend fun getVideoStreams(@Path("videoId") videoId: String): PipedStreamResponse
}

// 3. The Object that actually does the work
object YouTubeRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://pipedapi.kavin.rocks/") // Public YouTube Bridge
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(PipedService::class.java)

    // Helper to get video ID from "/watch?v=XYZ"
    private fun extractId(url: String): String {
        return url.substringAfter("v=")
    }

    suspend fun search(query: String): List<VideoItem> {
        return try {
            val results = service.searchVideos(query)
            results.items.map { 
                VideoItem(
                    title = it.title,
                    channelName = it.uploaderName,
                    thumbnailUrl = it.thumbnail,
                    audioStreamUrl = it.url // We store the Watch URL for now, will fetch audio later
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAudioUrl(videoUrl: String): String? {
        return try {
            val id = extractId(videoUrl)
            val streams = service.getVideoStreams(id)
            // Find the best audio stream (m4a is best for android)
            streams.audioStreams.find { it.format == "m4a" }?.url 
                ?: streams.audioStreams.firstOrNull()?.url
        } catch (e: Exception) {
            null
        }
    }
}
