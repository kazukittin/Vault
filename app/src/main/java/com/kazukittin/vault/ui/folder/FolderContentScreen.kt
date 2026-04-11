package com.kazukittin.vault.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContentScreen(
    folderName: String,
    viewModel: FolderContentViewModel,
    onBack: () -> Unit,
    onFolderClick: (String, String) -> Unit,
    onPhotoClick: (Int) -> Unit,
    onZipClick: (path: String, name: String) -> Unit = { _, _ -> }
) {
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasReachedEnd by viewModel.hasReachedEnd.collectAsState()

    val imageItems = items.filter { !it.isDir }

    val vaultSurface   = Color(0xFF071327)
    val vaultContainer = Color(0xFF142034)
    val vaultPrimary   = Color(0xFFA1CCED)

    // グリッドのスクロール状態を監視して末尾に近づいたら次ページを読み込む
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            // 末尾から30件以内になったら次ページをプリフェッチ
            lastVisible >= totalItems - 30 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoadingMore && !hasReachedEnd) {
            viewModel.loadNextPage()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderName, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = vaultSurface)
            )
        },
        containerColor = vaultSurface
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = vaultPrimary)
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 4.dp,
                    start = 4.dp, end = 4.dp, bottom = 4.dp
                ),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = items,
                    key = { it.path } // パスをキーにして再描画を最小限にする
                ) { item ->
                    val isZip = item.name.lowercase().endsWith(".zip")
                    val dlSiteUrl = viewModel.getDlSiteThumbnailUrl(item.name)

                    if (item.isDir) {
                        Card(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onFolderClick(item.path, item.name) },
                            colors = CardDefaults.cardColors(containerColor = vaultContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (dlSiteUrl != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(dlSiteUrl)
                                            .crossfade(true)
                                            .size(300)
                                            .build(),
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // フォルダ名のオーバーレイ（透明な黒背景）
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    ) {
                                        Text(
                                            item.name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = vaultPrimary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        item.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    } else if (isZip) {
                        // ZIP（マンガ）はアーカイブアイコンで表示。RJコードがあれば表紙を表示
                        Card(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onZipClick(item.path, item.name) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C1A)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (dlSiteUrl != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(dlSiteUrl)
                                            .crossfade(true)
                                            .size(300)
                                            .build(),
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        color = Color(0xFF1A2C1A).copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    ) {
                                        Text(
                                            item.name.substringBeforeLast("."),
                                            color = Color(0xFF88CC88),
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    // ZIPバッジ
                                    Surface(
                                        color = Color(0xFF1A2C1A),
                                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text("ZIP", color = Color(0xFF88CC88), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("📦", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        item.name.substringBeforeLast("."),
                                        color = Color(0xFF88CC88),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val isVideo = item.name.lowercase().let {
                            it.endsWith(".mp4") || it.endsWith(".mov") ||
                            it.endsWith(".avi") || it.endsWith(".mkv") || it.endsWith(".webm")
                        }
                        val mediaIndex = imageItems.indexOf(item)

                        if (isVideo) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0D1E35))
                                    .clickable {
                                        if (mediaIndex >= 0) onPhotoClick(mediaIndex)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFA1CCED).copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                                            .padding(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Video",
                                            tint = Color(0xFFA1CCED),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.name,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(viewModel.getThumbnailUrl(item.path))
                                    .crossfade(true)
                                    .size(300, 300) // グリッド用なので低解像度で十分
                                    .build(),
                                contentDescription = item.name,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(vaultContainer)
                                    .clickable {
                                        if (mediaIndex >= 0) onPhotoClick(mediaIndex)
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // 次ページ読み込み中のインジケーター（フル幅で表示）
                if (isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = vaultPrimary,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                // 全件読み込み完了メッセージ
                if (hasReachedEnd && items.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "全 ${items.size} 件を表示中",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
