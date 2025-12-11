package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil.size.Size
import com.google.common.collect.ImmutableList
import com.just_for_fun.synctax.data.local.entities.Song
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PrefetchAlbumNeighborsImg(
    current: Song?,
    queue: ImmutableList<Song>,
    radius: Int = 1
) {
    if (current == null) return
    val context = LocalContext.current
    val loader = remember { coil.ImageLoader(context) }
    val index = remember(current, queue) { queue.indexOfFirst { it.id == current.id } }
    LaunchedEffect(index, queue) {
        if (index == -1) return@LaunchedEffect
        val bounds = (maxOf(0, index - radius))..(minOf(queue.lastIndex, index + radius))
        for (i in bounds) {
            if (i == index) continue
            queue[i].albumArtUri?.let { data ->
                val req = coil.request.ImageRequest.Builder(context)
                    .data(data)
                    .memoryCacheKey("album:$data")
                    .diskCacheKey("album:$data")
                    .size(coil.size.Size.ORIGINAL)
                    .build()
                loader.enqueue(req)
            }
        }
    }
}


@Composable
fun PrefetchAlbumNeighbors(
    isActive: Boolean,
    pagerState: PagerState,
    queue: ImmutableList<Song>,
    radius: Int = 1,
    targetSize: Size = Size(600, 600)
) {
    if (!isActive || queue.isEmpty()) return
    val context = LocalContext.current
    val imageLoader = coil.Coil.imageLoader(context)

    LaunchedEffect(pagerState, queue) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val indices = (page - radius..page + radius)
                    .filter { it in queue.indices && it != page }
                indices.forEach { idx ->
                    queue[idx].albumArtUri?.let { uri ->
                        val req = coil.request.ImageRequest.Builder(context)
                            .data(uri)
                            .size(targetSize)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                            .allowHardware(true)
                            .build()
                        imageLoader.enqueue(req) // fire-and-forget
                    }
                }
            }
    }
}
