package dev.ddaraogae.data

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlin.native.HiddenFromObjC
import kotlin.experimental.ExperimentalObjCRefinement

interface CancelHandle { fun cancel() }
interface BackendUserObserver { fun onUser(userId: String?) }
interface NativeBackend { fun observeUser(observer: BackendUserObserver): CancelHandle }

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
fun observeBackendUser(backend: NativeBackend) = callbackFlow {
    val handle = backend.observeUser(object : BackendUserObserver {
        override fun onUser(userId: String?) { trySend(userId) }
    })
    // Cancellation closes the channel first: late native callbacks cannot
    // re-enter the cancelled user's session. ADR-003.
    awaitClose { handle.cancel() }
}
