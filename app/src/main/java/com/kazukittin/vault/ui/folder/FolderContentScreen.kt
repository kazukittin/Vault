package com.kazukittin.vault.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kazukittin.vault.data.remote.SynoFolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContentScreen(
    folderName: String,
    viewModel: FolderContentViewModel,
    onBack: () -> Unit,
    onFolderClick: (String, String) -> Unit,
    onPhotoClick: (Int) -> Unit
) {
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // フォルダと画像を分ける（画像のインデックスを計算するため）
    val imageItems = items.filter { !it.isDir }
    
    val vaultSurface = Color(0xFF071327)
    val vaultContainer = Color(0xFF142034)
    val vaultPrimary = Color(0xFFA1CCED)

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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = vaultPrimary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = padding,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items) { item ->
                    if (item.isDir) {
                        // フォルダ表示
                        Card(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onFolderClick(item.path, item.name) },
                            colors = CardDefaults.cardColors(containerColor = vaultContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = vaultPrimary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(item.name, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    } else {
                        // 画像表示 (サムネイル)
                        val url = viewModel.getThumbnailUrl(item.path)
                        val originalUrl = viewModel.getOriginalImageUrl(item.path)
                        val imageIndex = imageItems.indexOf(item)
                        
                        AsyncImage(
                            model = url,
                            contentDescription = item.name,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(vaultContainer)
                                .clickable { 
                                    if (imageIndex >= 0) onPhotoClick(imageIndex) 
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
