# Pagination Guide for Large Song Libraries

## Overview

Handling large song libraries (1000+ songs) from device storage requires careful pagination to prevent UI lag during fast scrolling. This guide analyzes current implementations and provides recommendations based on research from popular music apps.

## Current Implementation Analysis

### Your Current Setup
- **Page Size**: 30 songs per page
- **Home Screen**: Shows first 50 songs with "View All" button
- **Library Screen**: Uses pagination for full library
- **Database**: Room with `getSongsPaginated(limit, offset)`

### Performance Characteristics
- ✅ Good for initial load (30 songs)
- ⚠️ May cause lag with fast scrolling if pages load synchronously
- ✅ Efficient for memory usage
- ✅ Scales to 10,000+ songs

## Research from Popular Apps

### Spotube (Flutter/Dart)
**Pagination Strategy:**
- Uses `SpotubePaginationResponseObject` with `limit`, `nextOffset`, `hasMore`
- Page sizes: 10-50 items depending on content type
- Infinite scroll triggered at **80% scroll position**
- Uses `very_good_infinite_list` package
- Retry logic with fallback page sizes on failure

**Key Implementation:**
```dart
// Triggers at 80% scroll position
final nextPageTrigger = 0.8 * controller.position.maxScrollExtent;
if (controller.position.pixels >= nextPageTrigger) {
  await onTouchEdge?.call();
}
```

### OuterTune (Kotlin/Compose)
**Pagination Strategy:**
- Loads more when **3-5 items** from end are visible
- Uses `LazyColumn` with `snapshotFlow` monitoring
- Debounced loading (100ms) to prevent excessive calls
- Loading indicators with shimmer effects

**Key Implementation:**
```kotlin
LaunchedEffect(lazyListState) {
    snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
        .debounce(100L)
        .collectLatest { lastVisibleIndex ->
            if (lastVisibleIndex >= songs.size - 5) {
                viewModel.loadMoreSongs()
            }
        }
}
```

## Recommended Pagination Strategy

### 1. Adaptive Page Sizes

**Smaller initial pages for fast loading:**
```kotlin
const val INITIAL_PAGE_SIZE = 20  // First page
const val STANDARD_PAGE_SIZE = 50 // Subsequent pages
const val LARGE_PAGE_SIZE = 100   // When scrolling fast
```

**Dynamic sizing based on device performance:**
```kotlin
val pageSize = when {
    isLowEndDevice() -> 20
    isScrollingFast() -> 30
    else -> 50
}
```

### 2. Smart Loading Triggers

**Multiple trigger points:**
```kotlin
// Load more when 5 items from end are visible (normal scrolling)
val preloadTrigger = items.size - 5

// Load more when 10 items from end are visible (fast scrolling)
val fastScrollTrigger = items.size - 10

// Load more when 80% scrolled (very fast scrolling)
val emergencyTrigger = (items.size * 0.8).toInt()
```

### 3. Preloading Strategy

**Background preloading:**
```kotlin
// Preload next page when user reaches 50% of current page
val preloadTrigger = currentPageSize / 2

// Keep 2 pages in memory (current + next)
val maxCachedPages = 2
```

### 4. Fast Scroll Handling

**Detect fast scrolling:**
```kotlin
val scrollVelocity = lazyListState.layoutInfo.visibleItemsInfo
    .map { it.offset }
    .zipWithNext { a, b -> b - a }
    .average()

val isScrollingFast = abs(scrollVelocity) > FAST_SCROLL_THRESHOLD
```

**Adaptive loading during fast scroll:**
```kotlin
if (isScrollingFast) {
    // Load larger pages
    loadPage(pageSize = LARGE_PAGE_SIZE)
} else {
    // Load normal pages
    loadPage(pageSize = STANDARD_PAGE_SIZE)
}
```

## Implementation Guide

### Step 1: Update Database Layer

**Add flexible pagination:**
```kotlin
// SongDao.kt
@Query("SELECT * FROM songs ORDER BY title ASC LIMIT :limit OFFSET :offset")
fun getSongsPaginated(limit: Int, offset: Int): List<Song>

// Add count for progress indication
@Query("SELECT COUNT(*) FROM songs")
fun getTotalSongCount(): Int
```

### Step 2: Create Pagination Manager

**New file: `PaginationManager.kt`**
```kotlin
class PaginationManager(
    private val songDao: SongDao,
    private val defaultPageSize: Int = 30
) {
    private val _pages = mutableListOf<List<Song>>()
    val pages: List<List<Song>> = _pages
    
    private var _totalCount = 0
    val totalCount: Int get() = _totalCount
    
    val allSongs: List<Song>
        get() = _pages.flatten()
    
    suspend fun loadInitial(): List<Song> {
        _totalCount = songDao.getTotalSongCount()
        val firstPage = songDao.getSongsPaginated(defaultPageSize, 0)
        _pages.add(firstPage)
        return firstPage
    }
    
    suspend fun loadMore(pageSize: Int = defaultPageSize): List<Song>? {
        if (allSongs.size >= _totalCount) return null
        
        val offset = allSongs.size
        val newPage = songDao.getSongsPaginated(pageSize, offset)
        if (newPage.isNotEmpty()) {
            _pages.add(newPage)
            return newPage
        }
        return null
    }
    
    fun shouldLoadMore(visibleItemCount: Int, totalItemCount: Int): Boolean {
        return visibleItemCount >= totalItemCount - 5 // Load when 5 items from end
    }
}
```

