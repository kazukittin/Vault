package com.kazukittin.vault.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    viewModel: AudioPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val work by viewModel.currentWork.collectAsState()
    val playbackError by viewModel.playbackError.collectAsState()

    val vaultSurface = Color(0xFF071327)
    val vaultPrimary = Color(0xFFA1CCED)

    LaunchedEffect(Unit) {
        viewModel.initController(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playing", color = vaultPrimary) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Album Art
            AsyncImage(
                model = work?.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(280.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Black),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Work Info
            Text(
                text = work?.title ?: "Unknown",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = work?.circle ?: "Unknown Circle",
                color = vaultPrimary,
                fontSize = 16.sp
            )

            if (playbackError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = playbackError!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Seek Bar
            Slider(
                value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                onValueChange = { viewModel.seekTo((it * duration).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = vaultPrimary,
                    activeTrackColor = vaultPrimary,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(position), color = Color.Gray, fontSize = 12.sp)
                Text(formatTime(duration), color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                IconButton(onClick = { viewModel.previous() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = vaultPrimary, modifier = Modifier.size(36.dp))
                }

                FloatingActionButton(
                    onClick = { viewModel.togglePlay() },
                    containerColor = vaultPrimary,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(72.dp)
                ) {
                    val icon = if (isPlaying) {
                        // Pause icon would be better but let's use play arrow with logic or search
                        painterResource(android.R.drawable.ic_media_pause)
                    } else {
                        painterResource(android.R.drawable.ic_media_play)
                    }
                    Icon(painter = icon, contentDescription = "Play/Pause", tint = vaultSurface, modifier = Modifier.size(40.dp))
                }

                IconButton(onClick = { viewModel.next() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = vaultPrimary, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
