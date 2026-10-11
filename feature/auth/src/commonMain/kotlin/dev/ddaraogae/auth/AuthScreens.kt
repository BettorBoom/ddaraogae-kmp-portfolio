package dev.ddaraogae.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ddaraogae.designsystem.AppIcons
import dev.ddaraogae.designsystem.AppText
import dev.ddaraogae.designsystem.AppTheme
import dev.ddaraogae.designsystem.BrownButton
import dev.ddaraogae.designsystem.GoogleMark

// UI only. Real sign-in belongs to M06 and waits for the M04 gate (ADR-008, ADR-013); the
// callbacks do nothing here and these screens are shown only by the scenario gallery.

/** login_box: white field with a 1 unit #D9D9D9 stroke, 55dp tall. */
@Composable
private fun LoginField(value: String, onChange: (String) -> Unit, hint: String, tag: String, modifier: Modifier = Modifier,
                       password: Boolean = false, visible: Boolean = true, onToggle: (() -> Unit)? = null, maxLength: Int = 30) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(10.dp)
    // 55dp like the original, growing when large text wraps the hint (ADR-006).
    Box(modifier.fillMaxWidth().heightIn(min = 55.dp).background(colors.white, shape).border(1.dp, colors.grey, shape)
        .padding(horizontal = 20.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(hint, style = AppText.text, color = colors.greyTrans)
        BasicTextField(value, { onChange(it.take(maxLength)) }, Modifier.fillMaxWidth().padding(end = if (onToggle != null) 36.dp else 0.dp).testTag(tag),
            singleLine = true, textStyle = AppText.text.copy(color = colors.blackFix), cursorBrush = SolidColor(colors.brown),
            visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Email))
        if (onToggle != null) Icon(if (visible) AppIcons.Visibility else AppIcons.VisibilityOff, contentDescription = "비밀번호 표시",
            tint = colors.grey, modifier = Modifier.align(Alignment.CenterEnd).size(24.dp).clickable(role = Role.Button, onClick = onToggle))
    }
}

@Composable
fun LoginContent(onLogin: () -> Unit = {}, onSignUp: () -> Unit = {}, onGoogle: () -> Unit = {}) {
    val colors = AppTheme.colors
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(colors.white).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("따라오개에서\n산책을 시작해 볼까요?", style = AppText.content, color = colors.text,
            modifier = Modifier.padding(top = 80.dp).testTag("screen-title").semantics { heading() })
        LoginField(email, { email = it }, "이메일", "login-email", Modifier.padding(top = 60.dp))
        LoginField(password, { password = it }, "비밀번호", "login-password", Modifier.padding(top = 28.dp),
            password = true, visible = visible, onToggle = { visible = !visible }, maxLength = 16)
        BrownButton("로그인", onLogin, Modifier.padding(top = 50.dp).fillMaxWidth().testTag("primary-action"))
        Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("계정이 없으신가요?", style = AppText.caption, color = colors.text)
            Text("회원가입", style = AppText.caption, color = colors.brownTrans,
                modifier = Modifier.clickable(role = Role.Button, onClick = onSignUp))
        }
        Row(Modifier.padding(top = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(colors.grey))
            Text("SNS 로그인", style = AppText.caption, color = colors.text, modifier = Modifier.padding(horizontal = 20.dp))
            Box(Modifier.weight(1f).height(1.dp).background(colors.grey))
        }
        val shape = RoundedCornerShape(10.dp)
        Box(Modifier.padding(vertical = 36.dp).fillMaxWidth().heightIn(min = 55.dp).background(colors.whiteFix, shape)
            .border(1.dp, colors.grey, shape).clickable(role = Role.Button, onClick = onGoogle).padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center) {
            GoogleMark(Modifier.align(Alignment.CenterStart).size(28.dp))
            // AppCompatButton upper-cases Latin letters: the original reads "GOOGLE 로그인".
            Text("GOOGLE 로그인", style = AppText.captionBold, color = colors.blackFix, textAlign = TextAlign.Center)
        }
    }
}

private val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private val passwordPattern = Regex("^(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z0-9$@!%*#?&.]{8,16}$")

@Composable
fun SignUpContent(onBack: () -> Unit = {}, onDone: () -> Unit = {}) {
    val colors = AppTheme.colors
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    var terms by rememberSaveable { mutableStateOf(listOf(false, false, false)) }
    val valid = emailPattern.matches(email) && passwordPattern.matches(password) && password == confirm && terms.all { it }
    Column(Modifier.fillMaxSize().background(colors.white).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Box(Modifier.padding(top = 20.dp).size(48.dp).clickable(role = Role.Button, onClickLabel = "뒤로", onClick = onBack)
            .testTag("back"), contentAlignment = Alignment.CenterStart) {
            Icon(AppIcons.Back, contentDescription = "뒤로", tint = colors.black, modifier = Modifier.size(18.dp))
        }
        Text("환영합니다!\n이메일과 비밀번호를\n입력해주세요.", style = AppText.content, color = colors.text,
            modifier = Modifier.padding(top = 32.dp).testTag("screen-title").semantics { heading() })
        LoginField(email, { email = it }, "이메일", "signup-email", Modifier.padding(top = 50.dp))
        Warning("이메일 형식이 유효하지 않습니다.", email.isNotEmpty() && !emailPattern.matches(email))
        LoginField(password, { password = it }, "8~16자 영어/숫자 조합으로 입력", "signup-password", Modifier.padding(top = 18.dp),
            password = true, visible = showPassword, onToggle = { showPassword = !showPassword }, maxLength = 16)
        Warning("비밀번호 형식이 유효하지 않습니다.", password.isNotEmpty() && !passwordPattern.matches(password))
        LoginField(confirm, { confirm = it }, "비밀번호 확인", "signup-confirm", Modifier.padding(top = 18.dp),
            password = true, visible = showConfirm, onToggle = { showConfirm = !showConfirm }, maxLength = 16)
        Warning("비밀번호와 일치하지 않습니다.", confirm.isNotEmpty() && confirm != password)
        TermRow("약관 전체 동의", terms.all { it }, chevron = false, tag = "terms-all", indent = 0) { checked -> terms = List(3) { checked } }
        listOf("이용약관 동의(필수)", "개인정보 처리방침(필수)", "개인정보 처리 동의(필수)").forEachIndexed { index, label ->
            TermRow(label, terms[index], chevron = true, tag = "terms-$index", indent = 12) { checked ->
                terms = terms.toMutableList().also { it[index] = checked }
            }
        }
        BrownButton("완료", onDone, Modifier.padding(top = 36.dp, bottom = 32.dp).fillMaxWidth().testTag("primary-action"),
            color = if (valid) colors.brown else colors.grey)
    }
}

@Composable
private fun Warning(text: String, show: Boolean) {
    Text(text, style = AppText.mini, color = if (show) AppTheme.colors.red else Color.Transparent,
        modifier = Modifier.padding(start = 10.dp, top = 4.dp))
}

@Composable
private fun TermRow(label: String, checked: Boolean, chevron: Boolean, tag: String, indent: Int, onChange: (Boolean) -> Unit) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = indent.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onChange, Modifier.testTag(tag), colors = CheckboxDefaults.colors(checkedColor = colors.brown))
        Text(label, style = AppText.caption, color = colors.text, modifier = Modifier.weight(1f))
        if (chevron) Icon(AppIcons.ChevronRight, contentDescription = null, tint = colors.grey, modifier = Modifier.size(24.dp))
    }
}
