package com.just_for_fun.youtubemusic.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "listening_history",
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ListeningHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val playTimestamp: Long,
    val listenDuration: Long, // How long user listened in milliseconds
    val completionRate: Float, // 0.0 to 1.0
    val skipped: Boolean,
    val timeOfDay: Int, // Hour of day (0-23)
    val dayOfWeek: Int, // 1-7
    val repeatCount: Int = 0
)