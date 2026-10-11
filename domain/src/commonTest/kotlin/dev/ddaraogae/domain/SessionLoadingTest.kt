package dev.ddaraogae.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SessionLoadingTest {
    @Test fun readFailureDoesNotBecomeAnEmptyWalk() = runTest {
        var inserts = 0
        val store = object : WalkSessionStore {
            override fun observe(userId: String) = flow<WalkSession?> { error("broken database") }
            override fun observeHistory(userId: String) = flowOf(emptyList<WalkSession>())
            override fun observeRecord(userId: String, walkId: String) = flowOf<WalkSession?>(null)
            override fun observePoints(userId: String, walkId: String) = flowOf(emptyList<WalkPoint>())
            override fun observeDog(userId: String) = flowOf<DogProfile?>(null)
            override suspend fun saveDog(profile: DogProfile) {}
            override suspend fun deleteDog(userId: String) {}
            override suspend fun start(session: WalkSession) { inserts++ }
            override suspend fun finish(userId: String, walkId: String, endedAtMillis: Long) {}
            override suspend fun pause(userId: String, walkId: String, nowMillis: Long) {}
            override suspend fun resume(userId: String, walkId: String, nowMillis: Long) {}
            override suspend fun checkpoint(userId: String, walkId: String, nowMillis: Long) {}
            override suspend fun appendPoint(userId: String, walkId: String, segment: Int, sample: LocationSample) {}
            override fun close() {}
        }
        val controller = WalkSessionController("a", store, backgroundScope)
        assertFalse(controller.state.value.loaded)
        assertFailsWith<IllegalStateException> { controller.start("premature", 100) }
        runCurrent()
        assertTrue(controller.state.value.failed)
        assertFailsWith<IllegalStateException> { controller.start("after-error", 100) }
        assertEquals(0, inserts)
    }
}
