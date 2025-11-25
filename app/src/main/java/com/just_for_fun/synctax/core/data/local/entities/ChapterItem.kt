package com.just_for_fun.synctax.core.data.local.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChapterItem(
    @SerializedName(value = "start_time")
    var start_time: Long,
    @SerializedName(value = "end_time")
    var end_time: Long,
    @SerializedName(value = "title")
    var title: String,
) : Parcelable