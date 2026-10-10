package dev.ddaraogae.shared

import android.content.Context
import dev.ddaraogae.data.*

fun createAndroidRuntime(context: Context): AppRuntime {
    val localBackend = object : NativeBackend {
        override fun observeUser(observer: BackendUserObserver): CancelHandle {
            observer.onUser("local-preview")
            return object : CancelHandle { override fun cancel() {} }
        }
    }
    return AppRuntime(openAndroidWalkStore(context.applicationContext), "local-preview", localBackend)
}
