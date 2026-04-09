package com.kazukittin.vault.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    imageUrl: String?,
    fileName: String,
    onBack: () -> Unit
) {
    val vaultSurface = Color(0xFF000000) // 完全な黒背景で写真を引き立たせる
    val vaultPrimary = Color(0xFFA1CCED)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName, color = Color.White, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = vaultSurface.copy(alpha = 0.5f) // 半透明のヘッダー
                )
            )
        },
        containerColor = vaultSurface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(vaultSurface)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit // アスペクト比を維持して画面に収める
                )
            } else {
                Text("画像のURLを取得できませんでした", color = vaultPrimary)
            }
        }
    }
}
