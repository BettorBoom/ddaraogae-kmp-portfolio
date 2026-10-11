package dev.ddaraogae.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** The original reports most feedback as short Toasts; this is the shared equivalent. */
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set
    private var serial by mutableStateOf(0)
    val current: Pair<Int, String>? get() = message?.let { serial to it }

    fun show(text: String) {
        message = text
        serial += 1
    }

    internal fun dismiss(id: Int) {
        if (serial == id) message = null
    }
}

val LocalToast = staticCompositionLocalOf { ToastState() }

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val current = state.current ?: return
    LaunchedEffect(current.first) {
        delay(2_000)
        state.dismiss(current.first)
    }
    Box(modifier.padding(horizontal = 24.dp), contentAlignment = Alignment.BottomCenter) {
        Text(current.second, color = Color.White, style = AppText.caption,
            modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xE6323232))
                .padding(horizontal = 18.dp, vertical = 10.dp).testTag("toast"))
    }
}
