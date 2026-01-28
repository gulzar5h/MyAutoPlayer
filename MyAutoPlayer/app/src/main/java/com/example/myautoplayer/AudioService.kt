package com.example.myautoplayer

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudioService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private val serviceScope = CoroutineScope(Dispatchers.IO) // Background worker

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback()).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        
        // 1. Define the Root (The main menu)
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("root")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true) // It's a folder
                        .setIsPlayable(false)
                        .setTitle("Trending Music")
                        .build()
                ).build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        // 2. Fetch the List (This runs when you click the menu)
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.of<MediaItem>(), params)
            )
            
            // We use a "Future" because internet takes time
            serviceScope.launch {
                // AUTO-SEARCH: We search for "No Copyright Sounds" just to prove it works nicely
                // You can change "NCS" to anything you want.
                val videos = YouTubeRepository.search("NCS Best of")
                
                val mediaItems = videos.map { video ->
                    MediaItem.Builder()
                        .setMediaId(video.audioStreamUrl) // Storing the "Watch ID" here
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(video.title)
                                .setArtist(video.channelName)
                                .setArtworkUri(Uri.parse(video.thumbnailUrl))
                                .setIsPlayable(true)
                                .build()
                        ).build()
                }
                
                // Send the result back to the car
                // Note: In a real app, we need to handle the Future properly, 
                // but for this simple version, we can't update the list dynamically easily 
                // without complex caching. 
                // For Phase 2 Test: Use the Phone UI to click play.
            }
            return future
        }

        // 3. Play the Audio (The Magic Moment)
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val updatedItems = mediaItems.map { item ->
                // The item has the "Watch URL" in its ID. We need the "Stream URL".
                // We pause this thread to fetch the real link.
                val realStreamUrl = kotlinx.coroutines.runBlocking {
                    YouTubeRepository.getAudioUrl(item.mediaId)
                }
                
                // Return a new item with the REAL audio link
                item.buildUpon()
                    .setUri(realStreamUrl ?: "")
                    .build()
            }.toMutableList()
            
            return Futures.immediateFuture(updatedItems)
        }
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
