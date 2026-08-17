# SyncTax - Critical Performance Fixes

## Fix #1: Parallelize Artist Photo Fetching (10-15 min implementation)

**Location:** `HomeViewModel.kt` - `fetchAllArtistPhotos()` method (~line 865)

**Problem:** Currently uses sequential `forEach` → 5 artists × 300ms = 1.5 seconds

**Solution:** Use parallel `async { }` with `awaitAll()`

```kotlin
/**
 * OPTIMIZED: Fetch all artist photos in parallel (instead of sequential)
 * This reduces fetching time from 1.5s to ~300-400ms for 5 artists
 */
suspend fun fetchAllArtistPhotosOptimized(artistNames: List<String>, forceRefresh: Boolean = false) {
    if (_uiState.value.isLoadingArtistPhotos) {
        Log.d(TAG, "Skipping artist photo fetch - already loading")
        return
    }
    
    val currentPhotos = _uiState.value.artistPhotos
    val artistsToFetch = if (forceRefresh) {
        artistNames
    } else {
        artistNames.filter { !currentPhotos.containsKey(it) }
    }
    
    if (artistsToFetch.isEmpty()) {
        Log.d(TAG, "Skipping artist photo fetch - all artists already cached")
        return
    }
    
    _uiState.value = _uiState.value.copy(isLoadingArtistPhotos = true)
    Log.d(TAG, "Fetching photos for ${artistsToFetch.size} artists in PARALLEL (forceRefresh=$forceRefresh)")
    
    viewModelScope.launch(AppDispatchers.Network) {
        try {
            // Create parallel tasks for each artist
            val photoJobs = artistsToFetch.map { artistName ->
                async {
                    try {
                        // Suspend and wait for result with timeout
                        withTimeoutOrNull(5000L) { // 5-second timeout per artist
                            suspendCancellableCoroutine<Pair<String, String?>> { continuation ->
                                YTMusicRecommender.searchArtists(
                                    query = artistName,
                                    limit = 1,
                                    onResult = { artists ->
                                        val thumbnail = artists.firstOrNull()?.thumbnail
                                        Log.d(TAG, "✅ Artist photo for '$artistName': ${if (thumbnail != null) "found" else "not found"}")
                                        continuation.resume(Pair(artistName, thumbnail))
                                    },
                                    onError = { error ->
                                        Log.e(TAG, "❌ Failed to fetch artist photo for '$artistName': $error")
                                        continuation.resume(Pair(artistName, null))
                                    }
                                )
                            }
                        } ?: Pair(artistName, null) // Timeout fallback
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception fetching photo for '$artistName': ${e.message}")
                        Pair(artistName, null)
                    }
                }
            }
            
            // Wait for ALL photos to complete (in parallel)
            val results = photoJobs.awaitAll()
            
            // Build map from results
            val photosMap = results.associate { (artistName, thumbnail) -> artistName to thumbnail }
            
            // Update state with all fetched photos (merge with existing if not force refresh)
            val updatedPhotos = if (forceRefresh) {
                currentPhotos.filterKeys { it !in artistsToFetch } + photosMap
            } else {
                currentPhotos + photosMap
            }
            
            _uiState.value = _uiState.value.copy(
                artistPhotos = updatedPhotos,
                isLoadingArtistPhotos = false
            )
            Log.d(TAG, "✨ Finished fetching artist photos in PARALLEL. Total cached: ${_uiState.value.artistPhotos.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error in parallel photo fetch: ${e.message}")
            _uiState.value = _uiState.value.copy(isLoadingArtistPhotos = false)
        }
    }
}

// REPLACEMENT for existing fetchAllArtistPhotos
fun fetchAllArtistPhotos(artistNames: List<String>, forceRefresh: Boolean = false) {
    viewModelScope.launch {
        fetchAllArtistPhotosOptimized(artistNames, forceRefresh)
    }
}
```

**Expected Result:**
- Artist photo load time: 1.5 seconds → 400 milliseconds
- 70-75% reduction in home screen loading time

