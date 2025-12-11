package com.just_for_fun.synctax.presentation.components.utils

enum class SortOption(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST_ASC("Artist (A-Z)"),
    ARTIST_DESC("Artist (Z-A)"),
    RELEASE_YEAR_DESC("Newest First"),
    RELEASE_YEAR_ASC("Oldest First"),
    ADDED_TIMESTAMP_DESC("Recently Added"),
    ADDED_TIMESTAMP_ASC("Added First"),
    DURATION_DESC("Longest First"),
    DURATION_ASC("Shortest First"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    ARTIST("Artist"),
    DATE_ADDED_OLDEST("Date Added (Oldest)"),
    DATE_ADDED_NEWEST("Date Added (Newest)"),
    CUSTOM("Custom")
}
