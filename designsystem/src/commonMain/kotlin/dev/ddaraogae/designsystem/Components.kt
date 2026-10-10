package dev.ddaraogae.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import dev.ddaraogae.designsystem.resources.Res
import dev.ddaraogae.designsystem.resources.legacy_dog_logo

enum class ScreenStatus { Ready, Loading, Empty, Error, PermissionDenied }

object Space {
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
}

@Composable
fun DogAvatar(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.legacy_dog_logo), "기본 반려견 그림",
        modifier.clip(CircleShape), contentScale = ContentScale.Crop)
}

@Composable
fun Page(title: String, backLabel: String? = null, onBack: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).heightIn(min = 60.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (backLabel != null) TextButton(onClick = onBack, modifier = Modifier.widthIn(min = 78.dp).heightIn(min = 48.dp)) {
                Text(backLabel, maxLines = 1)
            } else Spacer(Modifier.width(78.dp))
            Text(title, modifier = Modifier.weight(1f).testTag("screen-title").semantics { heading() },
                style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.width(78.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) { content() }
    }
}

@Composable
fun PrimaryAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    // Minimum height lets scaled text wrap; a fixed height would clip it (ADR-006).
    Button(onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("primary-action"),
        contentPadding = PaddingValues(Space.medium)) { Text(label, style = MaterialTheme.typography.titleMedium) }
}

@Composable
fun InfoCard(title: String, body: String, content: @Composable ColumnScope.() -> Unit = {}) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(Space.medium), verticalArrangement = Arrangement.spacedBy(Space.small)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
fun StateNotice(status: ScreenStatus, emptyMessage: String, onRetry: (() -> Unit)?) {
    if (status == ScreenStatus.Ready) return
    InfoCard(when (status) {
        ScreenStatus.Loading -> "잠시 확인하고 있어요"
        ScreenStatus.Empty -> "아직 기록이 없어요"
        ScreenStatus.Error -> "불러오지 못했어요"
        ScreenStatus.PermissionDenied -> "권한 확인이 필요해요"
        ScreenStatus.Ready -> return
    }, when (status) {
        ScreenStatus.Loading -> "확인이 끝나면 이어서 이용할 수 있어요."
        ScreenStatus.Empty -> emptyMessage
        ScreenStatus.Error -> "연결 상태를 확인한 뒤 다시 시도해 주세요."
        ScreenStatus.PermissionDenied -> "위치를 기록하려면 권한이 필요해요. 거절해도 이전 기록은 남아 있어요."
        ScreenStatus.Ready -> return
    }) {
        if (status == ScreenStatus.Loading) CircularProgressIndicator()
        if (onRetry != null && (status == ScreenStatus.Error || status == ScreenStatus.PermissionDenied)) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (status == ScreenStatus.Error) "다시 시도" else "권한 안내 보기")
            }
        }
    }
}

@Composable
fun MapPlaceholder() {
    InfoCard("함께 걸은 길", "저장된 위치가 없어요. 앱이 열린 동안 위치 권한을 허용하면 경로를 기록해요.")
}
