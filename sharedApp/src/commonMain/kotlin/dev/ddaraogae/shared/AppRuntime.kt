package dev.ddaraogae.shared

import dev.ddaraogae.data.NativeBackend
import dev.ddaraogae.data.observeBackendUser
import dev.ddaraogae.domain.WalkSessionController
import dev.ddaraogae.domain.WalkSessionStore
import dev.ddaraogae.domain.WalkSessionState
import dev.ddaraogae.domain.WalkStatus
import dev.ddaraogae.domain.LocationSample
import dev.ddaraogae.walk.walkModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@OptIn(ExperimentalObjCRefinement::class)
class AppRuntime internal constructor(
    store: WalkSessionStore,
    userId: String,
    backend: NativeBackend,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private val validatedUserId = userId.also { require(it.isNotBlank()) }
    private val job = SupervisorJob()
    internal val userCoroutines = CoroutineScope(job + dispatcher)
    private val application = koinApplication {
        modules(walkModule, module {
            single<WalkSessionStore> { store }
            scope(named("user")) {
                scoped<CoroutineScope> { userCoroutines }
                scoped { WalkSessionController(validatedUserId, get(), get()) }
            }
        })
    }
    internal val userScope = application.koin.createScope("user-$userId", named("user"))
    internal val controller = userScope.get<WalkSessionController>()
    @HiddenFromObjC val walkState: StateFlow<WalkSessionState> get() = controller.state
    @HiddenFromObjC val writeFailed = MutableStateFlow(false)
    @HiddenFromObjC val locationNotice = MutableStateFlow<String?>(null)
    private val backgroundCutoffs = MutableStateFlow<List<Long>>(emptyList())
    internal var demoData: Boolean = false
    private val database = store
    private val createdAtMillis = now()
    var isClosed: Boolean = false
        private set

    init {
        // Subscription lifetime probe only. Authentication and account switching
        // belong to M04; local-preview is not a Firebase UID.
        userCoroutines.launch { observeBackendUser(backend).collect {} }
        // A new process cannot infer elapsed time or position while it was gone.
        userCoroutines.launch {
            controller.state.first { it.loaded }.session?.takeIf {
                // The initial Room emission may arrive after the first user tap.
                // Recover only a walk that predates this runtime instance.
                it.status == WalkStatus.Recording && it.startedAtMillis < createdAtMillis
            }
                ?.let { controller.pause(it.id, it.lastResumedAtMillis ?: it.startedAtMillis) }
        }
        userCoroutines.launch {
            combine(controller.state, backgroundCutoffs) { state, cutoffs -> state.session to cutoffs }
                .collect { (session, cutoffs) ->
                    if (session?.status != WalkStatus.Recording || cutoffs.isEmpty()) return@collect
                    val segmentStart = segmentStart(session)
                    val cutoff = cutoffs.firstOrNull { it >= segmentStart }
                    if (cutoff == null) {
                        backgroundCutoffs.value = cutoffs.filter { it >= segmentStart }
                        return@collect
                    }
                    // A start/resume can commit after onStop but before Room
                    // publishes it. Keep the stop time until that write appears.
                    while (isActive && controller.state.value.session?.let {
                            it.id == session.id && it.status == WalkStatus.Recording && it.segment == session.segment
                        } == true) {
                        try {
                            controller.pause(session.id, cutoff)
                            writeFailed.value = false
                            break
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            writeFailed.value = true
                            delay(1_000)
                        }
                    }
                    backgroundCutoffs.value = backgroundCutoffs.value.filter { it > cutoff }
                }
        }
        userCoroutines.launch {
            while (isActive) {
                delay(5_000)
                controller.state.value.session?.takeIf { it.status == WalkStatus.Recording }
                    ?.takeIf { session -> backgroundCutoffs.value.none { it >= segmentStart(session) } }
                    ?.let { session -> write { controller.checkpoint(session.id, now()) } }
            }
        }
    }

    private fun now() = Clock.System.now().toEpochMilliseconds()
    private fun segmentStart(session: dev.ddaraogae.domain.WalkSession) =
        session.segmentStartedAtMillis ?: session.lastResumedAtMillis ?: session.startedAtMillis

    private suspend fun write(block: suspend () -> Unit) {
        try { block(); writeFailed.value = false }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { writeFailed.value = true }
    }

    @HiddenFromObjC fun saveDog(name: String, onFinished: (Boolean) -> Unit) {
        userCoroutines.launch {
            try {
                controller.saveDog(name)
                onFinished(true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onFinished(false)
            }
        }
    }

    fun pauseForBackground() {
        backgroundCutoffs.value = backgroundCutoffs.value + now()
    }

    @HiddenFromObjC fun appendLocation(walkId: String, segment: Int, sample: LocationSample) {
        userCoroutines.launch {
            val current = walkState.value.session
            if (current?.id == walkId && current.segment == segment &&
                backgroundCutoffs.value.any { it >= segmentStart(current) && sample.recordedAtMillis >= it }) return@launch
            write { controller.appendPoint(walkId, segment, sample) }
        }
    }

    fun close() {
        if (isClosed) return
        isClosed = true
        // Koin.close alone does not cancel coroutines or native listeners.
        job.cancel()
        userScope.close()
        application.close()
        database.close()
    }
}
