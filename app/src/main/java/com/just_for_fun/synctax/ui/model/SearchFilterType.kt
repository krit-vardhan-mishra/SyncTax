package com.just_for_fun.synctax.ui.model

/**
 * Filter types for search results
 */
enum class SearchFilterType {
    ALL,      // Show all results (default)
    SONGS,    // Show only songs
    ALBUMS,   // Show only albums
    ARTISTS,  // Show only artists
    VIDEOS    // Show only videos (if supported in future)
}
