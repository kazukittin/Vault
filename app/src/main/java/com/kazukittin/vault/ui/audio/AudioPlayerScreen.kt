package com.kazukittin.vault.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val vaultSurface = Color.Red // DEBUG: まっかっか
    val vaultPrimary = Color.Yellow // DEBUG: きいろ

    LaunchedEffect(Unit) {
        android.util.Log.e("VaultDebug", ">>> AudioPlayerScreen: Composition STARTED")
        viewModel.initController(context)
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            android.util.Log.e("VaultDebug", ">>> AudioPlayerScreen: Composition DISPOSED")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("DEBUG PLAYER", color = vaultPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        work?.let { Text(it.title, color = Color.Yellow, fontSize = 12.sp, maxLines = 1) }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = vaultSurface
    ) { padding ->
        // Background Gradient
        Box(modifier = Modifier.fillMaxSize().background(vaultSurface)) {
            if (work == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = vaultPrimary)
                    Text("WAITING FOR DATA...", color = Color.Yellow, modifier = Modifier.padding(top = 80.dp))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    android.util.Log.i("VaultDebug", ">>> AudioPlayerScreen: Rendering UI for ${work?.title}")
                    // Album Art with Shadow and Glow
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        AsyncImage(
                            model = work?.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(android.R.drawable.ic_menu_report_image),
                            placeholder = painterResource(android.R.drawable.ic_menu_gallery)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Work Info
                    Text(
                        text = work?.title ?: "",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = work?.circle ?: "",
                        color = vaultPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (playbackError != null) {
                        Surface(
                            color = Color.Red.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = playbackError!!,
                                color = Color(0xFFFF8B8B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Seek Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { viewModel.seekTo((it * duration).toLong()) },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = vaultPrimary,
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(position), color = Color.LightGray, fontSize = 12.sp)
                            Text(formatTime(duration), color = Color.LightGray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        IconButton(onClick = { viewModel.previous() }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = vaultPrimary, modifier = Modifier.size(40.dp))
                        }

                        ElevatedButton(
                            onClick = { viewModel.togglePlay() },
                            colors = ButtonDefaults.elevatedButtonColors(containerColor = vaultPrimary),
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            val icon = if (isPlaying) {
                                painterResource(android.R.drawable.ic_media_pause)
                            } else {
                                painterResource(android.R.drawable.ic_media_play)
                            }
                            Icon(painter = icon, contentDescription = "Play/Pause", tint = vaultSurface, modifier = Modifier.size(40.dp))
                        }

                        IconButton(onClick = { viewModel.next() }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = vaultPrimary, modifier = Modifier.size(40.dp))
                        }
                    }
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
