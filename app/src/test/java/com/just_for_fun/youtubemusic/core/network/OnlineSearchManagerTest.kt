package com.just_for_fun.youtubemusic.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Test

class OnlineSearchManagerTest {

    @Test
    fun searchSunflower_returnsResults() {
        runBlocking {
            val manager = OnlineSearchManager("https://piped.video")
            val query = "Sunflower Post Malone"
            val results = manager.search(query)

            println("Found ${results.size} results for '$query'")
            results.forEach { println(it.title) }

            // Assert we found at least one result containing 'Sunflower' or 'Post Malone'
            assert(results.isNotEmpty())
            val ok = results.any { r -> r.title.contains("Sunflower", true) || r.title.contains("Post Malone", true) }
            assert(ok) { "Expected a result to contain 'Sunflower' or 'Post Malone'" }
        }
    }
}
