package com.kazukittin.vault.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kazukittin.vault.data.local.db.FolderEntity

@Composable
fun HomeScreen(
    pinnedCollections: List<FolderEntity>,
    allCollections: List<FolderEntity>,
    onFolderClick: (String, String) -> Unit
) {
    val vaultSurface   = Color(0xFF071327)
    val vaultContainer = Color(0xFF142034)

    // pinnedとallを単純に結合して1つのグリッドで表示
    val allFolders = (pinnedCollections + allCollections).distinctBy { it.id }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(vaultSurface),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(allFolders) { folder ->
            CollectionCard(
                folder = folder,
                containerColor = vaultContainer,
                onClick = { onFolderClick(folder.id, folder.name) }
            )
        }
    }
}

@Composable
fun CollectionCard(folder: FolderEntity, containerColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}
