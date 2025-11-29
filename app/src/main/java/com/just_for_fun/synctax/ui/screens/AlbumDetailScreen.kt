package com.just_for_fun.synctax.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
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
fun AlbumDetailScreen(
    albumName: String,
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
    var vibrantColor by remember { mutableStateOf(Color(0xFFE91D63)) } // fallback: vibrant red

    // Extract colors from album art
    LaunchedEffect(albumArtUri) {
        if (albumArtUri.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val bitmap = when {
                        albumArtUri.startsWith("http://") || albumArtUri.startsWith("https://") -> {
                            // Load from URL
                            val url = java.net.URL(albumArtUri)
                            val connection = url.openConnection() as java.net.HttpURLConnection
                            connection.doInput = true
                            connection.connect()
                            val inputStream = connection.inputStream
                            BitmapFactory.decodeStream(inputStream)
                        }
                        else -> {
                            // Load from local URI
                            context.contentResolver.openInputStream(albumArtUri.toUri())?.use { inputStream ->
                                BitmapFactory.decodeStream(inputStream)
                            }
                        }
                    }
                    
                    bitmap?.let { bmp ->
                        Palette.from(bmp).generate { palette ->
                            val dominant = palette?.dominantSwatch?.rgb?.let { Color(it) } ?: Color(0xFF121212)
                            val vibrant = palette?.vibrantSwatch?.rgb?.let { Color(it) }
                                ?: palette?.darkVibrantSwatch?.rgb?.let { Color(it) }
                                ?: palette?.mutedSwatch?.rgb?.let { Color(it) }
                                ?: dominant

                            dominantColor = dominant
                            vibrantColor = vibrant
                        }
                    }
                } catch (e: Exception) {
                    // Silently fail — keep fallback colors
                }
            }
        }
    }

    // Beautiful vertical gradient background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            vibrantColor.copy(alpha = 0.9f),
            dominantColor.copy(alpha = 0.6f),
            Color(0xFF121212) // Pure black at bottom
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = albumName,
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradientBrush)
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))

                        // Album Art - Large & Modern
                        Card(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                        ) {
                            if (albumArtUri.isEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        Icons.Default.Album,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(100.dp)
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(albumArtUri)
                                        .crossfade(true)
                                        .error(android.R.drawable.ic_menu_gallery)
                                        .build(),
                                    contentDescription = albumName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Album Name
                        Text(
                            text = albumName,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color.White,
                            maxLines = 2,
                            softWrap = true,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Artist Name
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Song Count
                        Text(
                            text = "${songs.size} songs",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // Action Buttons - Play All (Red) + Shuffle (Outlined)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .fillMaxWidth()
                        ) {
                            Button(
                                onClick = onPlayAll,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE91D63), // Exact red from your image
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Play all", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }

                            OutlinedButton(
                                onClick = onShuffle,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.White.copy(0.8f), Color.White.copy(0.5f))
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Shuffle", fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                // Songs List
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