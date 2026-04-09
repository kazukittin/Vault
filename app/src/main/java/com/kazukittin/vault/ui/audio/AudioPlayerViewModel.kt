package com.kazukittin.vault.ui.audio

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.kazukittin.vault.data.local.db.TrackEntity
import com.kazukittin.vault.data.local.db.WorkEntity
import com.kazukittin.vault.data.repository.AudioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AudioPlayerViewModel(
    private val audioRepository: AudioRepository
) : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex = _currentTrackIndex.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _currentWork = MutableStateFlow<WorkEntity?>(null)
    val currentWork = _currentWork.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError = _playbackError.asStateFlow()

    private var pendingPlayRequest: PendingPlayRequest? = null

    data class PendingPlayRequest(
        val work: WorkEntity,
        val tracks: List<TrackEntity>,
        val startIndex: Int
    )

    fun initController(context: Context) {
        android.util.Log.e("VaultDebug", "initController called")
        if (controller != null) {
            android.util.Log.e("VaultDebug", "Controller already exists")
            return
        }

        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val c = controllerFuture?.get() ?: return@addListener
                controller = c
                android.util.Log.e("VaultDebug", "MediaController connected SUCCESS")
                
                c.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        android.util.Log.e("VaultDebug", "onIsPlayingChanged: $isPlaying")
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _currentTrackIndex.value = c.currentMediaItemIndex
                        _duration.value = c.duration
                        _playbackError.value = null
                        android.util.Log.e("VaultDebug", "onMediaItemTransition index=${c.currentMediaItemIndex}")
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.e("VaultDebug", "PLAYER ERROR: ${error.message}", error)
                        _playbackError.value = "Playback Error: ${error.message}"
                    }
                })
                _isPlaying.value = c.isPlaying
                
                pendingPlayRequest?.let {
                    android.util.Log.e("VaultDebug", "Processing pendings for ${it.work.title}")
                    playTracksInternal(it.work, it.tracks, it.startIndex)
                    pendingPlayRequest = null
                }

                syncPosition()
            } catch (e: Exception) {
                android.util.Log.e("VaultDebug", "Failed to connect MediaController", e)
            }
        }, context.mainExecutor)
    }

    private fun syncPosition() {
        viewModelScope.launch {
            while (true) {
                _position.value = controller?.currentPosition ?: 0L
                _duration.value = controller?.duration ?: 0L
                delay(1000)
            }
        }
    }

    fun playTracks(work: WorkEntity, tracks: List<TrackEntity>, startIndex: Int) {
        android.util.Log.e("VaultDebug", "playTracks called: ${work.title}")
        if (controller == null) {
            android.util.Log.e("VaultDebug", "Controller NULL, queuing...")
            pendingPlayRequest = PendingPlayRequest(work, tracks, startIndex)
        } else {
            playTracksInternal(work, tracks, startIndex)
        }
    }

    private fun playTracksInternal(work: WorkEntity, tracks: List<TrackEntity>, startIndex: Int) {
        android.util.Log.e("VaultDebug", "playTracksInternal executing")
        _currentWork.value = work
        val mediaItems = tracks.map { track ->
            val url = audioRepository.getDownloadUrl(track.path) ?: ""
            android.util.Log.e("VaultDebug", "Track URL: $url")
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(work.circle)
                .setAlbumTitle(work.title)
                .setArtworkUri(android.net.Uri.parse(work.coverUrl ?: ""))
                .build().let { metadata ->
                    MediaItem.Builder()
                        .setMediaId(track.path)
                        .setUri(url)
                        .setMediaMetadata(metadata)
                        .build()
                }
        }

        controller?.setMediaItems(mediaItems, startIndex, 0L)
        controller?.prepare()
        controller?.play()
        android.util.Log.e("VaultDebug", "!!! controller.play() WAS CALLED !!!")
    }

    fun togglePlay() {
        if (controller?.isPlaying == true) {
            controller?.pause()
        } else {
            controller?.play()
        }
    }

    fun seekTo(pos: Long) {
        controller?.seekTo(pos)
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