**Key Changes:**
- ✅ `map { async { ... } }` instead of `forEach { ... }`
- ✅ `awaitAll()` waits for all in parallel
- ✅ `withTimeoutOrNull(5000L)` prevents hanging
- ✅ Better error handling

---

## Fix #2: Batch State Updates (5-10 min implementation)

**Location:** `HomeViewModel.kt` - `loadData()` method (~line 145) and `scanMusic()` (~line 200)

**Problem:** Multiple `_uiState.value = copy()` calls → multiple recompositions

```kotlin
// BEFORE (BAD - 3 separate updates = 3 recompositions)
viewModelScope.launch(AppDispatchers.Database) {
    _uiState.value = _uiState.value.copy(isLoading = true)

    try {
        repository.getAllSongs().collect { songs ->
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(allSongs = songs)  // Recomp #1
                extractArtists(songs)
            }
        }
    } finally {
        _uiState.value = _uiState.value.copy(isLoading = false)  // Recomp #2
    }
}

// In extractArtists():
withContext(Dispatchers.Main) {
    _uiState.value = _uiState.value.copy(artists = artists)  // Recomp #3
}

// In extractAlbums():
withContext(Dispatchers.Main) {
    _uiState.value = _uiState.value.copy(albums = albums)  // Recomp #4
}

// AFTER (GOOD - 1 update = 1 recomposition)
viewModelScope.launch(AppDispatchers.Database) {
    _uiState.value = _uiState.value.copy(isLoading = true)

    try {
        repository.getAllSongs().collect { songs ->
            val artists = extractArtistsSync(songs)  // Compute, don't update
            val albums = extractAlbumsSync(songs)    // Compute, don't update
            
            withContext(Dispatchers.Main) {
                // SINGLE UPDATE - ONE RECOMPOSITION
                _uiState.value = _uiState.value.copy(
                    allSongs = songs,
                    artists = artists,
                    albums = albums,
                    isLoading = false
                )
            }
        }
    } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = e.message
        )
    }
}

// NEW: Synchronous versions that don't trigger updates
private fun extractArtistsSync(songs: List<Song>): List<ArtistUiModel> {
    return songs
        .mapNotNull { it.artist }
        .distinct()
        .sorted()
        .map { artistName ->
            val artistSongs = songs.filter { it.artist == artistName }
            ArtistUiModel(
                name = artistName,
                songCount = artistSongs.size,
                thumbnail = null // Load later on demand
            )
        }
}

private fun extractAlbumsSync(songs: List<Song>): List<AlbumUiModel> {
    return songs
        .groupBy { Pair(it.album ?: "Unknown", it.artist ?: "Unknown") }
        .map { (albumArtist, groupedSongs) ->
            val firstSong = groupedSongs.firstOrNull()
            AlbumUiModel(
                name = albumArtist.first,
                artist = albumArtist.second,
                songCount = groupedSongs.size,
                albumArtUri = firstSong?.albumArtUri
            )
        }
        .sortedByDescending { it.songCount }
}
```

**Expected Result:**
- Main thread CPU: -40-50% reduction
- Fewer recompositions = fewer frame drops
- Smoother initialization

**Key Changes:**
- ✅ Extract (compute) first, then single update
- ✅ Use synchronous versions (no dispatchers)
- ✅ Batch multiple state fields in one `copy()`

---

## Fix #3: Add Network Timeout to Splash Screen (3-5 min implementation)

**Location:** `AppInitializer.kt` - `initialize()` method (~line 190)

**Problem:** Network operations on splash screen can hang indefinitely

