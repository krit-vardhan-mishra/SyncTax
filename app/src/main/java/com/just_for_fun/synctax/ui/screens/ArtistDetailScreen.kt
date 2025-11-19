package com.just_for_fun.synctax.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.ui.components.card.SongCard
import kotlinx.coroutines.Dispatchers
import com.just_for_fun.synctax.ui.components.app.TooltipIconButton
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    songs: List<Song>,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    val albumArtUri = songs.firstOrNull()?.albumArtUri.orEmpty()
    val context = LocalContext.current

    var dominantColor by remember { mutableStateOf(Color(0xFF121212)) }
    var vibrantColor by remember { mutableStateOf(Color(0xFFE91D63)) } // fallback vibrant red

    LaunchedEffect(albumArtUri) {
        if (albumArtUri.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(albumArtUri.toUri())?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        bitmap?.let {
                            Palette.from(it).generate { palette ->
                                val dominant = palette?.dominantSwatch?.rgb?.let { Color(it) } ?: Color(0xFF121212)
                                val vibrant = palette?.vibrantSwatch?.rgb?.let { Color(it) }
                                    ?: palette?.darkVibrantSwatch?.rgb?.let { Color(it) }
                                    ?: dominant

                                dominantColor = dominant
                                vibrantColor = vibrant
                            }
                        }
                    }
                } catch (e: Exception) { /* ignore */ }
            }
        }
    }

    // Beautiful gradient from top (vibrant) → bottom (dark)
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            vibrantColor.copy(alpha = 0.85f),
            dominantColor.copy(alpha = 0.6f),
            Color(0xFF121212)
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = artistName,
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TooltipIconButton(
                        onClick = onBackClick,
                        tooltipText = "Go back"
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradientBrush)
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))

                        // Large circular artist image
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (albumArtUri.isEmpty()) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp),
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            } else {
                                AsyncImage(
                                    model = albumArtUri,
                                    contentDescription = artistName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${songs.size} songs",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Play All (Red) + Shuffle (Outlined)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Button(
                                onClick = onPlayAll,
                                modifier = Modifier
                                    .height(56.dp)
                                    .weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE91D63), // Perfect red from your image
                                    contentColor = Color.White
                                ),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play all", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }

                            OutlinedButton(
                                onClick = onShuffle,
                                modifier = Modifier
                                    .height(56.dp)
                                    .weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(listOf(Color.White.copy(0.7f), Color.White.copy(0.4f)))
                                ),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Shuffle", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                items(songs) { song ->
                    SongCard(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}