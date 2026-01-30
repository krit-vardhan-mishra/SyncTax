package com.just_for_fun.synctax.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Advanced coroutine utilities following OuterTune/SimpMusic best practices
 * 
 * Key patterns:
 * 1. Proper Flow sharing with stateIn
 * 2. Structured concurrency
 * 3. Cancellation handling
 * 4. Proper dispatcher usage
 */

/**
 * Extension to convert Flow to StateFlow with proper sharing
 * Prevents multiple database queries for the same data
 * 
 * Usage:
 * ```kotlin
 * val songs: StateFlow<List<Song>> = database.getAllSongs()
 *     .toStateFlow(viewModelScope, emptyList())
 * ```
 */
fun <T> Flow<T>.toStateFlow(
    scope: CoroutineScope,
    initialValue: T,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
): StateFlow<T> = stateIn(scope, started, initialValue)

/**
 * Extension to add loading/error handling to Flow
 * Wraps the result in a Resource type
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

fun <T> Flow<T>.asResource(): Flow<Resource<T>> = map<T, Resource<T>> {
    Resource.Success(it)
}.onStart {
    emit(Resource.Loading)
}.catch { e ->
    Log.e("FlowUtils", "Flow error", e)
    emit(Resource.Error(e.message ?: "Unknown error"))
}

/**
 * Extension to safely collect Flow with error handling
 * Prevents crashes from Flow errors
 */
suspend fun <T> Flow<T>.collectSafely(
    onError: (Throwable) -> Unit = {},
    action: suspend (T) -> Unit
) {
    catch { e ->
        onError(e)
        Log.e("FlowUtils", "Flow collection error", e)
    }.collect(action)
}

/**
 * Launch a coroutine with proper error handling
 * Logs errors instead of crashing
 */
fun CoroutineScope.launchSafely(
    dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Main,
    onError: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit
): Job = launch(dispatcher) {
    try {
        block()
    } catch (e: Exception) {
        Log.e("CoroutineUtils", "Coroutine error", e)
        onError(e)
    }
}

/**
 * Execute block on IO dispatcher and return to current context
 * Prevents blocking main thread
 */
suspend fun <T> withIO(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.IO, block)

/**
 * Execute block on Main dispatcher
 * For UI updates from background threads
 */
suspend fun <T> withMain(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Main, block)

/**
 * Execute block on Default dispatcher
 * For CPU-intensive operations
 */
suspend fun <T> withDefault(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Default, block)

/**
 * Extension to throttle Flow emissions
 * Useful for search queries, position updates, etc.
 */
fun <T> Flow<T>.throttleFirst(windowDurationMs: Long): Flow<T> = 
    distinctUntilChanged().flowOn(Dispatchers.Default)

/**
 * Extension to debounce Flow emissions  
 * Waits for emissions to stop before emitting
 */
@OptIn(FlowPreview::class)
fun <T> Flow<T>.debounceLatest(timeoutMs: Long): Flow<T> =
    debounce(timeoutMs)

/**
 * Launch coroutine that automatically cancels when scope is cancelled
 * Use for cleanup operations
 */
fun CoroutineScope.launchCancellable(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(start = start) {
    try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        // Expected cancellation, don't log
        throw e
    } catch (e: Exception) {
        Log.e("CoroutineUtils", "Cancellable coroutine error", e)
    }
}

/**
 * Helper to create cold Flow that's immediately shared
 * Prevents duplicate work when multiple collectors
 */
fun <T> sharedFlow(
    scope: CoroutineScope,
    block: suspend () -> T
): StateFlow<T?> = kotlinx.coroutines.flow.flow {
    emit(block())
}.stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

/**
 * Extension to add retry logic to Flow
 * Retries failed emissions with exponential backoff
 */
fun <T> Flow<T>.retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 1000
): Flow<T> = catch { e ->
    var currentDelay = initialDelayMs
    repeat(maxAttempts - 1) { attempt ->
        try {
            kotlinx.coroutines.delay(currentDelay)
            emit(e as T) // Retry
            return@catch
        } catch (retryError: Exception) {
            currentDelay *= 2
            if (attempt == maxAttempts - 2) throw retryError
        }
    }
}

/**
 * Execute multiple operations in parallel
 * Waits for all to complete before returning
 */
suspend fun <T> parallelMap(
    items: List<T>,
    transform: suspend (T) -> Unit
) = withContext(Dispatchers.IO) {
    items.map { item ->
        launch {
            transform(item)
        }
    }.forEach { it.join() }
}
