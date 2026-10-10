package dev.ddaraogae.walk

import androidx.lifecycle.ViewModelStore
import dev.ddaraogae.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenLifetimeTest {
    @Test fun clearingScreenDoesNotCancelUserSession() = runTest {
        val latest = MutableStateFlow<WalkSession?>(null)
        val store = object : WalkSessionStore {
            override fun observe(userId: String) = latest
            override fun observeHistory(userId: String) = flowOf(emptyList<WalkSession>())
            override fun observeRecord(userId: String, walkId: String) = flowOf<WalkSession?>(null)
            override fun observePoints(userId: String, walkId: String) = flowOf(emptyList<WalkPoint>())
            override fun observeDog(userId: String) = flowOf<DogProfile?>(null)
            override suspend fun saveDog(profile: DogProfile) {}
            override suspend fun start(session: WalkSession) { latest.value = session }
            override suspend fun finish(userId: String, walkId: String, endedAtMillis: Long) {}
            override suspend fun pause(userId: String, walkId: String, nowMillis: Long) {}
            override suspend fun resume(userId: String, walkId: String, nowMillis: Long) {}
            override suspend fun checkpoint(userId: String, walkId: String, nowMillis: Long) {}
            override suspend fun appendPoint(userId: String, walkId: String, segment: Int, sample: LocationSample) {}
            override fun close() {}
        }
        val controller = WalkSessionController("a", store, backgroundScope)
        val screens = ViewModelStore()
        screens.put("walk", WalkViewModel(controller))
        screens.clear()
        store.start(WalkSession("survives", "a", 100, WalkStatus.Recording))
        runCurrent()
        assertEquals("survives", WalkViewModel(controller).state.value.session?.id)
    }

    @Test fun missingControllerRegistrationFailsResolution() {
        val app = koinApplication { modules(walkModule) }
        try {
            val scope = app.koin.createScope("missing", named("user"))
            assertFails { scope.get<WalkViewModel>() }
        } finally { app.close() }
    }
}
