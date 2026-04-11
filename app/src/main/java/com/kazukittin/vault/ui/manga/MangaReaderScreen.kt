package com.kazukittin.vault.ui.manga

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MangaReaderScreen(
    viewModel: MangaReaderViewModel,
    downloadUrl: String,
    zipName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()

    val vaultSurface = Color(0xFF000000)
    val vaultPrimary = Color(0xFFA1CCED)

    LaunchedEffect(downloadUrl) {
        viewModel.loadZip(context, downloadUrl, zipName)
    }

    Box(modifier = Modifier.fillMaxSize().background(vaultSurface)) {
        when (val s = state) {
            is MangaLoadState.Idle -> {}

            is MangaLoadState.Downloading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("ZIPをダウンロード中...", color = Color.White, fontSize = 16.sp)
                    LinearProgressIndicator(
                        progress = { s.progress },
                        modifier = Modifier.width(240.dp),
                        color = vaultPrimary,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Text("${(s.progress * 100).toInt()}%", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }

            is MangaLoadState.Extracting -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = vaultPrimary)
                    Text("ページを展開中...", color = Color.White, fontSize = 16.sp)
                }
            }

            is MangaLoadState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("⚠️ 読み込みエラー", color = Color.Red, fontSize = 18.sp)
                    Text(s.message, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Button(onClick = onBack) { Text("戻る") }
                }
            }

            is MangaLoadState.Ready -> {
                val pages = s.pages
                val pagerState = rememberPagerState(
                    initialPage = currentPage,
                    pageCount = { pages.size }
                )

                LaunchedEffect(pagerState.currentPage) {
                    viewModel.goToPage(pagerState.currentPage)
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { pages[it].name }
                ) { pageIndex ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(pages[pageIndex])
                            .crossfade(true)
                            .build(),
                        contentDescription = "Page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // 上部バー
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(top = 40.dp, bottom = 4.dp, start = 4.dp, end = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る", tint = Color.White)
                        }
                        Text(zipName.substringBeforeLast("."), color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    }
                }

                // 下部バー
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${pagerState.currentPage + 1} / ${pages.size}", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}
