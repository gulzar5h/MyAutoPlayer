package com.example.myautoplayer

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

// --- DATA MODELS ---

// 1. Search Response (Fixed the List error here)
data class PipedSearchResponse(
    val items: List<PipedVideo> 
)

// 2. Single Video Result
data class PipedVideo(
    val title: String, 
    val url: String, 
    val thumbnail: String, 
    val uploaderName: String
)

// 3. Audio Stream Response
data class PipedStreamResponse(
    val audioStreams: List<PipedAudioStream>
)

data class PipedAudioStream(
    val url: String, 
    val format: String
)

// --- API INTERFACE ---
interface PipedService {
    @GET("search")
    suspend fun searchVideos(
        @Query("q") query: String, 
        @Query("filter") filter: String = "videos"
    ): PipedSearchResponse

    @GET("streams/{videoId}")
    suspend fun getVideoStreams(
        @Path("videoId") videoId: String
    ): PipedStreamResponse
}

// --- REPOSITORY ---
object YouTubeRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://pipedapi.kavin.rocks/") 
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(PipedService::class.java)

    private fun extractId(url: String): String {
        return if (url.contains("v=")) url.substringAfter("v=") else url
    }

    suspend fun search(query: String): List<VideoItem> {
        return try {
            val results = service.searchVideos(query)
            // Convert Piped data to our App's data format
            results.items.map { 
                VideoItem(
                    title = it.title,
                    channelName = it.uploaderName,
                    thumbnailUrl = it.thumbnail,
                    audioStreamUrl = it.url 
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAudioUrl(videoUrl: String): String? {
        return try {
            val id = extractId(videoUrl)
            val streams = service.getVideoStreams(id)
            // Prioritize m4a, otherwise take whatever is first
            streams.audioStreams.find { it.format == "m4a" }?.url 
                ?: streams.audioStreams.firstOrNull()?.url
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
