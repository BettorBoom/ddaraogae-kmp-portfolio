package dev.ddaraogae.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ddaraogae.domain.WalkSessionController
import dev.ddaraogae.domain.WalkPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class WalkViewModel(private val controller: WalkSessionController) : ViewModel() {
    val state = controller.state
    val dog = controller.dog
    fun points(walkId: String): Flow<List<WalkPoint>> = controller.points(walkId)
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    fun start() {
        // Same wording as the original's no-dog toast (msg_no_selected_dog).
        val dogName = dog.value?.name ?: run {
            _error.value = "선택된 반려견이 없습니다"
            return
        }
        act { controller.start(Uuid.random().toString(), Clock.System.now().toEpochMilliseconds(), dogName) }
    }

    fun resume() {
        val walkId = state.value.session?.id ?: return
        act { controller.resume(walkId, Clock.System.now().toEpochMilliseconds()) }
    }

    /** Ends the walk; [onFinished] receives its id once the end is saved, to open the finish screen. */
    fun finish(onFinished: (String) -> Unit) {
        val walkId = state.value.session?.id ?: return
        act {
            controller.finish(walkId, Clock.System.now().toEpochMilliseconds())
            onFinished(walkId)
        }
    }

    fun clearError() { _error.value = null }

    private fun act(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _error.value = "기록을 저장하지 못했어요. 다시 시도해 주세요." }
            finally { _busy.value = false }
        }
    }
}
