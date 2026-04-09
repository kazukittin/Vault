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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage


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

    val imageItems = items.filter { !it.isDir }

    val vaultSurface   = Color(0xFF071327)
    val vaultContainer = Color(0xFF142034)
    val vaultPrimary   = Color(0xFFA1CCED)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(vaultSurface)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = vaultPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(top = 48.dp, start = 4.dp, end = 4.dp, bottom = 4.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items) { item ->
                    if (item.isDir) {
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
                    } else {
                        val url        = viewModel.getThumbnailUrl(item.path)
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

        // 小さなオーバーレイ戻るボタン（左上に常時表示）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(40.dp)
                .background(Color(0x99071327))
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
                text = folderName,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
        }
    }
}
