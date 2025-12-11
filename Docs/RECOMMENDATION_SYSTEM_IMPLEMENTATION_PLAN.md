# Online Listening History-Based Recommendation System Implementation Plan

## Overview
This document outlines the complete implementation of a personalized recommendation system for SyncTax music app, utilizing the user's online listening history stored in `OnlineListeningHistory` entity. The system will provide Spotify/YouTube Music-like recommendations without requiring OAuth authentication.

## Current Data Structure
The `OnlineListeningHistory` entity currently stores:
- `videoId`, `title`, `artist`, `thumbnailUrl`, `watchUrl`, `timestamp`

## Enhanced Data Model

### 1. Enhanced OnlineListeningHistory Entity
```kotlin
@Entity(tableName = "online_listening_history")
data class OnlineListeningHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val watchUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Enhanced fields for better recommendations
    val playDuration: Long = 0,        // How long user listened (seconds)
    val totalDuration: Long = 0,       // Song total duration (seconds)
    val completionRate: Float = 0f,    // playDuration/totalDuration
    val playCount: Int = 1,            // How many times played
    val userRating: Int? = null,       // 1-5 star rating (future feature)
    val skipCount: Int = 0,            // How many times skipped
    val genre: String? = null,         // Genre if available from metadata
    val mood: String? = null,          // Mood classification (future)
    val source: String = "online"      // "online" or "local"
)
```

### 2. Recommendation Cache Entity
```kotlin
@Entity(tableName = "recommendations_cache")
data class RecommendationCache(
    @PrimaryKey
    val cacheKey: String, // e.g., "artist_based", "discovery", "similar_songs"
    val recommendationsJson: String, // JSON string of Song list
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long // Cache validity timestamp
)
```

### 3. Recommendation Interaction Tracking
```kotlin
@Entity(tableName = "recommendation_interactions")
data class RecommendationInteraction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recommendationId: String,    // Cache key that generated this rec
    val songId: String,              // Video ID of recommended song
    val action: String,              // "played", "skipped", "liked", "disliked"
    val timestamp: Long = System.currentTimeMillis(),
    val source: String               // "artist_based", "discovery", etc.
)
```

## Core Services Architecture

### 1. ListeningAnalyticsService
Analyzes user listening patterns from history data.

```kotlin
class ListeningAnalyticsService(
    private val historyDao: OnlineListeningHistoryDao
) {
    data class UserPreferencesData(
        val topArtists: List<String>,
        val favoriteGenres: List<String>,
        val listeningPatterns: Map<String, Any>, // Time-based patterns
        val completionRates: Map<String, Float>, // Song completion rates
        val skipRates: Map<String, Float>        // Skip rates
    )
    
    suspend fun getUserPreferences(): UserPreferencesData {
        val history = historyDao.getAllHistory()
        
        return UserPreferencesData(
            topArtists = analyzeTopArtists(history),
            favoriteGenres = analyzeGenres(history),
            listeningPatterns = analyzeTimePatterns(history),
            completionRates = analyzeCompletionRates(history),
            skipRates = analyzeSkipRates(history)
        )
    }
    
    private fun analyzeTopArtists(history: List<OnlineListeningHistory>): List<String> {
        return history
            .groupBy { it.artist }
            .mapValues { (_, songs) -> 
                songs.sumOf { it.playCount } + 
                songs.sumOf { (it.completionRate * 10).toInt() }
            }
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
    }
    
    private fun analyzeGenres(history: List<OnlineListeningHistory>): List<String> {
        return history
            .filter { it.genre != null }
            .groupBy { it.genre!! }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }
    
    private fun analyzeTimePatterns(history: List<OnlineListeningHistory>): Map<String, Any> {
        // Analyze listening patterns by hour, day of week, etc.
        val hourPattern = history.groupBy { 
            java.util.Calendar.getInstance().apply {
                timeInMillis = it.timestamp
            }.get(java.util.Calendar.HOUR_OF_DAY)
        }
        
        return mapOf(
            "peakHours" to hourPattern.entries
                .sortedByDescending { it.value.size }
                .take(3)
                .map { it.key }
        )
    }
    
    private fun analyzeCompletionRates(history: List<OnlineListeningHistory>): Map<String, Float> {
        return history
            .filter { it.totalDuration > 0 }
            .associate { it.videoId to it.completionRate }
    }
    
    private fun analyzeSkipRates(history: List<OnlineListeningHistory>): Map<String, Float> {
        return history
            .filter { it.skipCount > 0 }
            .associate { it.videoId to (it.skipCount.toFloat() / (it.playCount + it.skipCount)) }
    }
}
```

