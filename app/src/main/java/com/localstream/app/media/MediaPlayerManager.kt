package com.localstream.app.media

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.localstream.app.domain.model.MediaFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class MediaPlayerManager(
    private val context: Context,
    val queueManager: MediaQueueManager
) {
    private var exoPlayer: ExoPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur,
                        isPlaying = true,
                        isBuffering = false
                    )
                }
            }
            mainHandler.postDelayed(this, 500)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
            val currentPos = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L

            _playerState.value = _playerState.value.copy(
                isBuffering = isBuffering,
                durationMs = duration,
                currentPositionMs = currentPos,
                hasNext = queueManager.hasNext(),
                hasPrevious = queueManager.hasPrevious()
            )

            if (playbackState == Player.STATE_ENDED) {
                playNext()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(
                isPlaying = isPlaying,
                hasNext = queueManager.hasNext(),
                hasPrevious = queueManager.hasPrevious()
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerState.value = _playerState.value.copy(
                isPlaying = false,
                isBuffering = false,
                error = error.localizedMessage ?: "Playback error"
            )
        }
    }

    fun getPlayer(): ExoPlayer {
        return exoPlayer ?: synchronized(this) {
            exoPlayer ?: ExoPlayer.Builder(context).build().apply {
                addListener(playerListener)
                mainHandler.post(progressUpdateRunnable)
                exoPlayer = this
            }
        }
    }

    fun playMedia(mediaFile: MediaFile) {
        val player = getPlayer()
        val mediaUri = when {
            mediaFile.uriString.isNotEmpty() -> Uri.parse(mediaFile.uriString)
            mediaFile.path.isNotEmpty() -> Uri.fromFile(File(mediaFile.path))
            else -> return
        }

        val item = MediaItem.fromUri(mediaUri)
        player.setMediaItem(item)
        player.prepare()
        player.play()

        _playerState.value = _playerState.value.copy(
            currentMedia = mediaFile,
            isPlaying = true,
            isBuffering = true,
            error = null,
            hasNext = queueManager.hasNext(),
            hasPrevious = queueManager.hasPrevious()
        )
    }

    fun playQueueItem(index: Int) {
        val item = queueManager.jumpToIndex(index)
        if (item != null) {
            playMedia(item)
        }
    }

    fun playNext() {
        val next = queueManager.next()
        if (next != null) {
            playMedia(next)
        } else {
            _playerState.value = _playerState.value.copy(isPlaying = false)
        }
    }

    fun playPrevious() {
        val prev = queueManager.previous()
        if (prev != null) {
            playMedia(prev)
        }
    }

    fun togglePlayPause() {
        val player = getPlayer()
        if (player.isPlaying) {
            player.pause()
            _playerState.value = _playerState.value.copy(isPlaying = false)
        } else {
            player.play()
            _playerState.value = _playerState.value.copy(isPlaying = true)
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun seekRelative(offsetMs: Long) {
        val current = exoPlayer?.currentPosition ?: return
        val target = (current + offsetMs).coerceIn(0L, exoPlayer?.duration ?: 0L)
        seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = clamped
        _playerState.value = _playerState.value.copy(volume = clamped)
    }

    fun toggleFullscreen() {
        _playerState.value = _playerState.value.copy(
            isFullscreen = !_playerState.value.isFullscreen
        )
    }

    fun release() {
        mainHandler.removeCallbacks(progressUpdateRunnable)
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
    }
}
