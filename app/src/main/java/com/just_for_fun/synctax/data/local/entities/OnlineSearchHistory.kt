package com.just_for_fun.synctax.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store online search history
 * This will be displayed in the Search screen when the search field is focused
 */
@Entity(tableName = "online_search_history")
data class OnlineSearchHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
