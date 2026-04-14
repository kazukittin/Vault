package com.kazukittin.vault.ui.viewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.kazukittin.vault.ui.audio.AudioPlayerViewModel
import com.kazukittin.vault.ui.folder.FolderContentViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    viewModel: FolderContentViewModel,
    audioPlayerViewModel: AudioPlayerViewModel,
    initialIndex: Int,
    onBack: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val imageItems = remember(items) { items.filter { !it.isDir } }

    val vaultSurface = Color(0xFF000000)
    val vaultPrimary = Color(0xFFA1CCED)

    if (imageItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(vaultSurface),
            contentAlignment = Alignment.Center
        ) {
            Text("画像がありません", color = vaultPrimary)
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageItems.size }
    )
    val scope = rememberCoroutineScope()

    // フォルダ名からワーク情報を取得
    val folderName = remember { viewModel.folderPath.substringAfterLast("/") }
    val folderCoverUrl = remember(folderName) { viewModel.getDlSiteThumbnailUrl(folderName) }

    // 音声ファイルのみのリストとインデックスマッピング
    val audioFiles = remember(imageItems) {
        imageItems.filter { item ->
            val n = item.name.lowercase()
            n.endsWith(".mp3") || n.endsWith(".flac") || n.endsWith(".wav") || n.endsWith(".m4a")
        }
    }
    val pageToAudioIndex = remember(imageItems, audioFiles) {
        val map = mutableMapOf<Int, Int>()
        imageItems.forEachIndexed { pageIdx, item ->
            val audioIdx = audioFiles.indexOf(item)
            if (audioIdx >= 0) map[pageIdx] = audioIdx
        }
        map
    }

    // 現在のページが音声ファイルの場合、サービス経由で再生を開始
    val currentPage = pagerState.currentPage
    val currentItem = imageItems.getOrNull(currentPage)
    val isCurrentAudio = currentItem?.let {
        val n = it.name.lowercase()
        n.endsWith(".mp3") || n.endsWith(".flac") || n.endsWith(".wav") || n.endsWith(".m4a")
    } ?: false

    LaunchedEffect(currentPage, isCurrentAudio) {
        if (isCurrentAudio && audioFiles.isNotEmpty()) {
            val audioIdx = pageToAudioIndex[currentPage] ?: 0
            audioPlayerViewModel.playFolderAudio(
                folderPath = viewModel.folderPath,
                audioFiles = audioFiles,
                startIndex = audioIdx,
                coverUrl = folderCoverUrl,
                folderName = folderName,
                urlBuilder = { path -> viewModel.getOriginalImageUrl(path) }
            )
        }
    }

    // サービス側のトラック遷移に追従してページャーも移動
    val trackIndex by audioPlayerViewModel.currentTrackIndex.collectAsState()
    LaunchedEffect(trackIndex) {
        if (isCurrentAudio && audioFiles.isNotEmpty()) {
            val targetItem = audioFiles.getOrNull(trackIndex) ?: return@LaunchedEffect
            val targetPage = imageItems.indexOf(targetItem)
            if (targetPage >= 0 && targetPage != pagerState.currentPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    // ズーム中はPagerのスクロールを止める
    var isPagingEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(vaultSurface)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = isPagingEnabled,
            modifier = Modifier.fillMaxSize(),
            key = { page -> imageItems[page].path },
            beyondViewportPageCount = 0
        ) { page ->
            val item = imageItems[page]
            val imageUrl = viewModel.getOriginalImageUrl(item.path)

            val name = item.name.lowercase()
            val isVideo = name.let { it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".avi") || it.endsWith(".mkv") || it.endsWith(".webm") }
            val isAudio = name.let { it.endsWith(".mp3") || it.endsWith(".flac") || it.endsWith(".wav") || it.endsWith(".m4a") }

            if (isAudio) {
                // ──── サービス経由の音声プレーヤー UI ────
                ServiceAudioPlayer(
                    audioPlayerViewModel = audioPlayerViewModel,
                    coverUrl = folderCoverUrl,
                    trackName = item.name,
                    workTitle = folderName,
                    modifier = Modifier.fillMaxSize(),
                    onPrevious = {
                        scope.launch {
                            if (pagerState.currentPage > 0)
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    onNext = {
                        scope.launch {
                            if (pagerState.currentPage < imageItems.size - 1)
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                )
            } else if (isVideo && imageUrl != null) {
                val isActivePage = page == pagerState.currentPage
                VideoPlayer(
                    videoUrl = imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    isPlaying = isActivePage
                )
            } else {
                // 画像（ズーム・パン対応）
                val scaleState  = remember { mutableFloatStateOf(1f) }
                val offsetState = remember { mutableStateOf(Offset.Zero) }

                LaunchedEffect(scaleState.floatValue) {
                    if (page == pagerState.currentPage) {
                        isPagingEnabled = scaleState.floatValue <= 1f
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var isTwoFinger = false
                                var prevDist = -1f
                                var prevCenter = Offset.Zero

                                do {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.filter { it.pressed }

                                    when {
                                        pressed.size >= 2 -> {
                                            isTwoFinger = true
                                            pressed.forEach { it.consume() }
                                            val a = pressed[0].position
                                            val b = pressed[1].position
                                            val center = (a + b) / 2f
                                            val dist = sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))
                                            if (prevDist > 0) {
                                                val zoomDelta = dist / prevDist
                                                val newScale = max(1f, min(scaleState.floatValue * zoomDelta, 5f))
                                                scaleState.floatValue = newScale
                                                if (newScale > 1f) {
                                                    val panDelta = center - prevCenter
                                                    val maxX = (size.width * (newScale - 1f)) / 2f
                                                    val maxY = (size.height * (newScale - 1f)) / 2f
                                                    val cur = offsetState.value
                                                    offsetState.value = Offset(
                                                        (cur.x + panDelta.x).coerceIn(-maxX, maxX),
                                                        (cur.y + panDelta.y).coerceIn(-maxY, maxY)
                                                    )
                                                } else {
                                                    offsetState.value = Offset.Zero
                                                }
                                            }
                                            prevDist = dist
                                            prevCenter = center
                                        }
                                        pressed.size == 1 && !isTwoFinger && scaleState.floatValue > 1f -> {
                                            val change = pressed[0]
                                            val delta = change.positionChange()
                                            change.consume()
                                            val s = scaleState.floatValue
                                            val maxX = (size.width * (s - 1f)) / 2f
                                            val maxY = (size.height * (s - 1f)) / 2f
                                            val cur = offsetState.value
                                            offsetState.value = Offset(
                                                (cur.x + delta.x).coerceIn(-maxX, maxX),
                                                (cur.y + delta.y).coerceIn(-maxY, maxY)
                                            )
                                        }
                                        else -> { }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (scaleState.floatValue <= 1f) {
                                    offsetState.value = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scaleState.floatValue,
                                    scaleY = scaleState.floatValue,
                                    translationX = offsetState.value.x,
                                    translationY = offsetState.value.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("画像を読み込めませんでした", color = vaultPrimary)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// サービス経由の音声プレーヤー（バックグラウンド再生対応）
// ─────────────────────────────────────────────────────────────

@Composable
fun ServiceAudioPlayer(
    audioPlayerViewModel: AudioPlayerViewModel,
    coverUrl: String?,
    trackName: String,
    workTitle: String,
    modifier: Modifier = Modifier,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {}
) {
    val vaultSurface = Color(0xFF071327)
    val vaultPrimary = Color(0xFFA1CCED)

    val isPlaying by audioPlayerViewModel.isPlaying.collectAsState()
    val position by audioPlayerViewModel.position.collectAsState()
    val duration by audioPlayerViewModel.duration.collectAsState()

    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .background(vaultSurface)
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.06f))

        // ──── カバーアート ────
        Box(
            modifier = Modifier
                .weight(0.52f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF142034)),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ──── トラック名 ────
        Text(
            text = trackName,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))

        // ──── ワークタイトル ────
        Text(
            text = workTitle,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ──── シークバー ────
        val sliderValue = if (isSeeking) seekPosition
        else if (duration > 0) position.toFloat() / duration.toFloat() else 0f

        Slider(
            value = sliderValue,
            onValueChange = { isSeeking = true; seekPosition = it },
            onValueChangeFinished = {
                audioPlayerViewModel.seekTo((seekPosition * duration).toLong())
                isSeeking = false
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = vaultPrimary,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayPos = if (isSeeking) (seekPosition * duration).toLong() else position
            Text(formatTime(displayPos), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text(formatTime(duration), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ──── 再生コントロール ────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipPrevious, "前のトラック", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { audioPlayerViewModel.seekBackward10() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Replay10, "10秒戻る", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Surface(
                onClick = { audioPlayerViewModel.togglePlay() },
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "再生/一時停止",
                        tint = vaultSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            IconButton(onClick = { audioPlayerViewModel.seekForward10() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Forward10, "10秒進む", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipNext, "次のトラック", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.weight(0.08f))
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

// ─────────────────────────────────────────────────────────────
// 動画プレーヤー
// ─────────────────────────────────────────────────────────────

@Composable
fun VideoPlayer(videoUrl: String, modifier: Modifier = Modifier, isPlaying: Boolean) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }
    LaunchedEffect(isPlaying) { exoPlayer.playWhenReady = isPlaying }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier.background(Color.Black)
    )
}
