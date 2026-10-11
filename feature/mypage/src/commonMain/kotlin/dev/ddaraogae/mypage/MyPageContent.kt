package dev.ddaraogae.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ddaraogae.designsystem.AppBar
import dev.ddaraogae.designsystem.AppIcons
import dev.ddaraogae.designsystem.AppText
import dev.ddaraogae.designsystem.AppTheme
import dev.ddaraogae.designsystem.LocalToast

/**
 * The original MyPage tab (fragment_mypage.xml). The dog list and the alarm screen are reachable;
 * account and terms rows only explain that this local build has no account or terms text yet.
 */
@Composable
fun MyPageContent(onDogs: () -> Unit, onAlarm: () -> Unit) {
    val colors = AppTheme.colors
    var unavailable by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(colors.white)) {
        AppBar("마이페이지")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Section("정보 수정")
            MenuRow("나의 반려견 정보", onDogs, chevron = true, tag = "mypage-dogs")
            Divider()
            Section("기능 설정")
            MenuRow("산책 알림 설정", onAlarm, chevron = true, tag = "mypage-alarm")
            Divider()
            Section("회원 관리")
            MenuRow("로그아웃", { unavailable = "로그아웃" }, tag = "mypage-logout")
            MenuRow("회원탈퇴", { unavailable = "회원탈퇴" }, tag = "mypage-withdraw")
            Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(0.5.dp))
            MenuRow("서비스 이용약관", { unavailable = "서비스 이용약관" }, tag = "mypage-terms")
            MenuRow("개인정보처리방침", { unavailable = "개인정보처리방침" }, tag = "mypage-privacy")
            MenuRow("개인정보처리방침 동의", { unavailable = "개인정보처리방침 동의" }, tag = "mypage-agreement")
        }
    }
    unavailable?.let { title ->
        AlertDialog(onDismissRequest = { unavailable = null },
            confirmButton = { TextButton(onClick = { unavailable = null }) { Text("확인") } },
            title = { Text(title) },
            text = { Text("이 로컬 시연판에는 계정과 약관 본문이 연결되어 있지 않아요.") })
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = AppText.textBold, color = AppTheme.colors.text, modifier = Modifier.padding(start = 20.dp, top = 30.dp))
}

@Composable
private fun Divider() {
    Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth().height(0.5.dp).background(AppTheme.colors.grey))
}

@Composable
private fun MenuRow(title: String, onClick: () -> Unit, chevron: Boolean = false, tag: String) {
    // The original rows are as tall as their 14sp text; the project keeps a 48dp touch height (ADR-006).
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).testTag(tag),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = AppText.caption, color = AppTheme.colors.text, modifier = Modifier.weight(1f).padding(horizontal = 20.dp))
        if (chevron) Icon(AppIcons.ChevronRight, contentDescription = null, tint = AppTheme.colors.grey,
            modifier = Modifier.padding(end = 20.dp).size(24.dp))
    }
}

/**
 * activity_alarm: an empty list with the orange "추가". Reminders are not scheduled in this build,
 * so adding one says so instead of inventing an entry (ADR-013).
 */
@Composable
fun AlarmScreen(onBack: () -> Unit) {
    val colors = AppTheme.colors
    val toast = LocalToast.current
    Column(Modifier.fillMaxSize().background(colors.white)) {
        AppBar("산책 리마인더", onBack = onBack, height = 56.dp, dividerHeight = 1.dp, titleStyle = AppText.bodyBold,
            trailing = {
                Text("추가", style = AppText.text.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = colors.orange,
                    modifier = Modifier.align(Alignment.CenterEnd).clickable(role = Role.Button) {
                        toast.show("알림 예약은 아직 연결되지 않았어요.")
                    }.padding(horizontal = 20.dp, vertical = 12.dp).testTag("primary-action"))
            })
        Box(Modifier.fillMaxSize().background(colors.bgGrey), contentAlignment = Alignment.Center) {
            Text("등록된 알람이 없습니다.", style = AppText.subtitle, color = colors.text,
                textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
        }
    }
}
