package com.example.myautoplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

        // Create the layout programmatically (so you don't need XML)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val searchBox = EditText(this).apply {
            hint = "Search YouTube..."
        }

        val searchButton = Button(this).apply {
            text = "Search"
        }

        val recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@PlayerActivity)
        }

        layout.addView(searchBox)
        layout.addView(searchButton)
        layout.addView(recyclerView)
        setContentView(layout)

        // SEARCH BUTTON CLICK LOGIC
        searchButton.setOnClickListener {
            val query = searchBox.text.toString()
            if (query.isBlank()) return@setOnClickListener

            Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show()

            // Run the search on a background thread
            lifecycleScope.launch {
                try {
                    val videos = YouTubeRepository.search(query)

                    if (videos.isEmpty()) {
                        Toast.makeText(this@PlayerActivity, "No videos found.", Toast.LENGTH_LONG).show()
                    } else {
                        // Update the list with results
                        recyclerView.adapter = VideoAdapter(videos) { video ->
                            playVideo(video)
                        }
                    }
                } catch (e: Exception) {
                    // Show error if internet fails
                    Toast.makeText(this@PlayerActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun playVideo(video: VideoItem) {
        Toast.makeText(this, "Fetching Audio...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val streamUrl = YouTubeRepository.getAudioUrl(video.audioStreamUrl)

            if (streamUrl != null) {
                Toast.makeText(this@PlayerActivity, "Playing: ${video.title}", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@PlayerActivity, AudioService::class.java)
                startService(intent)
            } else {
                Toast.makeText(this@PlayerActivity, "Could not find audio stream.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
