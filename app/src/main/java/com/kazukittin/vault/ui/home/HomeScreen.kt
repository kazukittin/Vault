package com.kazukittin.vault.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kazukittin.vault.data.local.db.FolderEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    pinnedCollections: List<FolderEntity>,
    allCollections: List<FolderEntity>,
    onFolderClick: (String, String) -> Unit
) {
    val vaultSurface = Color(0xFF071327)
    val vaultContainer = Color(0xFF142034)
    val vaultPrimary = Color(0xFFA1CCED)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault", color = vaultPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = vaultSurface)
            )
        },
        containerColor = vaultSurface
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .background(vaultSurface)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pinned Section
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "ピン留めされたコレクション",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            items(pinnedCollections) { folder ->
                CollectionCard(
                    folder = folder, 
                    isSmall = false, 
                    containerColor = vaultContainer,
                    onClick = { onFolderClick(folder.id, folder.name) }
                )
            }

            // All Collections Section
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "すべてのフォルダ",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }

            items(allCollections) { folder ->
                CollectionCard(
                    folder = folder, 
                    isSmall = true, 
                    containerColor = vaultContainer,
                    onClick = { onFolderClick(folder.id, folder.name) }
                )
            }
        }
    }
}

@Composable
fun CollectionCard(folder: FolderEntity, isSmall: Boolean, containerColor: Color, onClick: () -> Unit) {
    val cardHeight = if (isSmall) 100.dp else 160.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(
                text = folder.name,
                style = if (isSmall) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}
