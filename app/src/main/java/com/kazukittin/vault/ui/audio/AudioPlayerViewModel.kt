package com.kazukittin.vault.ui.audio

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.kazukittin.vault.data.local.db.TrackEntity
import com.kazukittin.vault.data.local.db.WorkEntity
import com.kazukittin.vault.data.remote.SynoFolder
import com.kazukittin.vault.data.repository.AudioRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioPlayerViewModel(
    private val audioRepository: AudioRepository
) : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    // ── 再生状態 ──
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

    // ── トラック情報（ミニプレーヤー用） ──
    private val _currentTrackTitle = MutableStateFlow("")
    val currentTrackTitle = _currentTrackTitle.asStateFlow()

    private val _hasActiveSession = MutableStateFlow(false)
    val hasActiveSession = _hasActiveSession.asStateFlow()

    /** 現在再生中のフォルダパス（重複再生防止用） */
    private var activeSessionFolderPath: String? = null

    private var pendingPlayRequest: PendingPlayRequest? = null

    data class PendingPlayRequest(
        val work: WorkEntity,
        val tracks: List<TrackEntity>,
        val startIndex: Int
    )

    // ── MediaController 接続 ──

    fun initController(context: Context) {
        if (controller != null) return

        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val c = controllerFuture?.get() ?: return@addListener
                controller = c
                Log.d("AudioPlayer", "MediaController connected")

                c.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _currentTrackIndex.value = c.currentMediaItemIndex
                        _duration.value = c.duration.coerceAtLeast(0L)
                        _playbackError.value = null
                        _currentTrackTitle.value = mediaItem?.mediaMetadata?.title?.toString() ?: ""
                        _hasActiveSession.value = c.mediaItemCount > 0
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("AudioPlayer", "Player Error: ${error.message}", error)
                        _playbackError.value = "再生エラー: ${error.message}"
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _hasActiveSession.value = c.mediaItemCount > 0
                    }
                })

                _isPlaying.value = c.isPlaying
                _hasActiveSession.value = c.mediaItemCount > 0
                if (c.currentMediaItem != null) {
                    _currentTrackTitle.value = c.currentMediaItem?.mediaMetadata?.title?.toString() ?: ""
                }

                pendingPlayRequest?.let {
                    playTracksInternal(it.work, it.tracks, it.startIndex)
                    pendingPlayRequest = null
                }

                syncPosition()
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Failed to connect MediaController", e)
            }
        }, context.mainExecutor)
    }

    private fun syncPosition() {
        viewModelScope.launch {
            while (true) {
                _position.value = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
                _duration.value = controller?.duration?.let { if (it > 0) it else 0L } ?: 0L
                delay(1000)
            }
        }
    }

    // ── 再生開始（Audio Library 経由） ──

    fun playTracks(work: WorkEntity, tracks: List<TrackEntity>, startIndex: Int) {
        if (controller == null) {
            pendingPlayRequest = PendingPlayRequest(work, tracks, startIndex)
        } else {
            playTracksInternal(work, tracks, startIndex)
        }
    }

    private fun playTracksInternal(work: WorkEntity, tracks: List<TrackEntity>, startIndex: Int) {
        _currentWork.value = work
        activeSessionFolderPath = work.folderPath

        val mediaItems = tracks.map { track ->
            val url = audioRepository.getDownloadUrl(track.path) ?: ""
            MediaItem.Builder()
                .setMediaId(track.path)
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(work.circle)
                        .setAlbumTitle(work.title)
                        .build()
                )
                .build()
        }

        controller?.setMediaItems(mediaItems, startIndex, 0L)
        controller?.prepare()
        controller?.play()
        _hasActiveSession.value = true
    }

    // ── 再生開始（フォルダ閲覧 → 音声ファイルタップ 経由） ──

    /**
     * フォルダ内の音声ファイルから直接再生する。
     * SynoFolder → 合成 WorkEntity/TrackEntity に変換してサービスに渡す。
     */
    fun playFolderAudio(
        folderPath: String,
        audioFiles: List<SynoFolder>,
        startIndex: Int,
        coverUrl: String?,
        folderName: String,
        urlBuilder: (String) -> String?
    ) {
        // 同じフォルダの同じトラックなら何もしない
        if (folderPath == activeSessionFolderPath
            && controller?.mediaItemCount == audioFiles.size
        ) {
            // インデックスだけ変更
            if (_currentTrackIndex.value != startIndex) {
                controller?.seekTo(startIndex, 0L)
            }
            return
        }

        val syntheticWork = WorkEntity(
            folderPath = folderPath,
            rjCode = Regex("RJ(\\d+)", RegexOption.IGNORE_CASE).find(folderName)?.value?.uppercase(),
            title = folderName,
            circle = "",
            coverUrl = coverUrl,
            cv = "",
            tags = ""
        )
        val syntheticTracks = audioFiles.mapIndexed { i, file ->
            TrackEntity(
                path = file.path,
                workFolderPath = folderPath,
                title = file.name,
                order = i,
                durationMs = 0
            )
        }

        // URL は FolderRepository 経由のものを使う
        _currentWork.value = syntheticWork
        activeSessionFolderPath = folderPath

        val mediaItems = syntheticTracks.map { track ->
            val url = urlBuilder(track.path) ?: ""
            MediaItem.Builder()
                .setMediaId(track.path)
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(folderName)
                        .setAlbumTitle(folderName)
                        .build()
                )
                .build()
        }

        if (controller == null) {
            pendingPlayRequest = PendingPlayRequest(syntheticWork, syntheticTracks, startIndex)
        } else {
            controller?.setMediaItems(mediaItems, startIndex, 0L)
            controller?.prepare()
            controller?.play()
            _hasActiveSession.value = true
        }
    }

    /** 特定のトラックインデックスに移動（同セッション内） */
    fun seekToTrack(index: Int) {
        controller?.seekTo(index, 0L)
    }

    // ── コントロール ──

    fun togglePlay() {
        if (controller?.isPlaying == true) controller?.pause() else controller?.play()
    }

    fun seekTo(pos: Long) {
        controller?.seekTo(pos)
    }

    fun seekBackward10() {
        val c = controller ?: return
        c.seekTo(max(0L, c.currentPosition - 10_000L))
    }

    fun seekForward10() {
        val c = controller ?: return
        c.seekTo(min(c.duration.coerceAtLeast(0L), c.currentPosition + 10_000L))
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    /** ミニプレーヤーの×ボタン */
    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        _currentWork.value = null
        _currentTrackTitle.value = ""
        _hasActiveSession.value = false
        _isPlaying.value = false
        _position.value = 0L
        _duration.value = 0L
        activeSessionFolderPath = null
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }

    private fun max(a: Long, b: Long) = if (a > b) a else b
    private fun min(a: Long, b: Long) = if (a < b) a else b
}
