package com.kazukittin.vault.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kazukittin.vault.data.local.db.WorkEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLibraryScreen(
    viewModel: AudioLibraryViewModel,
    onWorkClick: (WorkEntity) -> Unit,
    onBack: () -> Unit
) {
    val works by viewModel.allWorks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val vaultSurface = Color(0xFF071327)
    val vaultContainer = Color(0xFF142034)
    val vaultPrimary = Color(0xFFA1CCED)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Library", color = vaultPrimary) },
                actions = {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            color = vaultPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.scan("/audio") }) { // デフォルトパス
                            Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = vaultPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = vaultSurface)
            )
        },
        containerColor = vaultSurface
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("サークル、作品、声優で検索...") },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = vaultContainer,
                    focusedContainerColor = vaultContainer,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(works) { work ->
                    WorkCard(
                        work = work,
                        onClick = { onWorkClick(work) },
                        onFavoriteClick = { viewModel.toggleFavorite(work) }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkCard(
    work: WorkEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val vaultContainer = Color(0xFF142034)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = vaultContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = work.coverUrl,
                    contentDescription = work.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (work.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (work.isFavorite) Color.Red else Color.White
                    )
                }
            }
            
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = work.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = work.circle,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (work.rjCode != null) {
                    Text(
                        text = work.rjCode,
                        color = Color(0xFFA1CCED),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
