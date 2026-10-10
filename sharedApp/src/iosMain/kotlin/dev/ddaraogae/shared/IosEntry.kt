package dev.ddaraogae.shared

import androidx.compose.ui.window.ComposeUIViewController
import dev.ddaraogae.data.NativeBackend
import dev.ddaraogae.data.openIosWalkStore

fun createIosRuntime(databasePath: String, backend: NativeBackend): AppRuntime =
    AppRuntime(openIosWalkStore(databasePath), "local-preview", backend).also {
        it.locationNotice.value = "iOS 위치 수집은 준비 중이에요. 시간과 기록은 이 기기에 저장돼요."
    }

fun mainViewController(runtime: AppRuntime) = ComposeUIViewController { DdaraogaeApp(runtime) }
