package com.example.myautoplayer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // This sets up the list view without needing a main_activity.xml file
        val recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        setContentView(recyclerView)

        // Fake data to test the UI
        val dummyVideos = listOf(
            VideoItem("Test Video 1", "Channel A", "https://via.placeholder.com/150", "http://example.com/audio1.mp3"),
            VideoItem("Test Video 2", "Channel B", "https://via.placeholder.com/150", "http://example.com/audio2.mp3")
        )

        val adapter = VideoAdapter(dummyVideos) { video ->
            Toast.makeText(this, "Clicked: ${video.title}", Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter = adapter
    }
}