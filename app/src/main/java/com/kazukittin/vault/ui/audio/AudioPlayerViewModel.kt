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

    fun initController(context: Context) {
        if (controller != null) return

        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _currentTrackIndex.value = controller?.currentMediaItemIndex ?: 0
                    _duration.value = controller?.duration ?: 0L
                }
            })
            _isPlaying.value = controller?.isPlaying ?: false
            syncPosition()
        }, MoreExecutors.directExecutor())
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
        _currentWork.value = work
        val mediaItems = tracks.map { track ->
            val url = audioRepository.getDownloadUrl(track.path) ?: ""
            MediaItem.Builder()
                .setMediaId(track.path)
                .setUri(url)
                .build()
        }

        controller?.setMediaItems(mediaItems, startIndex, 0L)
        controller?.prepare()
        controller?.play()
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
