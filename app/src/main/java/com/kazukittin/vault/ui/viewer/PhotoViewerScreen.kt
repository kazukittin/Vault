package com.kazukittin.vault.ui.viewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.kazukittin.vault.ui.folder.FolderContentViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    viewModel: FolderContentViewModel,
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
            key = { page -> imageItems[page].path }
        ) { page ->
            val item = imageItems[page]
            val imageUrl = viewModel.getOriginalImageUrl(item.path)

            val isVideo = item.name.lowercase().let { it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".avi") }

            if (isVideo && imageUrl != null) {
                // 動画の場合はExoPlayerを表示（ズーム・パンは無効化）
                VideoPlayer(
                    videoUrl = imageUrl,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 画像の場合はズーム・パン可能な表示
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

                                var isTwoFinger      = false
                                var prevDist         = -1f
                                var prevCenter       = Offset.Zero

                                do {
                                    val event   = awaitPointerEvent()
                                    val pressed = event.changes.filter { it.pressed }

                                    when {
                                        pressed.size >= 2 -> {
                                            isTwoFinger = true
                                            pressed.forEach { it.consume() }

                                            val a = pressed[0].position
                                            val b = pressed[1].position
                                            val center = (a + b) / 2f
                                            val dist = sqrt(
                                                (a.x - b.x) * (a.x - b.x) +
                                                (a.y - b.y) * (a.y - b.y)
                                            )

                                            if (prevDist > 0) {
                                                val zoomDelta = dist / prevDist
                                                val newScale  = max(1f, min(scaleState.floatValue * zoomDelta, 5f))
                                                scaleState.floatValue = newScale

                                                if (newScale > 1f) {
                                                    val panDelta = center - prevCenter
                                                    val maxX = (size.width  * (newScale - 1f)) / 2f
                                                    val maxY = (size.height * (newScale - 1f)) / 2f
                                                    val cur  = offsetState.value
                                                    offsetState.value = Offset(
                                                        (cur.x + panDelta.x).coerceIn(-maxX, maxX),
                                                        (cur.y + panDelta.y).coerceIn(-maxY, maxY)
                                                    )
                                                } else {
                                                    offsetState.value = Offset.Zero
                                                }
                                            }
                                            prevDist   = dist
                                            prevCenter = center
                                        }

                                        pressed.size == 1 && !isTwoFinger && scaleState.floatValue > 1f -> {
                                            val change = pressed[0]
                                            val delta  = change.positionChange()
                                            change.consume()

                                            val s    = scaleState.floatValue
                                            val maxX = (size.width  * (s - 1f)) / 2f
                                            val maxY = (size.height * (s - 1f)) / 2f
                                            val cur  = offsetState.value
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

@Composable
fun VideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // ExoPlayerのインスタンス作成と設定
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true // 自動再生
        }
    }

    // 画面から離れる時にリソースを解放
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // AndroidViewを使ってExoPlayerを表示
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true // シークバーや再生ボタンを表示
            }
        },
        modifier = modifier.background(Color.Black)
    )
}
