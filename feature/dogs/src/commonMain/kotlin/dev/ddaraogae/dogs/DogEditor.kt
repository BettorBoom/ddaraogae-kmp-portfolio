package dev.ddaraogae.dogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import dev.ddaraogae.designsystem.*

@Composable
fun DogEditor(currentName: String?, busy: Boolean, error: String?,
              onSave: (String) -> Unit, onBack: () -> Unit) {
    var draft by rememberSaveable(currentName) { mutableStateOf(currentName.orEmpty()) }
    Page(if (currentName == null) "함께 걸을 친구를 알려주세요" else "반려견 이름 바꾸기",
        if (currentName == null) null else "홈으로", onBack) {
        Text("반려견 이름은 이 기기에 저장되고 산책 기록에 남아요.")
        OutlinedTextField(value = draft, onValueChange = { draft = it.take(30) },
            label = { Text("반려견 이름") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("dog-name-field"))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryAction(if (busy) "저장 중…" else "이름 저장하기",
            enabled = !busy && draft.trim().isNotEmpty(), onClick = { onSave(draft.trim()) })
    }
}