### 2. RecommendationService
Core service that generates recommendations using various algorithms.

```kotlin
class RecommendationService(
    private val analytics: ListeningAnalyticsService,
    private val ytmusicApi: YTMusic,
    private val historyDao: OnlineListeningHistoryDao,
    private val cacheDao: RecommendationCacheDao
) {
    
    data class RecommendationResult(
        val artistBased: List<Song>,
        val genreBased: List<Song>,
        val similarSongs: List<Song>,
        val discovery: List<Song>,
        val trending: List<Song>
    )
    
    suspend fun generateRecommendations(forceRefresh: Boolean = false): RecommendationResult {
        // Check cache first
        if (!forceRefresh) {
            val cached = getCachedRecommendations()
            if (cached != null) return cached
        }
        
        val preferences = analytics.getUserPreferences()
        val recentHistory = historyDao.getRecentHistory(50) // Last 50 songs
        
        val result = RecommendationResult(
            artistBased = generateArtistBasedRecommendations(preferences.topArtists),
            genreBased = generateGenreBasedRecommendations(preferences.favoriteGenres),
            similarSongs = generateSimilarSongRecommendations(recentHistory),
            discovery = generateDiscoveryRecommendations(recentHistory, preferences),
            trending = getTrendingRecommendations()
        )
        
        // Cache the results
        cacheRecommendations(result)
        
        return result
    }
    
    private suspend fun generateArtistBasedRecommendations(topArtists: List<String>): List<Song> {
        val recommendations = mutableListOf<Song>()
        
        for (artist in topArtists.take(5)) {
            try {
                // Try to get artist info and top songs
                val artistInfo = ytmusicApi.get_artist(artist)
                val topSongs = artistInfo.getTopSongs()?.take(5) ?: emptyList()
                recommendations.addAll(topSongs)
            } catch (e: Exception) {
                // Fallback: search for artist songs
                try {
                    val searchResults = ytmusicApi.search("$artist songs", filter = "songs")
                    recommendations.addAll(searchResults.take(3))
                } catch (e2: Exception) {
                    // Continue to next artist
                }
            }
        }
        
        return recommendations.distinctBy { it.videoId }.take(20)
    }
    
    private suspend fun generateGenreBasedRecommendations(genres: List<String>): List<Song> {
        val recommendations = mutableListOf<Song>()
        
        for (genre in genres.take(3)) {
            try {
                // Search for genre-based playlists or songs
                val searchResults = ytmusicApi.search("$genre music", filter = "playlists")
                if (searchResults.isNotEmpty()) {
                    val playlist = ytmusicApi.get_playlist(searchResults.first().browseId)
                    recommendations.addAll(playlist.tracks?.take(5) ?: emptyList())
                }
            } catch (e: Exception) {
                // Continue to next genre
            }
        }
        
        return recommendations.distinctBy { it.videoId }.take(15)
    }
    
    private suspend fun generateSimilarSongRecommendations(history: List<OnlineListeningHistory>): List<Song> {
        val recommendations = mutableListOf<Song>()
        
        // Get songs with high completion rates (user liked them)
        val likedSongs = history
            .filter { it.completionRate > 0.6f && it.skipCount == 0 }
            .sortedByDescending { it.timestamp }
            .take(10)
        
        for (song in likedSongs) {
            try {
                val related = ytmusicApi.get_song_related(song.videoId)
                recommendations.addAll(related.take(3))
            } catch (e: Exception) {
                // Try search-based related songs
                try {
                    val searchResults = ytmusicApi.search("songs similar to ${song.title} by ${song.artist}", filter = "songs")
                    recommendations.addAll(searchResults.take(2))
                } catch (e2: Exception) {
                    // Continue
                }
            }
        }
        
        return recommendations.distinctBy { it.videoId }.take(20)
    }
    
    private suspend fun generateDiscoveryRecommendations(
        history: List<OnlineListeningHistory>, 
        preferences: ListeningAnalyticsService.UserPreferencesData
    ): List<Song> {
        val listenedArtists = history.map { it.artist }.toSet()
        val recommendations = mutableListOf<Song>()
        
        // Find related artists of top listened artists
        val topArtist = preferences.topArtists.firstOrNull()
        
        if (topArtist != null) {
            try {
                val artistInfo = ytmusicApi.get_artist(topArtist)
                val relatedArtists = artistInfo.relatedArtists?.take(5) ?: emptyList()
                
                val newArtists = relatedArtists.filter { it.name !in listenedArtists }
                
                for (artist in newArtists.take(3)) {
                    try {
                        val songs = ytmusicApi.search("${artist.name} popular songs", filter = "songs")
                        recommendations.addAll(songs.take(3))
                    } catch (e: Exception) {
                        // Continue
                    }
                }
            } catch (e: Exception) {
                // Fallback to trending
                return getTrendingRecommendations().take(15)
            }
        }
        
        return recommendations.distinctBy { it.videoId }.take(15)
    }
    
    private suspend fun getTrendingRecommendations(): List<Song> {
        try {
            val charts = ytmusicApi.get_charts()
            return charts.entries?.take(20) ?: emptyList()
        } catch (e: Exception) {
            // Fallback to search trending
            try {
                return ytmusicApi.search("trending music", filter = "songs").take(15)
            } catch (e2: Exception) {
                return emptyList()
            }
        }
    }
    
    private suspend fun getCachedRecommendations(): RecommendationResult? {
        try {
            val artistBased = cacheDao.getValidRecommendations("artist_based")
            val genreBased = cacheDao.getValidRecommendations("genre_based") 
            val similarSongs = cacheDao.getValidRecommendations("similar_songs")
            val discovery = cacheDao.getValidRecommendations("discovery")
            val trending = cacheDao.getValidRecommendations("trending")
            
            if (artistBased != null && similarSongs != null && discovery != null && trending != null) {
                return RecommendationResult(
                    artistBased = artistBased,
                    genreBased = genreBased ?: emptyList(),
                    similarSongs = similarSongs,
                    discovery = discovery,
                    trending = trending
                )
            }
        } catch (e: Exception) {
            // Cache miss or error
        }
        return null
    }
    
    private suspend fun cacheRecommendations(result: RecommendationResult) {
        val expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
        
        cacheDao.saveRecommendations("artist_based", result.artistBased, expiryTime)
        cacheDao.saveRecommendations("genre_based", result.genreBased, expiryTime)
        cacheDao.saveRecommendations("similar_songs", result.similarSongs, expiryTime)
        cacheDao.saveRecommendations("discovery", result.discovery, expiryTime)
        cacheDao.saveRecommendations("trending", result.trending, expiryTime)
    }
}
```

