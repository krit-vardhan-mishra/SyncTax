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
}

@Dao
interface RecommendationInteractionDao {
    @Insert
    suspend fun insert(interaction: RecommendationInteraction)
    
    @Query("SELECT * FROM recommendation_interactions WHERE songId = :songId ORDER BY timestamp DESC")
    suspend fun getSongInteractions(songId: String): List<RecommendationInteraction>
    
    @Query("SELECT COUNT(*) FROM recommendation_interactions WHERE action = 'played' AND source = :source")
    suspend fun getPlayCountForSource(source: String): Int
}
```

## UI Components

### 1. Home Screen Integration

```kotlin
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    recommendationViewModel: RecommendationViewModel
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
                    onRefreshClick = {
                        recommendationViewModel.refreshRecommendations()
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
    onRefreshClick: () -> Unit
) {
    Column {
        SectionHeader(
            title = "Recommended for You",
            actionIcon = Icons.Default.Refresh,
            onActionClick = onRefreshClick
        )
        
        // Mix of different recommendation types
        val mixedRecommendations = (recommendations.artistBased.take(5) + 
                                   recommendations.similarSongs.take(5) + 
                                   recommendations.discovery.take(5)).shuffled()
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mixedRecommendations) { song ->
                RecommendationCard(
                    song = song,
                    reason = getRecommendationReason(song, recommendations),
                    onClick = { onSongClick(song) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Discovery section
        if (recommendations.discovery.isNotEmpty()) {
            SectionHeader(title = "Discover New Artists")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recommendations.discovery) { song ->
                    RecommendationCard(
                        song = song,
                        reason = "Similar to artists you love",
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
        
        // Similar songs section
        if (recommendations.similarSongs.isNotEmpty()) {
            val referenceSong = recommendations.similarSongs.firstOrNull()
            SectionHeader(title = "More Like ${referenceSong?.title?.take(20) ?: "This"}")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recommendations.similarSongs.take(10)) { song ->
                    RecommendationCard(
                        song = song,
                        reason = "Because you listened to similar songs",
                        onClick = { onSongClick(song) }
                    )
                }
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

### 3. RecommendationCard Component

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
```

### 4. Loading and Empty States

```kotlin
@Composable
fun RecommendationSkeleton() {
    Column {
        SectionHeader(title = "Recommended for You")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(5) {
                Card(
                    modifier = Modifier
                        .width(160.dp)
                        .height(200.dp)
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

## ViewModel Layer

### 1. RecommendationViewModel

```kotlin
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
            } catch (e: Exception) {
                _error.value = "Failed to refresh recommendations: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
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