package dev.ddaraogae.shared

import androidx.compose.ui.window.ComposeUIViewController
import dev.ddaraogae.data.NativeBackend
import dev.ddaraogae.data.openIosWalkStore

fun createIosRuntime(databasePath: String, backend: NativeBackend): AppRuntime =
    AppRuntime(openIosWalkStore(databasePath), "local-preview", backend)

fun mainViewController(runtime: AppRuntime) = ComposeUIViewController { DdaraogaeApp(runtime) }
