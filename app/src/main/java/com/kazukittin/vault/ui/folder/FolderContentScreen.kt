package com.kazukittin.vault.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kazukittin.vault.data.remote.VideoThumbnailUrl

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
    val items by viewModel.displayItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasReachedEnd by viewModel.hasReachedEnd.collectAsState()
    val metadataCache by viewModel.metadataCache.collectAsState()
    val folderCategory = viewModel.folderCategory

    val selectedCircle by viewModel.selectedCircle.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val availableFilters by viewModel.availableFilters.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadBookmarks() }

    val imageItems = remember(items) { items.filter { !it.isDir } }

    val vaultSurface   = Color(0xFF071327)
    val vaultContainer = Color(0xFF142034)
    val vaultPrimary   = Color(0xFFA1CCED)

    // メタデータ取得は loadNextPage 完了後に ViewModel 側で自動実行されるため UI 側のトリガー不要
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


    val (circles, tags) = availableFilters
    val activeFilterCount = listOfNotNull(selectedCircle, selectedTag).size
    var showFilterSheet by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        FilterModalSheet(
            circles = circles,
            tags = tags,
            selectedCircle = selectedCircle,
            selectedTag = selectedTag,
            onCircleSelect = { viewModel.selectCircle(it) },
            onTagSelect = { viewModel.selectTag(it) },
            onDismiss = { showFilterSheet = false },
            vaultPrimary = vaultPrimary,
            vaultContainer = vaultContainer
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderName, color = Color.White, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (folderCategory != "画像" && (circles.isNotEmpty() || tags.isNotEmpty())) {
                        BadgedBox(
                            badge = {
                                if (activeFilterCount > 0) {
                                    Badge { Text("$activeFilterCount") }
                                }
                            }
                        ) {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(Icons.Default.Search, contentDescription = "絞り込み", tint = vaultPrimary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = vaultSurface)
            )
        },
        containerColor = vaultSurface
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = vaultPrimary)
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.weight(1f),
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
                                    // フォルダ名のオーバーレイ
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                            val rjMatch = Regex("RJ(\\d+)", RegexOption.IGNORE_CASE).find(item.name)
                                            val rjCode = rjMatch?.value?.uppercase()
                                            val info = if (rjCode != null) metadataCache[rjCode] else null

                                            if (info != null) {
                                                // サークル名（上段）: maker_name → author → illustration の順で最初に見つかったものを使用
                                                val circleName = info.maker_name
                                                    ?: info.creaters?.author?.firstOrNull()?.name
                                                    ?: info.creaters?.illustration?.firstOrNull()?.name
                                                if (circleName != null) {
                                                    Text(
                                                        circleName,
                                                        color = vaultPrimary,
                                                        fontSize = 9.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                // ボイスフォルダなら声優名を表示
                                                if (folderCategory == "ボイス") {
                                                    val voices = info.creaters?.voice_by?.joinToString(", ") { v -> v.name ?: "" }
                                                    if (!voices.isNullOrEmpty()) {
                                                        Text(
                                                            "CV: $voices",
                                                            color = Color.White.copy(alpha = 0.7f),
                                                            fontSize = 8.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                            // 下段：マンガかつDLSiteタイトルあればwork_name、それ以外はフォルダ名
                                            Text(
                                                if ((folderCategory == "マンガ" || folderCategory == "ボイス") && info?.work_name != null)
                                                    info.work_name
                                                else
                                                    item.name,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
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
                                        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                            val rjMatch = Regex("RJ(\\d+)", RegexOption.IGNORE_CASE).find(item.name)
                                            val rjCode = rjMatch?.value?.uppercase()
                                            val info = if (rjCode != null) metadataCache[rjCode] else null

                                            if (info != null) {
                                                // サークル名（上段）: maker_name → author → illustration の順で最初に見つかったものを使用
                                                val circleName = info.maker_name
                                                    ?: info.creaters?.author?.firstOrNull()?.name
                                                    ?: info.creaters?.illustration?.firstOrNull()?.name
                                                if (circleName != null) {
                                                    Text(
                                                        circleName,
                                                        color = Color(0xFF88CC88),
                                                        fontSize = 9.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                // ボイスフォルダなら声優名を表示
                                                if (folderCategory == "ボイス") {
                                                    val voices = info.creaters?.voice_by?.joinToString(", ") { v -> v.name ?: "" }
                                                    if (!voices.isNullOrEmpty()) {
                                                        Text(
                                                            "CV: $voices",
                                                            color = Color(0xFF88CC88).copy(alpha = 0.7f),
                                                            fontSize = 8.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                            // 下段：マンガかつDLSiteタイトルあればwork_name、それ以外はファイル名
                                            Text(
                                                if ((folderCategory == "マンガ" || folderCategory == "ボイス") && info?.work_name != null)
                                                    info.work_name
                                                else
                                                    item.name.substringBeforeLast("."),
                                                color = Color(0xFF88CC88),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    // ZIPバッジ
                                    Surface(
                                        color = Color(0xFF1A2C1A),
                                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text("ZIP", color = Color(0xFF88CC88), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                    // しおりバッジ（左上）
                                    val bookmark = bookmarks[item.path]
                                    if (bookmark != null) {
                                        Surface(
                                            color = Color(0xFFA1CCED).copy(alpha = 0.9f),
                                            shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                "${bookmark.page + 1}/${bookmark.totalPages}p",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
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
                                    // しおりバッジ（左上）
                                    val bookmark = bookmarks[item.path]
                                    if (bookmark != null) {
                                        Surface(
                                            color = Color(0xFFA1CCED).copy(alpha = 0.9f),
                                            shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                "${bookmark.page + 1}/${bookmark.totalPages}p",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val itemName = item.name.lowercase()
                        val isVideo = itemName.let {
                            it.endsWith(".mp4") || it.endsWith(".mov") ||
                            it.endsWith(".avi") || it.endsWith(".mkv") || it.endsWith(".webm")
                        }
                        val isAudio = itemName.let {
                            it.endsWith(".mp3") || it.endsWith(".flac") ||
                            it.endsWith(".wav") || it.endsWith(".m4a")
                        }
                        val mediaIndex = imageItems.indexOf(item)

                        if (isAudio) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A1A2E))
                                    .clickable {
                                        if (mediaIndex >= 0) onPhotoClick(mediaIndex)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = "Audio",
                                        tint = Color(0xFFA1CCED),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.name,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        } else if (isVideo) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(vaultContainer)
                                    .clickable {
                                        if (mediaIndex >= 0) onPhotoClick(mediaIndex)
                                    }
                            ) {
                                // 動画サムネイル（DLSite優先、なければ動画から最初のフレームを抽出）
                                val videoData = if (dlSiteUrl != null) dlSiteUrl
                                    else viewModel.getOriginalImageUrl(item.path)
                                        ?.let { VideoThumbnailUrl(it) }
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(videoData)
                                        .crossfade(true)
                                        .size(300, 300)
                                        .build(),
                                    contentDescription = item.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // 再生アイコンオーバーレイ
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Video",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                // ファイル名オーバーレイ
                                Surface(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.name,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterModalSheet(
    circles: List<String>,
    tags: List<String>,
    selectedCircle: String?,
    selectedTag: String?,
    onCircleSelect: (String?) -> Unit,
    onTagSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    vaultPrimary: Color,
    vaultContainer: Color
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = vaultContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // ヘッダー
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("絞り込み", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onCircleSelect(null); onTagSelect(null) }) {
                    Text("リセット", color = vaultPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (circles.isNotEmpty()) {
                Text("サークル", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    circles.forEach { circle ->
                        val isSelected = circle == selectedCircle
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCircleSelect(if (isSelected) null else circle) },
                            label = { Text(circle, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = vaultPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = vaultPrimary,
                                labelColor = Color.White.copy(alpha = 0.7f),
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.White.copy(alpha = 0.15f),
                                selectedBorderColor = vaultPrimary.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (tags.isNotEmpty()) {
                Text("ジャンル", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.forEach { tag ->
                        val isSelected = tag == selectedTag
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTagSelect(if (isSelected) null else tag) },
                            label = { Text(tag, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = vaultPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = vaultPrimary,
                                labelColor = Color.White.copy(alpha = 0.7f),
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.White.copy(alpha = 0.15f),
                                selectedBorderColor = vaultPrimary.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    }
}
