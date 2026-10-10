package dev.ddaraogae.shared

import dev.ddaraogae.data.*
import dev.ddaraogae.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.time.Clock
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class RuntimeTest {
    @Test fun userScopeSharesControllerAndCloseReleasesResources() = runTest {
        val store = MemoryStore()
        var listenerCancelled = 0
        val backend = object : NativeBackend {
            override fun observeUser(observer: BackendUserObserver): CancelHandle {
                observer.onUser("user-a")
                return object : CancelHandle { override fun cancel() { listenerCancelled++ } }
            }
        }
        val runtime = AppRuntime(store, "user-a", backend, StandardTestDispatcher(testScheduler))
        runCurrent()
        val firstController = runtime.userScope.get<WalkSessionController>()
        store.start(WalkSession("walk", "user-a", 100, WalkStatus.Recording))
        runCurrent()
        val secondController = runtime.userScope.get<WalkSessionController>()
        assertSame(firstController, secondController)
        assertEquals("walk", secondController.state.value.session?.id)
        runtime.close()
        runCurrent()
        runtime.close()
        assertEquals(1, listenerCancelled)
        assertEquals(1, store.closed)
        assertFails { runtime.userScope.get<WalkSessionController>() }
        store.start(WalkSession("late", "user-a", 101, WalkStatus.Recording))
        runCurrent()
        assertEquals("walk", firstController.state.value.session?.id)
    }

    @Test fun independentUsersDoNotShareKoinState() = runTest {
        val a = MemoryStore()
        val b = MemoryStore()
        val backend = object : NativeBackend {
            override fun observeUser(observer: BackendUserObserver) = object : CancelHandle { override fun cancel() {} }
        }
        val dispatcher = StandardTestDispatcher(testScheduler)
        val ra = AppRuntime(a, "a", backend, dispatcher)
        val rb = AppRuntime(b, "b", backend, dispatcher)
        a.start(WalkSession("private", "a", 100, WalkStatus.Recording))
        runCurrent()
        assertEquals("private", ra.controller.state.value.session?.id)
        assertNull(rb.controller.state.value.session)
        ra.close(); rb.close(); runCurrent()
    }

    @Test fun startupRecoveryCannotPauseAWalkStartedAfterRuntimeCreation() = runTest {
        val store = MemoryStore()
        val backend = object : NativeBackend {
            override fun observeUser(observer: BackendUserObserver) = object : CancelHandle { override fun cancel() {} }
        }
        val runtime = AppRuntime(store, "a", backend, StandardTestDispatcher(testScheduler))
        store.start(WalkSession("new", "a", Clock.System.now().toEpochMilliseconds() + 1_000,
            WalkStatus.Recording))
        runCurrent()
        assertEquals(WalkStatus.Recording, runtime.walkState.value.session?.status)
        runtime.close()

        val old = MemoryStore()
        old.start(WalkSession("old", "a", 100, WalkStatus.Recording))
        val reopened = AppRuntime(old, "a", backend, StandardTestDispatcher(testScheduler))
        runCurrent()
        assertEquals(WalkStatus.Interrupted, reopened.walkState.value.session?.status)
        reopened.close()
    }

    @Test fun backgroundStopWaitsForDelayedStartEmissionButNotForAYoungerWalk() = runTest {
        val store = MemoryStore()
        val backend = object : NativeBackend {
            override fun observeUser(observer: BackendUserObserver) = object : CancelHandle { override fun cancel() {} }
        }
        val runtime = AppRuntime(store, "a", backend, StandardTestDispatcher(testScheduler))
        runCurrent() // The initial empty Room snapshot has arrived.
        val startedAt = Clock.System.now().toEpochMilliseconds()
        store.start(WalkSession("pending", "a", startedAt, WalkStatus.Recording,
            lastResumedAtMillis = startedAt, segmentStartedAtMillis = startedAt))
        // onStop runs before the new Room snapshot reaches the controller.
        runtime.pauseForBackground()
        assertNull(runtime.walkState.value.session)
        runCurrent()
        assertEquals(WalkStatus.Interrupted, runtime.walkState.value.session?.status)
        runtime.close()

        val laterStore = MemoryStore()
        val later = AppRuntime(laterStore, "a", backend, StandardTestDispatcher(testScheduler))
        runCurrent()
        later.pauseForBackground()
        val laterStart = Clock.System.now().toEpochMilliseconds() + 1_000
        laterStore.start(WalkSession("foreground", "a", laterStart, WalkStatus.Recording,
            lastResumedAtMillis = laterStart, segmentStartedAtMillis = laterStart))
        runCurrent()
        assertEquals(WalkStatus.Recording, later.walkState.value.session?.status)
        later.close()
    }

    @Test fun backgroundStopAlsoCatchesADelayedResumeEmission() = runTest {
        val store = MemoryStore()
        val backend = object : NativeBackend {
            override fun observeUser(observer: BackendUserObserver) = object : CancelHandle { override fun cancel() {} }
        }
        val runtime = AppRuntime(store, "a", backend, StandardTestDispatcher(testScheduler))
        val startedAt = Clock.System.now().toEpochMilliseconds() - 1_000
        store.start(WalkSession("pending-resume", "a", startedAt, WalkStatus.Interrupted,
            activeDurationMillis = 1_000, segmentStartedAtMillis = startedAt))
        runCurrent()
        val resumedAt = Clock.System.now().toEpochMilliseconds()
        store.resume("a", "pending-resume", resumedAt)
        // Room has committed Recording, but its Flow has not emitted to the runtime.
        runtime.pauseForBackground()
        assertEquals(WalkStatus.Interrupted, runtime.walkState.value.session?.status)
        runCurrent()
        assertEquals(1, runtime.walkState.value.session?.segment)
        assertEquals(WalkStatus.Interrupted, runtime.walkState.value.session?.status)
        runtime.close()
    }
}

private class MemoryStore : WalkSessionStore {
    private val latest = MutableStateFlow<WalkSession?>(null)
    var closed = 0
    override fun observe(userId: String) = latest.map { it?.takeIf { it.userId == userId } }
    override fun observeHistory(userId: String) = flowOf(emptyList<WalkSession>())
    override fun observeRecord(userId: String, walkId: String) = flowOf<WalkSession?>(null)
    override fun observePoints(userId: String, walkId: String) = flowOf(emptyList<WalkPoint>())
    override fun observeDog(userId: String) = flowOf<DogProfile?>(null)
    override suspend fun saveDog(profile: DogProfile) {}
    override suspend fun start(session: WalkSession) { latest.value = session }
    override suspend fun finish(userId: String, walkId: String, endedAtMillis: Long) {
        latest.value = latest.value?.copy(status = WalkStatus.Completed, endedAtMillis = endedAtMillis)
    }
    override suspend fun pause(userId: String, walkId: String, nowMillis: Long) {
        latest.value = latest.value?.takeIf { it.userId == userId && it.id == walkId }
            ?.copy(status = WalkStatus.Interrupted)
    }
    override suspend fun resume(userId: String, walkId: String, nowMillis: Long) {
        latest.value = latest.value?.takeIf {
            it.userId == userId && it.id == walkId && it.status == WalkStatus.Interrupted
        }?.let { it.copy(status = WalkStatus.Recording, lastResumedAtMillis = nowMillis,
                         segmentStartedAtMillis = nowMillis, segment = it.segment + 1) }
    }
    override suspend fun checkpoint(userId: String, walkId: String, nowMillis: Long) {}
    override suspend fun appendPoint(userId: String, walkId: String, segment: Int, sample: LocationSample) {}
    override fun close() { closed++ }
}
