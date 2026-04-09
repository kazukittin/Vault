package com.kazukittin.vault.ui.viewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kazukittin.vault.ui.folder.FolderContentViewModel
import kotlin.math.max
import kotlin.math.min

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

    val currentFileName = imageItems.getOrNull(pagerState.currentPage)?.name ?: ""

    // Pagerのスクロール許可フラグ（ズーム中は禁止）
    var isPagingEnabled by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(vaultSurface)) {

        // ───── ページャー本体（全画面） ─────
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = isPagingEnabled,
            modifier = Modifier.fillMaxSize(),
            key = { page -> imageItems[page].path }
        ) { page ->
            val item = imageItems[page]
            val imageUrl = viewModel.getOriginalImageUrl(item.path)

            // ★ State オブジェクトをキャプチャすることで
            //   pointerInput(Unit) 内でも常に最新値を参照できる
            val scaleState = remember { mutableFloatStateOf(1f) }
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
                        detectTransformGestures { _, pan, zoom, _ ->
                            // State オブジェクト経由で読み書きするので常に最新値
                            val currentScale = scaleState.floatValue
                            val newScale = max(1f, min(currentScale * zoom, 5f))
                            scaleState.floatValue = newScale

                            if (newScale > 1f) {
                                val maxX = (size.width * (newScale - 1f)) / 2f
                                val maxY = (size.height * (newScale - 1f)) / 2f
                                val cur = offsetState.value
                                offsetState.value = Offset(
                                    x = (cur.x + pan.x).coerceIn(-maxX, maxX),
                                    y = (cur.y + pan.y).coerceIn(-maxY, maxY)
                                )
                            } else {
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

        // ───── 小さなオーバーレイヘッダー（Pagerの上に重ねる） ─────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color(0x88000000)) // 半透明の黒
                .height(40.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = currentFileName,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
        }
    }
}