### Step 3: Update ViewModel

**LibraryViewModel.kt updates:**
```kotlin
class LibraryViewModel(
    private val paginationManager: PaginationManager
) : ViewModel() {
    
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore
    
    init {
        loadInitial()
    }
    
    fun loadInitial() = viewModelScope.launch {
        _isLoading.value = true
        val initialSongs = paginationManager.loadInitial()
        _songs.value = initialSongs
        _hasMore.value = initialSongs.size < paginationManager.totalCount
        _isLoading.value = false
    }
    
    fun loadMore() = viewModelScope.launch {
        if (_isLoading.value || !_hasMore.value) return
        
        _isLoading.value = true
        val newSongs = paginationManager.loadMore()
        if (newSongs != null) {
            _songs.value = paginationManager.allSongs
            _hasMore.value = paginationManager.allSongs.size < paginationManager.totalCount
        } else {
            _hasMore.value = false
        }
        _isLoading.value = false
    }
    
    fun shouldLoadMore(lastVisibleIndex: Int): Boolean {
        return paginationManager.shouldLoadMore(lastVisibleIndex + 1, _songs.value.size)
    }
}
```

### Step 4: Update UI Layer

**LibraryScreen.kt updates:**
```kotlin
@Composable
fun LibraryScreen(viewModel: LibraryViewModel) {
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    
    val lazyListState = rememberLazyListState()
    
    // Load more when approaching end
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .debounce(100L)
            .collectLatest { lastIndex ->
                if (lastIndex != null && viewModel.shouldLoadMore(lastIndex)) {
                    viewModel.loadMore()
                }
            }
    }
    
    LazyColumn(state = lazyListState) {
        items(songs, key = { it.id }) { song ->
            SongItem(song = song)
        }
        
        // Loading indicator
        if (isLoading && hasMore) {
            item {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        }
    }
}
```

## Performance Optimizations

### 1. Memory Management
```kotlin
// Limit cached pages
const val MAX_CACHED_PAGES = 3

// Clear old pages when memory is low
if (pages.size > MAX_CACHED_PAGES) {
    pages.removeFirst()
}
```

### 2. Database Optimization
```kotlin
// Add indexes for faster queries
@Query("CREATE INDEX IF NOT EXISTS index_songs_title ON songs(title)")
@Query("CREATE INDEX IF NOT EXISTS index_songs_artist ON songs(artist)")
```

### 3. UI Optimizations
```kotlin
// Use derivedStateOf for expensive computations
val filteredSongs by remember(songs, searchQuery) {
    derivedStateOf {
        if (searchQuery.isBlank()) songs
        else songs.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }
}
```

## Testing Recommendations

### Performance Testing
1. **Load Test**: 100, 500, 1000, 5000 songs
2. **Scroll Test**: Fast scrolling through large lists
3. **Memory Test**: Monitor memory usage during scrolling
4. **Battery Test**: Check battery drain during heavy usage

### Edge Cases
1. **Empty Library**: Handle 0 songs gracefully
2. **Single Page**: Libraries smaller than page size
3. **Network Issues**: Handle database errors
4. **Device Rotation**: Maintain scroll position

## Configuration Options

### Page Size Configuration
```kotlin
object PaginationConfig {
    const val INITIAL_PAGE_SIZE = 20
    const val STANDARD_PAGE_SIZE = 50
    const val FAST_SCROLL_PAGE_SIZE = 100
    const val PRELOAD_TRIGGER_ITEMS = 5
    const val MAX_CACHED_PAGES = 3
}
```

### Device-Specific Tuning
```kotlin
@Composable
fun getOptimalPageSize(): Int {
    val context = LocalContext.current
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    
    return when {
        memoryInfo.lowMemory -> 20
        isTablet() -> 100
        else -> 50
    }
}
```

## Monitoring and Analytics

### Performance Metrics
```kotlin
// Track loading times
val loadStartTime = System.currentTimeMillis()
// ... load data ...
val loadTime = System.currentTimeMillis() - loadStartTime
analytics.logEvent("page_load_time", loadTime)

// Track scroll performance
val scrollMetrics = lazyListState.layoutInfo
analytics.logEvent("scroll_metrics", mapOf(
    "visible_items" to scrollMetrics.visibleItemsInfo.size,
    "total_items" to scrollMetrics.totalItemsCount
))
```

## Migration Path

### Phase 1: Core Pagination
1. Implement basic pagination (current setup)
2. Add loading states
3. Test with small libraries

### Phase 2: Performance Optimization
1. Add adaptive page sizes
2. Implement preloading
3. Add fast scroll detection

### Phase 3: Advanced Features
1. Memory management
2. Analytics integration
3. A/B testing for optimal page sizes

## Summary

**Recommended Configuration:**
- **Initial Page**: 20 songs
- **Standard Page**: 50 songs  
- **Fast Scroll Page**: 100 songs
- **Preload Trigger**: 5 items from end
- **Max Cached Pages**: 3

**Key Benefits:**
- ✅ Smooth scrolling even with 10,000+ songs
- ✅ Fast initial load times
- ✅ Memory efficient
- ✅ Handles fast scrolling gracefully
- ✅ Scales with device capabilities

**Implementation Priority:**
1. Smart loading triggers (high impact)
2. Adaptive page sizes (medium impact)  
3. Memory management (low impact)
4. Analytics (optional)

This approach balances performance with user experience, ensuring smooth scrolling regardless of library size.</content>
<parameter name="filePath">PAGINATION_GUIDE.md