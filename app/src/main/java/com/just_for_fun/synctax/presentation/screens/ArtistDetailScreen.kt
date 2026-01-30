package com.just_for_fun.synctax.presentation.screens

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.lazy.items
import com.just_for_fun.synctax.presentation.components.optimization.OptimizedLazyColumn
import com.just_for_fun.synctax.presentation.components.utils.PolyShapes
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.app.TooltipIconButton
import com.just_for_fun.synctax.presentation.components.card.SongCard
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.core.utils.ArtistDetails
import com.just_for_fun.synctax.core.utils.RecommendedSong
import kotlinx.coroutines.Dispatchers
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
    onOnlineSongClick: (RecommendedSong) -> Unit = {},
    onGetArtistDetails: ((String, (ArtistDetails?) -> Unit) -> Unit)? = null,
    onLoadMoreSongs: ((String, (List<RecommendedSong>) -> Unit) -> Unit)? = null,
    onRefresh: () -> Unit = {}
) {
    var fetchedDetails by remember { mutableStateOf<ArtistDetails?>(null) }
    var isLoadingArtistDetails by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    // Refresh counter to trigger re-fetch when pull-to-refresh is used
    var refreshKey by remember { mutableStateOf(0) }
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Pagination state for loading more songs
    var additionalSongs by remember { mutableStateOf<List<RecommendedSong>>(emptyList()) }
    var isLoadingMoreSongs by remember { mutableStateOf(false) }
    var hasLoadedAllSongs by remember { mutableStateOf(false) }

    // Handle refresh completion - increment refreshKey to trigger re-fetch
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            // Increment refresh key to trigger the fetch LaunchedEffect
            refreshKey++
            // Reset pagination state on refresh
            additionalSongs = emptyList()
            hasLoadedAllSongs = false
            onRefresh()
            isRefreshing = false
        }
    }

    // Determine if we need to fetch details (image/songs)
    // We fetch if:
    // 1. We are not explicitly in "online mode" (where details are passed in)
    // 2. We have a fetcher provided
    // 3. We haven't fetched yet OR refreshKey changed (pull-to-refresh)
    // 4. EITHER we have no local image/songs OR we just want to enrich with online image
    LaunchedEffect(artistName, refreshKey) {
        if (!isOnline && onGetArtistDetails != null) {
            isLoadingArtistDetails = true
            fetchedDetails = null // Reset before fetching
            // Reset additional songs when fetching new details
            additionalSongs = emptyList()
            hasLoadedAllSongs = false
            onGetArtistDetails(artistName) { details ->
                fetchedDetails = details
                isLoadingArtistDetails = false
            }
        }
    }

    val effectiveDetails = artistDetails ?: fetchedDetails

    // Use fetched details if available for name, image, description
    val displayName = effectiveDetails?.name ?: artistName

    val imageUri = effectiveDetails?.thumbnail ?: songs.firstOrNull()?.albumArtUri.orEmpty()

    // Calculate total song count: local songs + fetched online songs + additional loaded songs
    val songCount = when {
        isOnline && effectiveDetails != null -> effectiveDetails.songs.size + additionalSongs.size
        else -> {
            val localCount = songs.size
            val onlineCount = (fetchedDetails?.songs?.size ?: 0) + additionalSongs.size
            localCount + onlineCount
        }
    }
    
    // Check if more songs can be loaded
    val canLoadMore = effectiveDetails?.hasMoreSongs == true && 
                      effectiveDetails.songsBrowseId != null && 
                      !hasLoadedAllSongs &&
                      onLoadMoreSongs != null

    val description = effectiveDetails?.description ?: "No details about this artist available"

    val subscribers = effectiveDetails?.subscribers ?: ""


    val context = LocalContext.current

    var dominantColor by remember { mutableStateOf(Color(0xFF2A2A2A)) }
    var vibrantColor by remember { mutableStateOf(Color(0xFFFF0033)) }
    var darkMutedColor by remember { mutableStateOf(Color(0xFF1A1A1A)) }

    // Theme-aware colors from AppColors
    val cardBackgroundColor = AppColors.artistDetailCardBackground
    val sectionTitleColor = AppColors.artistDetailTitle
    val sectionSubtitleColor = AppColors.artistDetailBio

    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        if (imageUri.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    if (imageUri.startsWith("http")) {
                        // For online images, use default rich colors
                        dominantColor = Color(0xFF2A2A2A)
                        vibrantColor = Color(0xFFFF0033)
                        darkMutedColor = Color(0xFF1A1A1A)
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
                                            ?: 0xFFFF0033.toInt()

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
                } catch (e: Exception) { /* Keep defaults */
                }
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
                        overflow = TextOverflow.Ellipsis,
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
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        color = vibrantColor
                    )
                }
            ) {
                OptimizedLazyColumn(
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
                                        shape = PolyShapes.Cookie9
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(PolyShapes.Cookie9)
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
                                                .clip(PolyShapes.Cookie9)
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
                                textAlign = TextAlign.Center,
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
                                val shouldShowSeeMore =
                                    description.length > 140 && description != "No details about this artist available"
                                val displayText =
                                    if (shouldShowSeeMore) description.take(140) + "...see more" else description
                                if (shouldShowSeeMore) {
                                    val annotatedString =
                                        buildAnnotatedString {
                                            append(description.take(140))
                                            withStyle(
                                                style = SpanStyle(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            ) {
                                                append("...see more")
                                            }
                                        }
                                    Text(
                                        text = annotatedString,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .padding(horizontal = 32.dp)
                                            .clickable { showBottomSheet = true }
                                    )
                                } else {
                                    Text(
                                        text = displayText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                }
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
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Play all",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = onShuffle,
                                    modifier = Modifier
                                        .height(56.dp)
                                        .weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                                        .copy(
                                            brush = Brush.horizontalGradient(
                                                listOf(
                                                    Color.White.copy(0.7f),
                                                    Color.White.copy(0.4f)
                                                )
                                            )
                                        ),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Icon(
                                        Icons.Default.Shuffle,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Shuffle", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }


                    // Priority:
                    // 1. If explicit online Details provided, show those (Online Mode)
                    // 2. If valid local songs exist, show those (Local Mode)
                    // 3. If fetched details exist (Fallback/Enriched Mode), show those
                    val explicitlyOnline = isOnline && artistDetails != null
                    val hasLocalSongs = songs.isNotEmpty()
                    val hasFetchedSongs = fetchedDetails?.songs?.isNotEmpty() == true

                    if (explicitlyOnline) {
                        items(artistDetails!!.songs) { song ->
                            OnlineSongCardForItem(song, onOnlineSongClick)
                        }
                    } else {
                        // Mixed Mode: Show Local AND/OR Fetched Songs

                        // 1. Local Songs
                        if (hasLocalSongs) {
                            item {
                                Text(
                                    "On Device",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(songs) { song ->
                                SongCard(
                                    song = song,
                                    onClick = { onSongClick(song) },
                                    backgroundColor = cardBackgroundColor,
                                    titleColor = sectionTitleColor,
                                    artistColor = sectionSubtitleColor
                                )
                            }
                        }

                        // 2. Fetched Online Songs
                        if (hasFetchedSongs) {
                            item {
                                Text(
                                    "Top Songs (Online)",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(fetchedDetails!!.songs) { song ->
                                OnlineSongCardForItem(song, onOnlineSongClick)
                            }
                            
                            // Show additional loaded songs
                            if (additionalSongs.isNotEmpty()) {
                                items(additionalSongs) { song ->
                                    OnlineSongCardForItem(song, onOnlineSongClick)
                                }
                            }
                            
                            // Load More button or loading indicator
                            if (canLoadMore || isLoadingMoreSongs) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoadingMoreSongs) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    strokeWidth = 2.dp
                                                )
                                                Text(
                                                    text = "Loading more songs...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White.copy(alpha = 0.7f)
                                                )
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = {
                                                    val browseId = effectiveDetails?.songsBrowseId
                                                    if (browseId != null && onLoadMoreSongs != null) {
                                                        isLoadingMoreSongs = true
                                                        onLoadMoreSongs(browseId) { moreSongs ->
                                                            if (moreSongs.isNotEmpty()) {
                                                                // Filter out duplicates
                                                                val existingIds = (fetchedDetails?.songs?.map { it.videoId } ?: emptyList()) + 
                                                                                  additionalSongs.map { it.videoId }
                                                                val newSongs = moreSongs.filter { it.videoId !in existingIds }
                                                                additionalSongs = additionalSongs + newSongs
                                                            }
                                                            hasLoadedAllSongs = true
                                                            isLoadingMoreSongs = false
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = Color.White
                                                ),
                                                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                                            ) {
                                                Text("Load More Songs")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Loading Online Songs
                        if (isLoadingArtistDetails && !hasFetchedSongs) {
                            item {
                                Text(
                                    "Top Songs (Online)",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp),
                                            color = Color.White.copy(alpha = 0.8f),
                                            strokeWidth = 3.dp
                                        )
                                        Text(
                                            text = "Loading online songs...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. No songs at all
                        if (!hasLocalSongs && !hasFetchedSongs) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No songs found", color = Color.White.copy(0.6f))
                                }
                            }
                        }
                    }
                }
            } // End PullToRefreshBox
        }

        // Bottom Sheet for full description
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Artist Details",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Name: $displayName",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (subscribers.isNotEmpty()) {
                        Text(
                            text = "Subscribers: $subscribers",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text(
                        text = "Description:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OnlineSongCardForItem(
    song: RecommendedSong,
    onClick: (RecommendedSong) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick(song) },
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
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = { onClick(song) }) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White
                )
            }
        }
    }
}