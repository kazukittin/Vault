package com.kazukittin.vault.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kazukittin.vault.data.local.db.TrackEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDetailScreen(
    viewModel: AudioDetailViewModel,
    onTrackClick: (List<TrackEntity>, Int) -> Unit,
    onBack: () -> Unit
) {
    val work by viewModel.work.collectAsState()
    val tracks by viewModel.tracks.collectAsState()

    if (work == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val vaultSurface = Color(0xFF071327)
    val vaultContainer = Color(0xFF142034)
    val vaultPrimary = Color(0xFFA1CCED)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(work!!.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = vaultSurface,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = vaultSurface
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                HeaderSection(work = work!!, vaultPrimary = vaultPrimary)
            }

            item {
                Text(
                    text = "Tracks",
                    modifier = Modifier.padding(16.dp),
                    color = vaultPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            items(tracks.indexed()) { (index, track) ->
                TrackItem(
                    track = track,
                    onItemClick = { onTrackClick(tracks, index) }
                )
            }
        }
    }
}

private fun <T> List<T>.indexed(): List<Pair<Int, T>> = mapIndexed { i, t -> i to t }

@Composable
fun HeaderSection(work: com.kazukittin.vault.data.local.db.WorkEntity, vaultPrimary: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = work.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color.Black),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = work.circle,
                color = vaultPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = work.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (work.cv.isNotBlank()) {
                Text(
                    text = "CV: ${work.cv}",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
            
            // Tags
            if (work.tags.isNotBlank()) {
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    work.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFF2C3E50),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}

@Composable
fun TrackItem(track: TrackEntity, onItemClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onItemClick() },
        headlineContent = { Text(track.title, color = Color.White) },
        leadingContent = {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.LightGray)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
