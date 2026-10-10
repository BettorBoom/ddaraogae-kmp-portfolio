package dev.ddaraogae.data

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BackendCancellationTest {
    @Test fun releasesListenerAndRejectsLateCallbacks() = runTest {
        var observer: BackendUserObserver? = null
        var cancellations = 0
        val values = mutableListOf<String?>()
        val backend = object : NativeBackend {
            override fun observeUser(value: BackendUserObserver): CancelHandle {
                observer = value
                return object : CancelHandle { override fun cancel() { cancellations++ } }
            }
        }
        val job = launch { observeBackendUser(backend).collect { values.add(it) } }
        runCurrent()
        observer!!.onUser("user-a")
        runCurrent()
        job.cancel()
        runCurrent()
        observer!!.onUser("late-user")
        runCurrent()
        assertEquals(listOf<String?>("user-a"), values)
        assertEquals(1, cancellations)
    }
}
