package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.data.model.Track
import com.example.data.repository.LeafRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioPlayerManager private constructor(private val context: Context) {
    private val repository = LeafRepository.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var pingCounter = 0

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioPlayerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
        } else if (!_queue.value.any { it.id == track.id }) {
            _queue.value = _queue.value + track
        }

        _currentTrack.value = track
        _durationMs.value = track.duration ?: 0L
        _currentPositionMs.value = 0L
        _isLoading.value = true
        _errorMessage.value = null

        releasePlayer()

        scope.launch {
            val streamResult = repository.resolveStreamUrl(track.id)
            if (streamResult.isFailure) {
                _isLoading.value = false
                _errorMessage.value = streamResult.exceptionOrNull()?.message ?: "Ошибка получения потока"
                return@launch
            }

            val streamUrl = streamResult.getOrNull()
            if (streamUrl.isNullOrBlank()) {
                _isLoading.value = false
                _errorMessage.value = "Некорректная ссылка на аудио"
                return@launch
            }

            try {
                withContext(Dispatchers.IO) {
                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(streamUrl)
                        setOnPreparedListener { mp ->
                            _isLoading.value = false
                            _isPlaying.value = true
                            if (mp.duration > 0) {
                                _durationMs.value = mp.duration.toLong()
                            }
                            mp.start()
                            startProgressTicker()
                        }
                        setOnCompletionListener {
                            _isPlaying.value = false
                            _currentPositionMs.value = _durationMs.value
                            playNext()
                        }
                        setOnErrorListener { _, what, extra ->
                            _isLoading.value = false
                            _isPlaying.value = false
                            _errorMessage.value = "Ошибка воспроизведения ($what, $extra)"
                            true
                        }
                        prepareAsync()
                    }
                    mediaPlayer = player
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Ошибка плеера: ${e.message}"
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressTicker()
            } else {
                player.start()
                _isPlaying.value = true
                startProgressTicker()
            }
        } catch (_: Exception) {}
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        try {
            player.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        } catch (_: Exception) {}
    }

    fun playNext() {
        val current = _currentTrack.value ?: return
        val currentQueue = _queue.value
        val currentIndex = currentQueue.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex + 1 < currentQueue.size) {
            playTrack(currentQueue[currentIndex + 1])
        } else if (currentQueue.isNotEmpty()) {
            // Loop back to start
            playTrack(currentQueue[0])
        }
    }

    fun playPrevious() {
        // If passed 3 seconds into song, restart it, otherwise go previous
        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }
        val current = _currentTrack.value ?: return
        val currentQueue = _queue.value
        val currentIndex = currentQueue.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playTrack(currentQueue[currentIndex - 1])
        } else if (currentQueue.isNotEmpty()) {
            seekTo(0)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition.toLong()
                            if (player.duration > 0) {
                                _durationMs.value = player.duration.toLong()
                            }
                            pingCounter++
                            if (pingCounter >= 30) { // ~15 seconds of playback
                                pingCounter = 0
                                repository.pingStreak()
                            }
                        }
                    } catch (_: Exception) {}
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun releasePlayer() {
        stopProgressTicker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
    }

    fun stop() {
        releasePlayer()
        _currentTrack.value = null
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
    }
}