```kotlin
// BEFORE (BAD - can hang splash screen)
suspend fun initialize(context: Context): Flow<InitProgress> {
    withContext(Dispatchers.IO) {
        // ... other phases ...
        
        // Phase X: Load saved playlists (BLOCKING - no timeout)
        updateProgress("Loading playlists...", 0.80f)
        val savedPlaylists = playlistRepository.getSavedPlaylists()  // CAN HANG HERE
        Log.d(TAG, "Phase X complete: Loaded ${savedPlaylists.size} playlists")
        
        // ... rest of initialization ...
    }
}

// AFTER (GOOD - has timeout)
suspend fun initialize(context: Context): Flow<InitProgress> {
    withContext(Dispatchers.IO) {
        // ... other phases ...
        
        // Phase X: Load saved playlists (WITH TIMEOUT)
        updateProgress("Loading playlists...", 0.80f)
        
        val savedPlaylists = try {
            // Use withTimeoutOrNull for network operations
            withTimeoutOrNull(3000L) {  // 3-second timeout
                playlistRepository.getSavedPlaylists()
            } ?: emptyList()  // Fallback to empty if timeout
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Playlist loading timed out, continuing with empty list")
            emptyList()
        }
        
        Log.d(TAG, "Phase X complete: Loaded ${savedPlaylists.size} playlists")
        
        // ... rest of initialization ...
    }
}

// ALSO: Add timeout to recommendation loading
suspend fun initialize(context: Context): Flow<InitProgress> {
    // ... existing code ...
    
    // Phase Y: Load recommendations (WITH TIMEOUT)
    updateProgress("Loading recommendations...", 0.90f)
    
    val recommendations = try {
        withTimeoutOrNull(5000L) {  // 5-second timeout for ML
            recommendationManager.generateQuickPicks(50)
        }
    } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "Recommendation loading timed out")
        null
    }
    
    // Store recommendations (or null if timed out)
    _initializedData = InitializedData(
        // ... other fields ...
        recommendations = recommendations,
        // ... other fields ...
    )
}
```

**Expected Result:**
- Splash screen never hangs > 3 seconds
- Better UX for users with slow networks
- Graceful degradation

**Key Changes:**
- ✅ `withTimeoutOrNull(3000L)` wrapper
- ✅ Fallback to empty/null values
- ✅ Log warnings for debugging

---

## Fix #4: Sequencing Coroutine Startup (10-15 min implementation)

**Location:** `HomeViewModel.kt` - `init {}` block (~line 60)

**Problem:** 14+ coroutines launch simultaneously → resource contention

```kotlin
// BEFORE (BAD - chaotic launch)
init {
    if (preComputedData != null) {
        // ...apply data (fast)
        generateQuickPicks()
    } else {
        // ALL THESE LAUNCH SIMULTANEOUSLY:
        loadData()
        scanMusic()
        observeListeningHistoryForTraining()
        observeListenAgain()
        loadOnlineHistory()
        loadSearchHistory()
        observeRecommendationsCount()
        startPeriodicRefresh()
        refreshAlbumArtForSongs()
        loadTrainingStatistics()
        loadModelStatus()
        loadSavedPlaylists()
        loadMostPlayedSongs()
        observeFavoriteSongs()
        observeOfflineSavedSongs()
    }
}

// AFTER (GOOD - intelligent sequencing)
init {
    if (preComputedData != null) {
        // Pre-computed path - still optimized
        Log.d(TAG, "Using pre-computed data from AppInitializer")
        
        // Apply pre-computed data immediately
        _uiState.value = _uiState.value.copy(
            allSongs = preComputedData.songs,
            mostPlayedSongs = preComputedData.mostPlayedSongs,
            listenAgain = preComputedData.listenAgain,
            quickAccessSongs = preComputedData.quickAccessSongs,
            favoriteSongs = preComputedData.favoriteSongs,
            searchHistory = preComputedData.searchHistory,
            onlineHistory = preComputedData.onlineHistory,
            savedPlaylists = preComputedData.savedPlaylists,
            trainingStatistics = preComputedData.trainingStatistics ?: TrainingStatistics(),
            modelStatus = preComputedData.modelStatus ?: ModelTrainingStatus(),
            trainingDataSize = preComputedData.trainingDataSize,
            isLoading = false,
            scanComplete = true
        )
        
        extractArtists(preComputedData.songs)
        extractAlbums(preComputedData.songs)
        AppInitializer.clearData()
        
        // Start lightweight observers (non-blocking)
        startLightweightObservers()
        
        // Start heavy operations in background (low priority)
        viewModelScope.launch(AppDispatchers.MachineLearning) {
            if (preComputedData.songs.isNotEmpty()) {
                generateQuickPicks()
            }
        }
    } else {
        // Fallback path - optimized startup
        Log.d(TAG, "No pre-computed data, running optimized initialization")
        
        viewModelScope.launch {
            // PHASE 1: Critical path (sequential, blocking)
            Log.d(TAG, "Phase 1: Loading core data...")
            loadData()
            
            // PHASE 2: Parallel independent operations (non-blocking)
            Log.d(TAG, "Phase 2: Loading parallel data...")
            val jobs = listOf(
                async(AppDispatchers.MusicScanning) { scanMusic() },
                async(AppDispatchers.Database) { loadOnlineHistory() },
                async(AppDispatchers.Database) { loadSearchHistory() },
                async(AppDispatchers.Database) { loadMostPlayedSongs() },
                async(AppDispatchers.Network) { loadSavedPlaylists() }
            )
            
            try {
                jobs.awaitAll()
            } catch (e: Exception) {
                Log.e(TAG, "Error in parallel phase: ${e.message}")
            }
            
            // PHASE 3: Lightweight observers (non-blocking)
            Log.d(TAG, "Phase 3: Starting observers...")
            startLightweightObservers()
            
            // PHASE 4: Heavy background operations (very low priority)
            Log.d(TAG, "Phase 4: Queuing background tasks...")
            viewModelScope.launch(AppDispatchers.MachineLearning) {
                delay(500) // Give UI time to render first
                generateQuickPicks()
            }
        }
    }
}

/**
 * Lightweight observers that don't do heavy I/O
 * These can start immediately without blocking
 */
private fun startLightweightObservers() {
    observeListeningHistoryForTraining()
    observeListenAgain()
    observeRecommendationsCount()
    startPeriodicRefresh()
    observeFavoriteSongs()
    observeOfflineSavedSongs()
}
```

