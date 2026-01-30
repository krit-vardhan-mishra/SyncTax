package com.just_for_fun.synctax.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a GitHub release
 * Used for both app updates and library updates
 */
data class GithubRelease(
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("body")
    val body: String = "",
    @SerializedName("published_at")
    val publishedAt: String = "",
    @SerializedName("assets")
    val assets: List<GithubReleaseAsset> = emptyList(),
    @SerializedName("prerelease")
    val prerelease: Boolean = false,
    @SerializedName("draft")
    val draft: Boolean = false
)

/**
 * Data class representing a GitHub release asset (downloadable file)
 */
data class GithubReleaseAsset(
    @SerializedName("name")
    val name: String,
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String,
    @SerializedName("size")
    val size: Long = 0,
    @SerializedName("content_type")
    val contentType: String = ""
)
