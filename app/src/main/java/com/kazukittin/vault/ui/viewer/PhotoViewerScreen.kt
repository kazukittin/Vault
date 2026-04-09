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

            // State オブジェクト（安定した参照）でズーム・パン状態を保持
            val scaleState  = remember { mutableFloatStateOf(1f) }
            val offsetState = remember { mutableStateOf(Offset.Zero) }

            // scaleが変わったらPagerの許可フラグを更新
            LaunchedEffect(scaleState.floatValue) {
                if (page == pagerState.currentPage) {
                    isPagingEnabled = scaleState.floatValue <= 1f
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // awaitEachGesture: ジェスチャーごとに独立して処理
                        awaitEachGesture {
                            // 最初の指が置かれるまで待つ
                            awaitFirstDown(requireUnconsumed = false)

                            var isTwoFinger      = false
                            var prevDist         = -1f
                            var prevCenter       = Offset.Zero

                            // すべての指が離れるまでループ
                            do {
                                val event   = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }

                                when {
                                    // ── 2本指: ピンチズーム ＋ パン ──────────────
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

                                    // ── 1本指 + ズーム中: パン ──────────────────
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

                                    // ── 1本指 + 等倍: Pagerに任せる（消費しない）
                                    else -> { /* no-op: HorizontalPager がスワイプを処理 */ }
                                }
                            } while (event.changes.any { it.pressed })

                            // 2本指ジェスチャー後に等倍に戻ったらオフセットリセット
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
