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
    isConnecting: Boolean,
    connectionError: String?,
    onRetry: () -> Unit,
    onManualConnect: () -> Unit,
    onFolderClick: (String, String) -> Unit,
    onSetCategory: (String, String?) -> Unit
) {
    val vaultSurface   = Color(0xFF071327)
    val vaultContainer = Color(0xFF142034)
    val vaultPrimary   = Color(0xFFA1CCED)

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(vaultSurface)
                .padding(top = padding.calculateTopPadding())
        ) {
            when {
                isConnecting -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = vaultPrimary)
                            Text("NASに接続中...", color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }

                connectionError != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            colors = CardDefaults.cardColors(containerColor = vaultContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "接続エラー",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = vaultPrimary
                                )
                                Text(
                                    text = connectionError,
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Wi-Fiに接続されているか、NASが起動しているか確認してください。",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = onRetry,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = vaultPrimary)
                                ) {
                                    Text("再試行", color = vaultSurface)
                                }
                                OutlinedButton(
                                    onClick = onManualConnect,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = vaultPrimary)
                                ) {
                                    Text("手動で接続設定を変更")
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
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
