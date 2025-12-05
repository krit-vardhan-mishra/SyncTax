package com.just_for_fun.synctax.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "templateShortcuts")
data class TemplateShortcut(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    val content: String
)
