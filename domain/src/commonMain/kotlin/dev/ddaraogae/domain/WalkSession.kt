@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package dev.ddaraogae.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlin.math.*
import kotlin.native.HiddenFromObjC
import kotlin.experimental.ExperimentalObjCRefinement

enum class WalkStatus { Recording, Completed, Interrupted }

@HiddenFromObjC
data class DogProfile(val userId: String, val name: String) {
    init { require(userId.isNotBlank() && name.isNotBlank()) }
}

@HiddenFromObjC
data class LocationSample(val latitude: Double, val longitude: Double, val recordedAtMillis: Long,
                          val accuracyMeters: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        require(recordedAtMillis >= 0 && accuracyMeters.isFinite() && accuracyMeters >= 0.0)
    }
}

@HiddenFromObjC
data class WalkPoint(val sequence: Int, val segment: Int, val sample: LocationSample)

@HiddenFromObjC
fun distanceMeters(a: LocationSample, b: LocationSample): Double {
    val dLat = PI * (b.latitude - a.latitude) / 180.0
    val dLon = PI * (b.longitude - a.longitude) / 180.0
    val latA = PI * a.latitude / 180.0
    val latB = PI * b.latitude / 180.0
    val half = sin(dLat / 2).let { it * it } + cos(latA) * cos(latB) * sin(dLon / 2).let { it * it }
    return 6371000.0 * 2.0 * asin(sqrt(half.coerceIn(0.0, 1.0)))
}

data class WalkSession(
    val id: String,
    val userId: String,
    val startedAtMillis: Long,
    val status: WalkStatus,
    val endedAtMillis: Long? = null,
    val dogName: String? = null,
    val distanceMeters: Double = 0.0,
    val activeDurationMillis: Long = 0,
    val lastResumedAtMillis: Long? = null,
    val segment: Int = 0,
    val segmentStartedAtMillis: Long? = null,
) {
    init {
        require(id.isNotBlank() && userId.isNotBlank())
        require(startedAtMillis >= 0)
        require(endedAtMillis == null || endedAtMillis >= startedAtMillis)
        require(distanceMeters.isFinite() && distanceMeters >= 0 && activeDurationMillis >= 0 && segment >= 0)
        require(segmentStartedAtMillis == null || segmentStartedAtMillis >= startedAtMillis)
    }

    fun elapsedMillis(nowMillis: Long): Long = activeDurationMillis +
        if (status == WalkStatus.Recording && lastResumedAtMillis != null)
            (nowMillis - lastResumedAtMillis).coerceAtLeast(0) else 0
}

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
interface WalkSessionStore {
    fun observe(userId: String): Flow<WalkSession?>
    fun observeHistory(userId: String): Flow<List<WalkSession>>
    fun observeRecord(userId: String, walkId: String): Flow<WalkSession?>
    fun observePoints(userId: String, walkId: String): Flow<List<WalkPoint>>
    fun observeDog(userId: String): Flow<DogProfile?>
    suspend fun saveDog(profile: DogProfile)
    suspend fun start(session: WalkSession)
    suspend fun finish(userId: String, walkId: String, endedAtMillis: Long)
    suspend fun pause(userId: String, walkId: String, nowMillis: Long)
    suspend fun resume(userId: String, walkId: String, nowMillis: Long)
    suspend fun checkpoint(userId: String, walkId: String, nowMillis: Long)
    suspend fun appendPoint(userId: String, walkId: String, segment: Int, sample: LocationSample)
    fun close()
}

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
class WalkSessionController(
    private val userId: String,
    private val store: WalkSessionStore,
    sessionScope: CoroutineScope,
) {
    // Leaving a screen must not stop the walk. The authenticated session owns
    // this scope; ViewModels only observe it. ADR-001 and M03 scope tests.
    val state: StateFlow<WalkSessionState> = store.observe(userId)
        .map { WalkSessionState(session = it, loaded = true) }
        .catch { emit(WalkSessionState(loaded = true, failed = true)) }
        .stateIn(sessionScope, SharingStarted.Eagerly, WalkSessionState())

    val dog: StateFlow<DogProfile?> = store.observeDog(userId)
        .stateIn(sessionScope, SharingStarted.Eagerly, null)
    val history: StateFlow<List<WalkSession>> = store.observeHistory(userId)
        .stateIn(sessionScope, SharingStarted.Eagerly, emptyList())

    fun record(walkId: String): Flow<WalkSession?> = store.observeRecord(userId, walkId)
    fun points(walkId: String): Flow<List<WalkPoint>> = store.observePoints(userId, walkId)

    suspend fun saveDog(name: String) = store.saveDog(DogProfile(userId, name.trim()))

    suspend fun start(walkId: String, nowMillis: Long, dogName: String? = null) {
        check(state.value.loaded && !state.value.failed)
        store.start(WalkSession(walkId, userId, nowMillis, WalkStatus.Recording,
            dogName = dogName, lastResumedAtMillis = nowMillis, segmentStartedAtMillis = nowMillis))
    }

    suspend fun finish(walkId: String, nowMillis: Long) {
        store.finish(userId, walkId, nowMillis)
    }

    suspend fun pause(walkId: String, nowMillis: Long) = store.pause(userId, walkId, nowMillis)
    suspend fun resume(walkId: String, nowMillis: Long) = store.resume(userId, walkId, nowMillis)
    suspend fun checkpoint(walkId: String, nowMillis: Long) = store.checkpoint(userId, walkId, nowMillis)
    suspend fun appendPoint(walkId: String, segment: Int, sample: LocationSample) =
        store.appendPoint(userId, walkId, segment, sample)
}

data class WalkSessionState(val session: WalkSession? = null, val loaded: Boolean = false, val failed: Boolean = false)
