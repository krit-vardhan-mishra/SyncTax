package com.just_for_fun.synctax.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.just_for_fun.synctax.util.ArtistDetails
import com.just_for_fun.synctax.util.RecommendedSong
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
    onShuffle: () -> Unit,
    isOnline: Boolean = false,
    artistDetails: ArtistDetails? = null,
    onOnlineSongClick: (RecommendedSong) -> Unit = {}
) {
    val displayName = if (isOnline && artistDetails != null) artistDetails.name else artistName
    val displaySongs = if (isOnline && artistDetails != null) artistDetails.songs else songs
    val imageUri = if (isOnline && artistDetails != null) artistDetails.thumbnail else songs.firstOrNull()?.albumArtUri.orEmpty()
    val songCount = if (isOnline && artistDetails != null) artistDetails.songs.size else songs.size
    val description = when {
        isOnline && artistDetails != null -> {
            val desc = artistDetails.description
            when {
                desc.isNullOrBlank() -> "No details about this artist available"
                desc == "null" -> "No details about this artist available"
                else -> desc
            }
        }
        else -> "No details about this artist available"
    }
    val subscribers = if (isOnline && artistDetails != null) artistDetails.subscribers else ""

    val context = LocalContext.current

    var dominantColor by remember { mutableStateOf(Color(0xFF1A1A2E)) }
    var vibrantColor by remember { mutableStateOf(Color(0xFF6C63FF)) }
    var darkMutedColor by remember { mutableStateOf(Color(0xFF0F0F1E)) }

    LaunchedEffect(imageUri) {
        if (imageUri.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    if (imageUri.startsWith("http")) {
                        // For online images, use default rich colors
                        dominantColor = Color(0xFF1A1A2E)
                        vibrantColor = Color(0xFF6C63FF)
                        darkMutedColor = Color(0xFF0F0F1E)
                    } else {
                        // Local URI - extract palette
                        context.contentResolver.openInputStream(imageUri.toUri())?.use { stream ->
                            val bitmap = BitmapFactory.decodeStream(stream)
                            bitmap?.let {
                                Palette.from(it).generate { palette ->
                                    palette?.let { p ->
                                        // Get vibrant color with fallback chain
                                        val vibrant = p.vibrantSwatch?.rgb
                                            ?: p.lightVibrantSwatch?.rgb
                                            ?: p.mutedSwatch?.rgb
                                            ?: 0xFF6C63FF.toInt()

                                        // Get dark muted for depth
                                        val darkMuted = p.darkMutedSwatch?.rgb
                                            ?: p.darkVibrantSwatch?.rgb
                                            ?: 0xFF0F0F1E.toInt()

                                        // Get dominant for middle gradient
                                        val dominant = p.dominantSwatch?.rgb
                                            ?: p.mutedSwatch?.rgb
                                            ?: 0xFF1A1A2E.toInt()

                                        vibrantColor = Color(vibrant)
                                        darkMutedColor = Color(darkMuted)
                                        dominantColor = Color(dominant)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) { /* Keep defaults */ }
            }
        }
    }

    // Enhanced gradient with richer color transitions
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            vibrantColor.copy(alpha = 0.9f),
            dominantColor.copy(alpha = 0.7f),
            darkMutedColor.copy(alpha = 0.85f),
            Color(0xFF0A0A0F)
        ),
        startY = 0f,
        endY = 2000f
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayName,
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

                        // Enhanced circular artist image with glow effect
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            vibrantColor.copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                        radius = 350f
                                    ),
                                    shape = CircleShape
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (imageUri.isEmpty()) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp),
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                } else {
                                    AsyncImage(
                                        model = imageUri,
                                        contentDescription = displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${songCount} songs" + if (subscribers.isNotEmpty()) " • $subscribers subscribers" else "",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        if (description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Action Buttons
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
                                    containerColor = Color(0xFFE91D63),
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

                if (isOnline && artistDetails != null) {
                    items(artistDetails.songs) { song ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onOnlineSongClick(song) },
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.thumbnail,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { onOnlineSongClick(song) }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
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
}