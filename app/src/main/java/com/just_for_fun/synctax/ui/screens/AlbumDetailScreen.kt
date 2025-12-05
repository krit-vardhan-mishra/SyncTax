package com.just_for_fun.synctax.ui.screens

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.isSystemInDarkTheme
import com.just_for_fun.synctax.ui.theme.LightHomeCardBackground
import com.just_for_fun.synctax.ui.theme.LightHomeSectionTitle
import com.just_for_fun.synctax.ui.theme.LightHomeSectionSubtitle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.ui.components.app.TooltipIconButton
import com.just_for_fun.synctax.ui.components.card.SongCard
import com.just_for_fun.synctax.util.AlbumDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumName: String,
    artistName: String,
    songs: List<Song>,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    isOnline: Boolean = false,
    albumDetails: AlbumDetails? = null,
    isAlbumSaved: Boolean = false,
    onSaveAlbumClick: () -> Unit = {},
    onUnsaveAlbumClick: () -> Unit = {}
) {
    val displayAlbumName = if (isOnline && albumDetails != null) albumDetails.title else albumName
    val displayArtistName = if (isOnline && albumDetails != null) albumDetails.artist else artistName
    val displaySongs = songs
    val albumArtUri = if (isOnline && albumDetails != null) albumDetails.thumbnail else songs.firstOrNull()?.albumArtUri.orEmpty()
    val description = when {
        isOnline && albumDetails != null -> {
            val desc = albumDetails.description
            when {
                desc.isNullOrBlank() -> "No description available for this album"
                desc == "null" -> "No description available for this album"
                else -> desc
            }
        }
        else -> "No description available for this album"
    }

    val context = LocalContext.current

    var dominantColor by remember { mutableStateOf(Color(0xFF1A1A2E)) }
    var vibrantColor by remember { mutableStateOf(Color(0xFF6C63FF)) }
    var darkMutedColor by remember { mutableStateOf(Color(0xFF0F0F1E)) }

    // Theme-aware colors
    val isDarkTheme = isSystemInDarkTheme()
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF1A1A1D) else LightHomeCardBackground
    val sectionTitleColor = if (isDarkTheme) Color.White else LightHomeSectionTitle
    val sectionSubtitleColor = if (isDarkTheme) Color(0xFFB3B3B3) else LightHomeSectionSubtitle

    // Extract colors from album art with improved palette
    LaunchedEffect(albumArtUri) {
        if (albumArtUri.isNotEmpty()) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .allowHardware(false)
                    .build()

                val drawable = coil.Coil.imageLoader(context).execute(request).drawable
                val bitmap = (drawable as? BitmapDrawable)?.bitmap

                bitmap?.let {
                    Palette.from(it).generate { palette ->
                        palette?.let { p ->
                            // Get vibrant color with fallback to light vibrant or muted
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
            } catch (e: Exception) {
                // Keep default colors on error
            }
        }
    }

    // Enhanced gradient with smoother transitions and richer colors
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            vibrantColor.copy(alpha = 0.95f),
            dominantColor.copy(alpha = 0.75f),
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
                        text = displayAlbumName,
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
                actions = {
                    TooltipIconButton(
                        onClick = {
                            if (isAlbumSaved) {
                                onUnsaveAlbumClick()
                            } else {
                                onSaveAlbumClick()
                            }
                        },
                        tooltipText = if (isAlbumSaved) "Remove from playlists" else "Add to playlists"
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isAlbumSaved) R.drawable.playlist_added else R.drawable.playlist_add
                            ),
                            contentDescription = if (isAlbumSaved) "Remove from playlists" else "Add to playlists",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
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

                        // Album Art with enhanced card styling
                        Card(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.08f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                vibrantColor.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            ) {
                                if (albumArtUri.isEmpty()) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Album,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(100.dp)
                                        )
                                    }
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(albumArtUri)
                                            .crossfade(true)
                                            .error(android.R.drawable.ic_menu_gallery)
                                            .build(),
                                        contentDescription = albumName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(20.dp))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Album Name
                        Text(
                            text = displayAlbumName,
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
                            text = displayArtistName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Song Count
                        Text(
                            text = "${displaySongs.size} songs",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Description
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // Action Buttons
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
                                    containerColor = Color(0xFFE91D63),
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
                items(displaySongs) { song ->
                    if (isOnline) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onSongClick(song) },
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.albumArtUri,
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
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { onSongClick(song) }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    } else {
                        SongCard(
                            song = song,
                            onClick = { onSongClick(song) },
                            backgroundColor = cardBackgroundColor,
                            titleColor = sectionTitleColor,
                            artistColor = sectionSubtitleColor
                        )
                    }
                }
            }
        }
    }
}