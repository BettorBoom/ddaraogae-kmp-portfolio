package dev.ddaraogae.shared

import androidx.compose.ui.window.ComposeUIViewController
import dev.ddaraogae.data.NativeBackend
import dev.ddaraogae.data.openIosWalkStore
import dev.ddaraogae.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

fun scenarioViewController(screen: String, stateName: String, dark: Boolean, fontScale: Float) =
    ComposeUIViewController { DdaraogaeScenario(screen, stateName, dark, fontScale) }

fun createIosDemoRuntime(databasePath: String, backend: NativeBackend): AppRuntime {
    val store = openIosWalkStore(databasePath)
    val uid = "local-preview"
    runBlocking {
        if (store.observeHistory(uid).first().isEmpty()) {
            store.saveDog(DogProfile(uid, "보리 · 시연"))
            store.start(WalkSession("demo-route-1", uid, 1_000, WalkStatus.Recording,
                dogName = "보리 · 시연", lastResumedAtMillis = 1_000))
            store.appendPoint(uid, "demo-route-1", 0, LocationSample(37.5, 127.0, 2_000, 5.0))
            store.appendPoint(uid, "demo-route-1", 0, LocationSample(37.5001, 127.0, 5_000, 5.0))
            store.appendPoint(uid, "demo-route-1", 0, LocationSample(37.5002, 127.0, 8_000, 5.0))
            store.finish(uid, "demo-route-1", 12_000)
        }
    }
    return AppRuntime(store, uid, backend).also { it.demoData = true }
}