### 3. Database DAOs

```kotlin
@Dao
interface OnlineListeningHistoryDao {
    @Query("SELECT * FROM online_listening_history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<OnlineListeningHistory>
    
    @Query("SELECT * FROM online_listening_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<OnlineListeningHistory>
    
    @Query("SELECT * FROM online_listening_history WHERE videoId = :videoId")
    suspend fun getSongHistory(videoId: String): List<OnlineListeningHistory>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: OnlineListeningHistory): Long
    
    @Update
    suspend fun update(history: OnlineListeningHistory)
    
    @Query("DELETE FROM online_listening_history WHERE id = :id")
    suspend fun delete(id: Long)
}

```kotlin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Dao
interface RecommendationCacheDao {
    @Query("SELECT * FROM recommendations_cache WHERE cacheKey = :key AND expiresAt > :currentTime")
    suspend fun getValidCache(key: String, currentTime: Long = System.currentTimeMillis()): RecommendationCache?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: RecommendationCache)
    
    @Query("DELETE FROM recommendations_cache WHERE cacheKey = :key")
    suspend fun delete(key: String)
    
    @Query("DELETE FROM recommendations_cache WHERE expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis())
    
    // Helper method to save recommendations
    suspend fun saveRecommendations(key: String, songs: List<Song>, expiryTime: Long) {
        val json = kotlinx.serialization.json.Json.encodeToString(songs)
        val cache = RecommendationCache(
            cacheKey = key,
            recommendationsJson = json,
            expiresAt = expiryTime
        )
        insert(cache)
    }
    
    // Helper method to get valid recommendations
    suspend fun getValidRecommendations(key: String): List<Song>? {
        val cache = getValidCache(key)
        return cache?.let {
            try {
                kotlinx.serialization.json.Json.decodeFromString(it.recommendationsJson)
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Dao
interface RecommendationInteractionDao {
    @Insert
    suspend fun insert(interaction: RecommendationInteraction)
    
    @Query("SELECT * FROM recommendation_interactions ORDER BY timestamp DESC")
    suspend fun getAllInteractions(): List<RecommendationInteraction>
    
    @Query("SELECT * FROM recommendation_interactions WHERE songId = :songId ORDER BY timestamp DESC")
    suspend fun getSongInteractions(songId: String): List<RecommendationInteraction>
    
    @Query("SELECT COUNT(*) FROM recommendation_interactions WHERE action = 'played' AND source = :source")
    suspend fun getPlayCountForSource(source: String): Int
}
```

## UI Components

### 1. Home Screen Integration

```kotlin
import androidx.navigation.NavController

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    recommendationViewModel: RecommendationViewModel,
    navController: NavController
) {
    val recommendations by recommendationViewModel.recommendations.collectAsState()
    val isLoadingRecommendations by recommendationViewModel.isLoading.collectAsState()
    
    LazyColumn {
        // Existing content...
        
        // Recommendations Section
        item {
            if (isLoadingRecommendations) {
                RecommendationSkeleton()
            } else if (recommendations != null) {
                RecommendationsSection(
                    recommendations = recommendations!!,
                    onSongClick = { song ->
                        playerViewModel.playSong(song)
                        // Track interaction
                        recommendationViewModel.trackInteraction(song.videoId, "played", song.source)
                    },
                    onViewAllClick = {
                        // Navigate to detailed recommendations screen
                        navController.navigate("recommendations_detail")
                    }
                )
            } else {
                EmptyRecommendationsPrompt(
                    onExploreClick = { /* Navigate to search */ }
                )
            }
        }
    }
}
```

### 2. RecommendationsSection Component

```kotlin
@Composable
fun RecommendationsSection(
    recommendations: RecommendationService.RecommendationResult,
    onSongClick: (Song) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column {
        // Header with View All button instead of title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Online Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(onClick = onViewAllClick) {
                Text("View All")
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View All",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        
        // 3x3 Grid of 9 recommendations
        val gridRecommendations = (recommendations.artistBased.take(3) + 
                                  recommendations.similarSongs.take(3) + 
                                  recommendations.discovery.take(3)).shuffled().take(9)
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(400.dp) // Fixed height for 3 rows
        ) {
            items(gridRecommendations) { song ->
                RecommendationGridCard(
                    song = song,
                    reason = getRecommendationReason(song, recommendations),
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun getRecommendationReason(song: Song, recommendations: RecommendationService.RecommendationResult): String {
    return when {
        recommendations.artistBased.any { it.videoId == song.videoId } -> 
            "Based on artists you love"
        recommendations.discovery.any { it.videoId == song.videoId } -> 
            "Discover new music"
        recommendations.similarSongs.any { it.videoId == song.videoId } -> 
            "Similar to songs you've enjoyed"
        else -> "Recommended for you"
    }
}
```

### 3. RecommendationGridCard Component

```kotlin
@Composable
fun RecommendationGridCard(
    song: Song,
    reason: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f), // Square cards
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Album art
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Crop
            )
            
            // Song info - compact
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
```

### 4. RecommendationCard Component (for horizontal lists)

```kotlin
@Composable
fun RecommendationCard(
    song: Song,
    reason: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(200.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Album art
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            
            // Song info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (reason.isNotEmpty()) {
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
```

### 5. Loading and Empty States

```kotlin
@Composable
fun RecommendationSkeleton() {
    Column {
        // Header skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(24.dp)
                    .shimmer()
            )
            
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(24.dp)
                    .shimmer()
            )
        }
        
        // 3x3 Grid skeleton
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(400.dp)
        ) {
            items(9) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shimmer()
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyRecommendationsPrompt(onExploreClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Start listening to get personalized recommendations",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Play some songs and we'll suggest music you'll love",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onExploreClick) {
                Text("Explore Music")
            }
        }
    }
}
```

## Detailed Recommendations Screen

### 1. RecommendationsDetailScreen

```kotlin
import androidx.compose.material3.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun RecommendationsDetailScreen(
    recommendationViewModel: RecommendationViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit
) {
    val recommendations by recommendationViewModel.recommendations.collectAsState()
    val isLoading by recommendationViewModel.isLoading.collectAsState()
    val currentShuffleBatch by recommendationViewModel.currentShuffleBatch.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recommendations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { recommendationViewModel.shuffleRecommendations() }
                    ) {
                        Icon(Icons.Default.Shuffle, "Shuffle")
                    }
                    IconButton(
                        onClick = { recommendationViewModel.refreshRecommendations() }
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                recommendations == null -> {
                    EmptyRecommendationsPrompt(
                        onExploreClick = { /* Navigate to search */ }
                    )
                }
                else -> {
                    RecommendationsDetailContent(
                        recommendations = recommendations!!,
                        currentBatch = currentShuffleBatch,
                        onSongClick = { song ->
                            playerViewModel.playSong(song)
                            recommendationViewModel.trackInteraction(song.videoId, "played", song.source)
                        },
                        onLoadMore = {
                            recommendationViewModel.loadNextShuffleBatch()
                        }
                    )
                }
            }
        }
    }
}
```

### 2. RecommendationsDetailContent Component

```kotlin
@Composable
fun RecommendationsDetailContent(
    recommendations: RecommendationService.RecommendationResult,
    currentBatch: List<Song>,
    onSongClick: (Song) -> Unit,
    onLoadMore: () -> Unit
) {
    val allRecommendations = recommendations.artistBased + 
                            recommendations.genreBased + 
                            recommendations.similarSongs + 
                            recommendations.discovery + 
                            recommendations.trending
    
    val displaySongs = if (currentBatch.isNotEmpty()) currentBatch else allRecommendations.take(20)
    
    LazyColumn {
        // Categories
        item {
            RecommendationCategorySection(
                title = "Based on Your Artists",
                songs = recommendations.artistBased.take(10),
                onSongClick = onSongClick
            )
        }
        
        item {
            RecommendationCategorySection(
                title = "Similar Songs",
                songs = recommendations.similarSongs.take(10),
                onSongClick = onSongClick
            )
        }
        
        item {
            RecommendationCategorySection(
                title = "Discover New Music",
                songs = recommendations.discovery.take(10),
                onSongClick = onSongClick
            )
        }
        
        item {
            RecommendationCategorySection(
                title = "Trending Now",
                songs = recommendations.trending.take(10),
                onSongClick = onSongClick
            )
        }
        
        // Current shuffle batch
        if (currentBatch.isNotEmpty()) {
            item {
                Text(
                    text = "Shuffle Mix (${currentBatch.size} songs)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            items(currentBatch) { song ->
                SongListItem(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
            
            item {
                Button(
                    onClick = onLoadMore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Load More Shuffle Songs")
                }
            }
        }
    }
}
```

### 3. RecommendationCategorySection Component

```kotlin
@Composable
fun RecommendationCategorySection(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    if (songs.isEmpty()) return
    
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs) { song ->
                RecommendationCard(
                    song = song,
                    reason = "", // No reason needed in detail view
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}
```

### 4. SongListItem Component

```kotlin
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Play button
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play"
            )
        }
    }
}
```

## Navigation Setup

Add the detailed recommendations screen to your navigation graph:

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                homeViewModel = hiltViewModel(),
                playerViewModel = hiltViewModel(),
                recommendationViewModel = hiltViewModel(),
                navController = navController
            )
        }
        
        composable("recommendations_detail") {
            RecommendationsDetailScreen(
                recommendationViewModel = hiltViewModel(),
                playerViewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Other screens...
    }
}
```

## ViewModel Layer

### 1. RecommendationViewModel

```kotlin
import kotlin.math.minOf

class RecommendationViewModel(
    private val recommendationService: RecommendationService,
    private val interactionDao: RecommendationInteractionDao
) : ViewModel() {
    
    private val _recommendations = MutableStateFlow<RecommendationService.RecommendationResult?>(null)
    val recommendations: StateFlow<RecommendationService.RecommendationResult?> = _recommendations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _currentShuffleBatch = MutableStateFlow<List<Song>>(emptyList())
    val currentShuffleBatch: StateFlow<List<Song>> = _currentShuffleBatch.asStateFlow()
    
    private var allAvailableSongs = listOf<Song>()
    private var currentBatchIndex = 0
    private val BATCH_SIZE = 15 // Load 15 songs at a time to avoid performance issues
    
    init {
        loadRecommendations()
    }
    
    fun loadRecommendations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val result = recommendationService.generateRecommendations()
                _recommendations.value = result
                
                // Prepare shuffle pool
                allAvailableSongs = (result.artistBased + result.genreBased + 
                                   result.similarSongs + result.discovery + 
                                   result.trending).distinctBy { it.videoId }.shuffled()
                currentBatchIndex = 0
                _currentShuffleBatch.value = emptyList()
                
            } catch (e: Exception) {
                _error.value = "Failed to load recommendations: ${e.message}"
                Log.e("RecommendationViewModel", "Error loading recommendations", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshRecommendations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val result = recommendationService.generateRecommendations(forceRefresh = true)
                _recommendations.value = result
                
                // Refresh shuffle pool
                allAvailableSongs = (result.artistBased + result.genreBased + 
                                   result.similarSongs + result.discovery + 
                                   result.trending).distinctBy { it.videoId }.shuffled()
                currentBatchIndex = 0
                _currentShuffleBatch.value = emptyList()
                
            } catch (e: Exception) {
                _error.value = "Failed to refresh recommendations: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun shuffleRecommendations() {
        if (allAvailableSongs.isEmpty()) {
            loadNextShuffleBatch()
        } else {
            // Shuffle current batch
            _currentShuffleBatch.value = _currentShuffleBatch.value.shuffled()
        }
    }
    
    fun loadNextShuffleBatch() {
        if (allAvailableSongs.isEmpty()) return
        
        val startIndex = currentBatchIndex
        val endIndex = minOf(startIndex + BATCH_SIZE, allAvailableSongs.size)
        
        if (startIndex >= allAvailableSongs.size) {
            // Wrap around or reshuffle
            allAvailableSongs = allAvailableSongs.shuffled()
            currentBatchIndex = 0
            loadNextShuffleBatch()
            return
        }
        
        val newBatch = allAvailableSongs.subList(startIndex, endIndex)
        _currentShuffleBatch.value = newBatch
        currentBatchIndex = endIndex
    }
    
    fun trackInteraction(songId: String, action: String, source: String) {
        viewModelScope.launch {
            try {
                val interaction = RecommendationInteraction(
                    recommendationId = source,
                    songId = songId,
                    action = action,
                    source = source
                )
                interactionDao.insert(interaction)
            } catch (e: Exception) {
                Log.e("RecommendationViewModel", "Error tracking interaction", e)
            }
        }
    }
}
```

## Background Processing

### 1. Periodic Recommendation Updates

```kotlin
class RecommendationUpdateWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val recommendationService: RecommendationService
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            Log.d("RecommendationWorker", "Starting background recommendation update")
            
            // Generate fresh recommendations
            recommendationService.generateRecommendations(forceRefresh = true)
            
            Log.d("RecommendationWorker", "Recommendation update completed")
            Result.success()
        } catch (e: Exception) {
            Log.e("RecommendationWorker", "Recommendation update failed", e)
            Result.retry()
        }
    }
    
    companion object {
        fun schedulePeriodicUpdates(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<RecommendationUpdateWorker>(
                repeatInterval = 24, // Every 24 hours
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "recommendation_updates",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
        }
    }
}
```

### 2. Analytics Worker

```kotlin
class RecommendationAnalyticsWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val interactionDao: RecommendationInteractionDao,
    private val analyticsService: ListeningAnalyticsService
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            // Analyze interaction patterns
            val interactions = interactionDao.getAllInteractions()
            
            // Update user preferences based on interactions
            val preferences = analyticsService.getUserPreferences()
            
            // Log analytics (could send to analytics service)
            Log.d("RecommendationAnalytics", 
                "Total interactions: ${interactions.size}, " +
                "Top artists: ${preferences.topArtists.take(3)}")
            
            Result.success()
        } catch (e: Exception) {
            Log.e("RecommendationAnalytics", "Analytics processing failed", e)
            Result.retry()
        }
    }
}
```

## Dependency Injection Setup

```kotlin
@Module
class RecommendationModule {
    
    @Provides
    @Singleton
    fun provideListeningAnalyticsService(
        historyDao: OnlineListeningHistoryDao
    ): ListeningAnalyticsService {
        return ListeningAnalyticsService(historyDao)
    }
    
    @Provides
    @Singleton
    fun provideRecommendationService(
        analytics: ListeningAnalyticsService,
        ytmusicApi: YTMusic,
        historyDao: OnlineListeningHistoryDao,
        cacheDao: RecommendationCacheDao
    ): RecommendationService {
        return RecommendationService(analytics, ytmusicApi, historyDao, cacheDao)
    }
    
    @Provides
    @Singleton
    fun provideRecommendationViewModel(
        recommendationService: RecommendationService,
        interactionDao: RecommendationInteractionDao
    ): RecommendationViewModel {
        return RecommendationViewModel(recommendationService, interactionDao)
    }
    
    @Provides
    @Singleton
    fun provideYTMusicApi(): YTMusic {
        // Return public instance (no auth)
        return YTMusic()
    }
}
```

## Testing Strategy

### 1. Unit Tests

```kotlin
class RecommendationServiceTest {
    
    private lateinit var service: RecommendationService
    private lateinit var mockAnalytics: ListeningAnalyticsService
    private lateinit var mockApi: YTMusic
    private lateinit var mockHistoryDao: OnlineListeningHistoryDao
    private lateinit var mockCacheDao: RecommendationCacheDao
    
    @Before
    fun setup() {
        mockAnalytics = mock()
        mockApi = mock()
        mockHistoryDao = mock()
        mockCacheDao = mock()
        
        service = RecommendationService(
            mockAnalytics, mockApi, mockHistoryDao, mockCacheDao
        )
    }
    
    @Test
    fun `generates recommendations from history`() = runTest {
        // Given
        val mockHistory = listOf(
            createMockHistory("song1", "Artist A", completionRate = 0.8f),
            createMockHistory("song2", "Artist A", completionRate = 0.9f),
            createMockHistory("song3", "Artist B", completionRate = 0.7f)
        )
        
        val mockPreferences = ListeningAnalyticsService.UserPreferencesData(
            topArtists = listOf("Artist A", "Artist B"),
            favoriteGenres = emptyList(),
            listeningPatterns = emptyMap(),
            completionRates = emptyMap(),
            skipRates = emptyMap()
        )
        
        whenever(mockHistoryDao.getRecentHistory(50)).thenReturn(mockHistory)
        whenever(mockAnalytics.getUserPreferences()).thenReturn(mockPreferences)
        whenever(mockApi.get_artist("Artist A")).thenReturn(mockArtistInfo())
        
        // When
        val result = service.generateRecommendations()
        
        // Then
        assert(result.artistBased.isNotEmpty())
        verify(mockCacheDao).saveRecommendations(any(), any(), any())
    }
}
```

### 2. Integration Tests

```kotlin
class RecommendationIntegrationTest {
    
    @Test
    fun `end-to-end recommendation flow`() = runTest {
        // Test with real database and mocked API
        val database = createInMemoryDatabase()
        val historyDao = database.onlineListeningHistoryDao()
        
        // Insert test history
        historyDao.insert(createTestHistory())
        
        // Generate recommendations
        val service = createRecommendationService(historyDao)
        val result = service.generateRecommendations()
        
        // Verify results
        assert(result.artistBased.size <= 20)
        assert(result.similarSongs.size <= 20)
    }
}
```

## Performance Considerations

### 1. Caching Strategy
- Cache recommendations for 24 hours
- Cache API responses for 1 hour
- Use memory cache for frequently accessed data

### 2. Background Processing
- Generate recommendations in background threads
- Use WorkManager for periodic updates
- Implement circuit breaker for API failures

### 3. Database Optimization
- Index frequently queried columns
- Use pagination for large result sets
- Implement data cleanup for old history

## Privacy & Ethics

### 1. Data Handling
- All processing happens locally on device
- No user data sent to external servers
- History data encrypted in database

### 2. Transparency
- Clear indication of why songs are recommended
- Option to disable recommendations
- Ability to clear recommendation history

### 3. Fairness
- Avoid algorithmic bias
- Ensure diverse recommendations
- Allow user feedback to improve suggestions

## Success Metrics

### 1. Engagement Metrics
- Click-through rate on recommendations
- Completion rate of recommended songs
- Time spent listening to recommended music

### 2. User Satisfaction
- User ratings of recommendation quality
- Feature usage analytics
- Retention impact

### 3. Technical Metrics
- API response times
- Cache hit rates
- Background job success rates

## Future Enhancements

### 1. Advanced Algorithms
- Collaborative filtering with anonymous aggregated data
- Machine learning models for better personalization
- Mood-based recommendations

### 2. Social Features
- Share recommendations with friends
- Import recommendations from other users

### 3. Offline Capabilities
- Generate recommendations without internet
- Cache recommendations for offline use

This comprehensive plan provides a production-ready recommendation system that enhances user engagement while maintaining privacy and performance standards.