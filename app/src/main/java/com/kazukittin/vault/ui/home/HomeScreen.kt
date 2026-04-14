package com.kazukittin.vault.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazukittin.vault.data.local.db.FolderEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    pinnedCollections: List<FolderEntity>,
    allCollections: List<FolderEntity>,
    onFolderClick: (String, String) -> Unit,
    onSetCategory: (String, String?) -> Unit
) {
    val vaultSurface   = Color(0xFF071327)
    val vaultContainer = Color(0xFF142034)
    val vaultPrimary   = Color(0xFFA1CCED)

    // pinnedとallを単純に結合して1つのグリッドで表示
    val allFolders = (pinnedCollections + allCollections).distinctBy { it.id }

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
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                start = 16.dp, end = 16.dp, bottom = 16.dp
            ),
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
                    onClick = { onFolderClick(folder.id, folder.name) },
                    onSetCategory = { category -> onSetCategory(folder.id, category) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionCard(
    folder: FolderEntity,
    containerColor: Color,
    onClick: () -> Unit,
    onSetCategory: (String?) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val categories = listOf("マンガ", "ボイス", "画像", "ビデオ")
    val vaultPrimary = Color(0xFFA1CCED)

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                
                if (folder.category != null) {
                    Surface(
                        color = vaultPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = folder.category,
                            color = vaultPrimary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(Color(0xFF142034))
        ) {
            Text(
                "属性を設定", 
                color = Color.White.copy(alpha = 0.5f), 
                fontSize = 12.sp, 
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat, color = Color.White) },
                    onClick = {
                        onSetCategory(cat)
                        showMenu = false
                    }
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            DropdownMenuItem(
                text = { Text("解除", color = Color.Gray) },
                onClick = {
                    onSetCategory(null)
                    showMenu = false
                }
            )
        }
    }
}
