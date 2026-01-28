package com.example.myautoplayer

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

// --- DATA MODELS ---
data class PipedSearchResponse(
    val items: List<PipedVideo>? // Made nullable to prevent crashes
)

data class PipedVideo(
    val title: String, 
    val url: String, 
    val thumbnail: String, 
    val uploaderName: String
)

data class PipedStreamResponse(val audioStreams: List<PipedAudioStream>)
data class PipedAudioStream(val url: String, val format: String)

// --- API INTERFACE ---
interface PipedService {
    @GET("search")
    suspend fun searchVideos(
        @Query("q") query: String, 
        @Query("filter") filter: String = "videos"
    ): PipedSearchResponse

    @GET("streams/{videoId}")
    suspend fun getVideoStreams(@Path("videoId") videoId: String): PipedStreamResponse
}

// --- REPOSITORY ---
object YouTubeRepository {
    // PRIMARY SERVER (Faster)
    private const val BASE_URL_1 = "https://pipedapi.kavin.rocks/" 
    // BACKUP SERVER (If primary fails)
    private const val BASE_URL_2 = "https://api.piped.otse.one/" 

    private fun createService(url: String): PipedService {
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PipedService::class.java)
    }

    private val service1 = createService(BASE_URL_1)
    private val service2 = createService(BASE_URL_2)

    suspend fun search(query: String): List<VideoItem> {
        return try {
            // Try Server 1
            Log.d("YouTubeApi", "Trying Server 1...")
            val response = service1.searchVideos(query)
            processResponse(response)
        } catch (e: Exception) {
            Log.e("YouTubeApi", "Server 1 Failed: ${e.message}")
            try {
                // Try Server 2
                Log.d("YouTubeApi", "Trying Server 2...")
                val response2 = service2.searchVideos(query)
                processResponse(response2)
            } catch (e2: Exception) {
                Log.e("YouTubeApi", "All Servers Failed: ${e2.message}")
                throw Exception("Connection Failed: ${e2.message}") // Throw to UI
            }
        }
    }

    private fun processResponse(response: PipedSearchResponse): List<VideoItem> {
        return response.items?.map { 
            VideoItem(
                title = it.title,
                channelName = it.uploaderName,
                thumbnailUrl = it.thumbnail,
                audioStreamUrl = it.url 
            )
        } ?: emptyList()
    }

    suspend fun getAudioUrl(videoUrl: String): String? {
        val id = if (videoUrl.contains("v=")) videoUrl.substringAfter("v=") else videoUrl
        return try {
            val streams = service1.getVideoStreams(id)
            streams.audioStreams.find { it.format == "m4a" }?.url 
                ?: streams.audioStreams.firstOrNull()?.url
        } catch (e: Exception) {
            try {
                val streams = service2.getVideoStreams(id)
                streams.audioStreams.find { it.format == "m4a" }?.url 
                    ?: streams.audioStreams.firstOrNull()?.url
            } catch (e2: Exception) {
                null
            }
        }
    }
}