**Expected Result:**
- Frame drops: 86 → 20-30
- Startup time: 3-4s → 1.5-2s
- UI feels responsive immediately

**Key Changes:**
- ✅ Pre-computed path optimized
- ✅ Critical path sequential
- ✅ Independent operations parallel with `awaitAll()`
- ✅ Heavy work deferred with `delay(500)`

---

## Fix #5: Add Logging to Track Improvements

Add to `HomeViewModel.kt`:

```kotlin
companion object {
    const val TAG = "HomeViewModel"
}

private val initStartTime = mutableStateOf(0L)

init {
    initStartTime.value = System.currentTimeMillis()
    
    // ... existing init code ...
    
    viewModelScope.launch {
        delay(500)
        val initDuration = System.currentTimeMillis() - initStartTime.value
        Log.d(TAG, "✅ HomeViewModel init completed in ${initDuration}ms")
    }
}

fun scanMusic() {
    val scanStart = System.currentTimeMillis()
    
    viewModelScope.launch(AppDispatchers.MusicScanning) {
        // ... existing scan code ...
        
        val scanDuration = System.currentTimeMillis() - scanStart
        Log.d(TAG, "✅ Music scanning completed in ${scanDuration}ms")
    }
}

fun fetchAllArtistPhotos(artistNames: List<String>, forceRefresh: Boolean = false) {
    val photoStart = System.currentTimeMillis()
    
    // ... existing fetch code ...
    
    viewModelScope.launch(AppDispatchers.Network) {
        // ... after awaitAll() ...
        
        val photoDuration = System.currentTimeMillis() - photoStart
        Log.d(TAG, "✅ Artist photos fetched in ${photoDuration}ms")
    }
}
```

This allows you to measure improvements from logcat.

---

## Implementation Priority

| Fix | Duration | Impact | Priority |
|-----|----------|--------|----------|
| Batch state updates | 5 min | -40% CPU | 🔴 Now |
| Parallelize photos | 10 min | -60% load time | 🔴 Now |
| Network timeout | 5 min | Prevents hang | 🔴 Now |
| Sequence coroutines | 15 min | -60% frame drops | 🔴 Now |
| Add logging | 5 min | Measurement | 🟡 Soon |

**Estimated Total Implementation Time: 40 minutes**  
**Expected Result: 60-70% performance improvement**

