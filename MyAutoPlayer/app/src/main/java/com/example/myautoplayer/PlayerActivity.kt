package com.example.myautoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ... inside onCreate ...

        searchButton.setOnClickListener {
            val query = searchBox.text.toString()
            if (query.isBlank()) return@setOnClickListener

            Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show()
            
            lifecycleScope.launch {
                try {
                    val videos = YouTubeRepository.search(query)
                    
                    if (videos.isEmpty()) {
                        Toast.makeText(this@PlayerActivity, "No videos found.", Toast.LENGTH_LONG).show()
                    } else {
                        recyclerView.adapter = VideoAdapter(videos) { video ->
                            playVideo(video)
                        }
                    }
                } catch (e: Exception) {
                    // THIS WILL SHOW THE ERROR ON SCREEN
                    Toast.makeText(this@PlayerActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        // Create a simple UI: [Search Box] [Button] [List]
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val searchBox = EditText(this).apply { hint = "Search YouTube..." }
        val searchButton = Button(this).apply { text = "Search" }
        val recyclerView = RecyclerView(this).apply { 
            layoutManager = LinearLayoutManager(this@PlayerActivity) 
        }

        layout.addView(searchBox)
        layout.addView(searchButton)
        layout.addView(recyclerView)
        setContentView(layout)

        // Button Click Logic
        searchButton.setOnClickListener {
            val query = searchBox.text.toString()
            Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show()
            
            // Run internet search in background
            lifecycleScope.launch {
                val videos = YouTubeRepository.search(query)
                
                // Update the list
                recyclerView.adapter = VideoAdapter(videos) { video ->
                    // ON CLICK: Play the video
                   playVideo(video)
                }
            }
        }
    }
    
    private fun playVideo(video: VideoItem) {
        Toast.makeText(this, "Fetching Audio...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            // 1. Get the direct .m4a link
            val streamUrl = YouTubeRepository.getAudioUrl(video.audioStreamUrl)
            
            if (streamUrl != null) {
                // 2. Open standard player (or pass to service in Phase 3)
                // For now, let's just prove we have the link by showing it
                Toast.makeText(this@PlayerActivity, "Audio Link Found! Playing...", Toast.LENGTH_LONG).show()
                
                // This intent starts the audio service directly
                val intent = Intent(this@PlayerActivity, AudioService::class.java)
                startService(intent)
            } else {
                Toast.makeText(this@PlayerActivity, "Error: Could not extract audio", Toast.LENGTH_LONG).show()
            }
        }
    }
}

