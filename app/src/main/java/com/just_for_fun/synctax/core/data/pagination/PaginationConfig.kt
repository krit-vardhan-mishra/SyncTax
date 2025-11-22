package com.just_for_fun.synctax.core.data.pagination

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration

/**
 * Configuration for pagination behavior across the app
 * Adapts page sizes based on device capabilities
 * 
 * **Complexity Analysis:**
 * All methods are O(1) time and space complexity
 */
object PaginationConfig {
    // Default page sizes (based on research from Spotube and OuterTune)
    const val INITIAL_PAGE_SIZE = 20
    const val STANDARD_PAGE_SIZE = 50
    const val FAST_SCROLL_PAGE_SIZE = 100
    const val PRELOAD_TRIGGER_ITEMS = 5
    const val MAX_CACHED_PAGES = 3
    
    // Scroll detection thresholds
    const val FAST_SCROLL_THRESHOLD = 1000f // pixels per second
    const val PRELOAD_PERCENTAGE = 0.8f // Load more at 80% scroll
    
    // Debounce intervals
    const val SCROLL_DEBOUNCE_MS = 50L
    const val LOAD_DEBOUNCE_MS = 100L
    
    /**
     * Get optimal page size based on device characteristics
     * 
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     * 
     * @param context Application context
     * @return Recommended page size
     */
    fun getOptimalPageSize(context: Context): Int {
        return when {
            isLowMemoryDevice(context) -> 20
            isTablet(context) -> 100
            else -> STANDARD_PAGE_SIZE
        }
    }
    
    /**
     * Check if device is low memory
     * 
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     * 
     * @param context Application context
     * @return true if device has low memory
     */
    fun isLowMemoryDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        
        // Consider device low memory if < 2GB total RAM
        return memoryInfo.totalMem < 2L * 1024 * 1024 * 1024
    }
    
    /**
     * Check if device is a tablet
     * 
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     * 
     * @param context Application context
     * @return true if device is a tablet
     */
    fun isTablet(context: Context): Boolean {
        val configuration = context.resources.configuration
        val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        
        return screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE ||
               screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE
    }
    
    /**
     * Calculate optimal preload trigger based on library size
     * 
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     * 
     * @param totalSongs Total number of songs
     * @return Number of items before end to trigger preload
     */
    fun getPreloadTrigger(totalSongs: Int): Int {
        return when {
            totalSongs < 100 -> 3 // Small library, aggressive preload
            totalSongs < 1000 -> 5 // Medium library, standard preload
            else -> 10 // Large library, more aggressive preload
        }
    }
    
    /**
     * Get page size based on scroll velocity
     * 
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     * 
     * @param scrollVelocity Current scroll velocity
     * @param isLowMemory Whether device has low memory
     * @return Recommended page size
     */
    fun getPageSizeForVelocity(scrollVelocity: Float, isLowMemory: Boolean = false): Int {
        if (isLowMemory) return INITIAL_PAGE_SIZE
        
        return when {
            kotlin.math.abs(scrollVelocity) > FAST_SCROLL_THRESHOLD * 2 -> FAST_SCROLL_PAGE_SIZE
            kotlin.math.abs(scrollVelocity) > FAST_SCROLL_THRESHOLD -> STANDARD_PAGE_SIZE
            else -> INITIAL_PAGE_SIZE
        }
    }
}

/**
 * Device-specific pagination strategy
 * Provides recommendation for optimal pagination approach
 */
data class PaginationStrategy(
    val initialPageSize: Int,
    val standardPageSize: Int,
    val fastScrollPageSize: Int,
    val preloadTrigger: Int,
    val maxCachedPages: Int,
    val reason: String
) {
    companion object {
        /**
         * Generate optimal pagination strategy for device
         * 
         * Time Complexity: O(1)
         * Space Complexity: O(1)
         * 
         * @param context Application context
         * @param totalSongs Total songs in library
         * @return Recommended pagination strategy
         */
        fun forDevice(context: Context, totalSongs: Int = 0): PaginationStrategy {
            val isLowMemory = PaginationConfig.isLowMemoryDevice(context)
            val isTablet = PaginationConfig.isTablet(context)
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            val availableMemoryMB = memoryInfo.availMem / 1024 / 1024
            
            return when {
                isLowMemory -> PaginationStrategy(
                    initialPageSize = 10,
                    standardPageSize = 20,
                    fastScrollPageSize = 30,
                    preloadTrigger = 3,
                    maxCachedPages = 2,
                    reason = "Low memory device (${availableMemoryMB}MB available)"
                )
                isTablet -> PaginationStrategy(
                    initialPageSize = 30,
                    standardPageSize = 100,
                    fastScrollPageSize = 200,
                    preloadTrigger = 10,
                    maxCachedPages = 5,
                    reason = "Tablet device with large screen"
                )
                totalSongs > 5000 -> PaginationStrategy(
                    initialPageSize = 20,
                    standardPageSize = 50,
                    fastScrollPageSize = 100,
                    preloadTrigger = 10,
                    maxCachedPages = 3,
                    reason = "Large library (${totalSongs} songs)"
                )
                else -> PaginationStrategy(
                    initialPageSize = PaginationConfig.INITIAL_PAGE_SIZE,
                    standardPageSize = PaginationConfig.STANDARD_PAGE_SIZE,
                    fastScrollPageSize = PaginationConfig.FAST_SCROLL_PAGE_SIZE,
                    preloadTrigger = PaginationConfig.PRELOAD_TRIGGER_ITEMS,
                    maxCachedPages = PaginationConfig.MAX_CACHED_PAGES,
                    reason = "Standard device with ${totalSongs} songs"
                )
            }
        }
    }
}

/**
 * Complexity Summary for PaginationConfig:
 * 
 * **All Operations: O(1)**
 * - getOptimalPageSize(): O(1) - Simple device checks
 * - isLowMemoryDevice(): O(1) - System call
 * - isTablet(): O(1) - Configuration check
 * - getPreloadTrigger(): O(1) - Conditional logic
 * - getPageSizeForVelocity(): O(1) - Threshold comparison
 * - PaginationStrategy.forDevice(): O(1) - Device analysis
 * 
 * **Memory: O(1)**
 * - All methods use constant memory
 * - No dynamic allocations based on input size
 * 
 * **Benefits:**
 * 1. Adapts to device capabilities automatically
 * 2. Prevents memory issues on low-end devices
 * 3. Maximizes performance on high-end devices
 * 4. Scales with library size
 * 5. Zero runtime overhead (all O(1) operations)
 */
