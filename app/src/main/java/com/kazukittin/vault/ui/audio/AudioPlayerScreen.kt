package com.kazukittin.vault.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// ─────────────────────────────────────────────────────────────
// フル音声プレイヤー
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    viewModel: AudioPlayerViewModel,
    onBack: () -> Unit,
    onMinimize: () -> Unit = onBack
) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val work by viewModel.currentWork.collectAsState()
    val currentTrackTitle by viewModel.currentTrackTitle.collectAsState()
    val playbackError by viewModel.playbackError.collectAsState()

    val bgDark   = Color(0xFF071327)
    val bgMid    = Color(0xFF0D2040)
    val primary  = Color(0xFFA1CCED)

    LaunchedEffect(Unit) {
        android.util.Log.wtf("VaultDebug", ">>> AudioPlayerScreen: COMPOSITION STARTED")
        viewModel.initController(context)
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            android.util.Log.e("VaultDebug", ">>> AudioPlayerScreen: Composition DISPOSED")
        }
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(bgMid, bgDark))
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragOffset = 0f },
                    onDragEnd   = { dragOffset = 0f },
                    onDragCancel = { dragOffset = 0f },
                    onVerticalDrag = { _, delta ->
                        dragOffset += delta
                        if (dragOffset > 150f) {
                            dragOffset = 0f
                            onMinimize()
                        }
                    }
                )
            }
    ) {
        if (work == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.weight(0.06f))

                // ── カバーアート ──
                Box(
                    modifier = Modifier
                        .weight(0.52f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF142034)),
                    contentAlignment = Alignment.Center
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

                Spacer(modifier = Modifier.height(28.dp))

                // ── トラック名 ──
                Text(
                    text = currentTrackTitle.ifEmpty { work?.title ?: "" },
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))

                // ── ワーク / サークル名 ──
                Text(
                    text = work?.circle ?: "",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                if (playbackError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
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

                // ── シークバー ──
                val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
                Slider(
                    value = progress,
                    onValueChange = { viewModel.seekTo((it * duration).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(position), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text(formatTime(duration), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 再生コントロール ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { viewModel.previous() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipPrevious, "前へ", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Surface(
                        onClick = { viewModel.togglePlay() },
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "再生/一時停止",
                                tint = bgDark,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.next() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipNext, "次へ", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.weight(0.08f))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
