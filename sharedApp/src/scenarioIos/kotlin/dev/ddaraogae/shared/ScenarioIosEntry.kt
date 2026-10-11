package dev.ddaraogae.shared

import androidx.compose.ui.window.ComposeUIViewController
import dev.ddaraogae.data.NativeBackend
import dev.ddaraogae.data.openIosWalkStore
import dev.ddaraogae.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

fun scenarioViewController(screen: String, stateName: String, dark: Boolean, fontScale: Float) =
    ComposeUIViewController { DdaraogaeScenario(screen, stateName, dark, fontScale) }

fun createIosDemoRuntime(databasePath: String, backend: NativeBackend): AppRuntime {
    val store = openIosWalkStore(databasePath)
    val uid = "local-preview"
    runBlocking {
        if (store.observeHistory(uid).first().isEmpty()) {
            // Dated now so the history screen's current month lists it.
            val start = Clock.System.now().toEpochMilliseconds() - 600_000
            store.saveDog(DogProfile(uid, "보리 · 시연", DogGender.Female, 3, "시연견", null))
            store.start(WalkSession("demo-route-1", uid, start, WalkStatus.Recording,
                dogName = "보리 · 시연", lastResumedAtMillis = start))
            store.appendPoint(uid, "demo-route-1", 0, LocationSample(37.5, 127.0, start + 1_000, 5.0))
            store.appendPoint(uid, "demo-route-1", 0, LocationSample(37.5001, 127.0, start + 4_000, 5.0))
            store.appendPoint(uid, "demo-route-1", 0, LocationSample(37.5002, 127.0, start + 7_000, 5.0))
            store.finish(uid, "demo-route-1", start + 11_000)
        }
    }
    return AppRuntime(store, uid, backend).also { it.demoData = true }
}
