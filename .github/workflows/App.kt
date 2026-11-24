package com.example.ytdlpapp

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class App : Application() {
    @Volatile var ready = false
    @Volatile var initError: String? = null

    override fun onCreate() {
        super.onCreate()
        Thread {
            try {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
                ready = true
            } catch (e: Exception) {
                initError = e.message
                Log.e("YtDlpApp", "init failed", e)
            }
        }.start()
    }
}
