package com.example.myautoplayer

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

// --- DATA MODELS ---
data class PipedSearchResponse(val items: List<PipedVideo>?)
data class PipedVideo(val title: String, val url: String, val thumbnail: String, val uploaderName: String)
data class PipedStreamResponse(val audioStreams: List<PipedAudioStream>)
data class PipedAudioStream(val url: String, val format: String)

// --- API INTERFACE ---
interface PipedService {
    @GET("search")
    suspend fun searchVideos(@Query("q") query: String, @Query("filter") filter: String = "videos"): PipedSearchResponse

    @GET("streams/{videoId}")
    suspend fun getVideoStreams(@Path("videoId") videoId: String): PipedStreamResponse
}

// --- REPOSITORY ---
object YouTubeRepository {
    // A list of stable servers to try in order
    private val SERVERS = listOf(
        "https://pipedapi.kavin.rocks/",       // Primary
        "https://api.piped.projectsegfau.lt/", // Very Stable Backup
        "https://pipedapi.adminforge.de/"      // Emergency Backup
    )

    private fun createService(url: String): PipedService {
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PipedService::class.java)
    }

    suspend fun search(query: String): List<VideoItem> {
        // Try every server in the list until one works
        for (url in SERVERS) {
            try {
                Log.d("YouTubeApi", "Trying Server: $url")
                val service = createService(url)
                val response = service.searchVideos(query)
                return response.items?.map { 
                    VideoItem(it.title, it.uploaderName, it.thumbnail, it.url) 
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e("YouTubeApi", "Failed $url: ${e.message}")
                // Continue to next server...
            }
        }
        throw Exception("All servers failed. Check Internet.")
    }

    suspend fun getAudioUrl(videoUrl: String): String? {
        val id = if (videoUrl.contains("v=")) videoUrl.substringAfter("v=") else videoUrl
        
        for (url in SERVERS) {
            try {
                val service = createService(url)
                val streams = service.getVideoStreams(id)
                // Return the first valid m4a link we find
                return streams.audioStreams.find { it.format == "m4a" }?.url 
                    ?: streams.audioStreams.firstOrNull()?.url
            } catch (e: Exception) {
                continue // Try next server
            }
        }
        return null
    }
}
