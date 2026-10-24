package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.data.repository.LeafRepository
import com.example.player.AudioPlayerManager
import com.example.ui.screens.MainScreen
import com.example.ui.theme.LeafMusicTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: LeafRepository
    private lateinit var playerManager: AudioPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = LeafRepository.getInstance(applicationContext)
        playerManager = AudioPlayerManager.getInstance(applicationContext)

        setContent {
            val themeState by repository.themeState.collectAsState()
            val accentColor = remember(themeState.accentHex) {
                try {
                    Color(android.graphics.Color.parseColor(themeState.accentHex))
                } catch (_: Exception) {
                    Color(0xFF22C55E)
                }
            }

            LeafMusicTheme(
                isDark = themeState.isDark,
                accentColor = accentColor,
                isWhiteTheme = themeState.colorTheme == "white"
            ) {
                MainScreen(
                    repository = repository,
                    playerManager = playerManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            playerManager.stop()
        }
    }
}
