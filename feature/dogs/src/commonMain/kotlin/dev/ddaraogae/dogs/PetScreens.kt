package dev.ddaraogae.dogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ddaraogae.designsystem.AppBar
import dev.ddaraogae.designsystem.AppIcons
import dev.ddaraogae.designsystem.AppText
import dev.ddaraogae.designsystem.AppTheme
import dev.ddaraogae.designsystem.BrownButton
import dev.ddaraogae.designsystem.CameraBadge
import dev.ddaraogae.designsystem.DogCircle
import dev.ddaraogae.designsystem.LocalToast
import dev.ddaraogae.domain.DogGender
import dev.ddaraogae.domain.DogProfile

private fun DogGender?.label() = when (this) {
    DogGender.Male -> "남아"
    DogGender.Female -> "여아"
    null -> ""
}

private const val MAX_NAME = 20
private const val MAX_BREED = 20

/** What the form hands back; the caller saves it and shows the result toast. */
class PetDraft(val name: String, val gender: DogGender, val age: Int?, val breed: String?, val memo: String?)

@Composable
private fun BoxScope.BarAction(text: String, tag: String, color: Color, onClick: () -> Unit) {
    Text(text, style = AppText.text, color = color, modifier = Modifier.align(Alignment.CenterEnd)
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp).testTag(tag))
}

/** fragment_pet_list: 140dp rows, "추가하기" at the top right, empty text when no dog is saved. */
@Composable
fun PetListScreen(dog: DogProfile?, onBack: () -> Unit, onAdd: () -> Unit, onOpen: () -> Unit) {
    val colors = AppTheme.colors
    Column(Modifier.fillMaxSize().background(colors.white)) {
        AppBar("반려견 정보", onBack = onBack, trailing = { BarAction("추가하기", "pet-add", colors.brownTrans, onAdd) })
        if (dog == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("반려견 정보가 없습니다.", style = AppText.text, color = colors.text)
            }
        } else {
            Row(Modifier.fillMaxWidth().heightIn(min = 140.dp).clickable(role = Role.Button, onClick = onOpen).testTag("pet-row"),
                verticalAlignment = Alignment.CenterVertically) {
                DogCircle(Modifier.padding(start = 20.dp), size = 100.dp)
                Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                    Text(dog.name, style = AppText.bodyBold, color = colors.text)
                    Text(dog.gender.label(), style = AppText.caption, color = colors.text, modifier = Modifier.padding(top = 8.dp))
                }
                Icon(AppIcons.ChevronRight, contentDescription = null, tint = colors.grey,
                    modifier = Modifier.padding(end = 20.dp).size(24.dp))
            }
            Box(Modifier.fillMaxWidth(0.9f).align(Alignment.CenterHorizontally).height(1.dp).background(colors.lightGrey))
        }
    }
}

@Composable
private fun FieldLabel(text: String, top: Int) =
    Text(text, style = AppText.textBold, color = AppTheme.colors.text, modifier = Modifier.padding(start = 20.dp, top = top.dp))

/** The photo slot: the original opens the gallery; this build has no photo storage yet. */
@Composable
private fun PhotoSlot(onPhoto: (() -> Unit)?) {
    Box(Modifier.fillMaxWidth().padding(top = 50.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(130.dp).then(
            if (onPhoto != null) Modifier.clickable(role = Role.Button, onClickLabel = "사진 바꾸기", onClick = onPhoto)
            else Modifier).testTag("pet-photo")) {
            DogCircle(size = 130.dp, ring = AppTheme.colors.brown, ringWidth = 1.dp, description = "반려견 사진")
            CameraBadge(Modifier.align(Alignment.BottomEnd).size(40.dp))
        }
    }
}

/** bg_edit_area: the light-grey outlined box the original keeps light even in night mode. */
@Composable
private fun BoxField(value: String, onChange: (String) -> Unit, hint: String, tag: String, modifier: Modifier = Modifier,
                     keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, minHeight: Int = 0,
                     readOnly: Boolean = false) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.padding(start = 20.dp, top = 10.dp, end = 20.dp).fillMaxWidth().heightIn(min = minHeight.dp)
        .background(colors.lightGrey, shape).border(1.dp, colors.grey, shape).padding(16.dp)) {
        if (value.isEmpty() && !readOnly) Text(hint, style = AppText.caption, color = colors.brown)
        BasicTextField(value, onChange, Modifier.fillMaxWidth().testTag(tag), readOnly = readOnly, singleLine = singleLine,
            textStyle = AppText.caption.copy(color = colors.blackFix), cursorBrush = SolidColor(colors.brown),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard))
    }
}

@Composable
private fun GenderChip(text: String, selected: Boolean, tag: String, onClick: (() -> Unit)?) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(Modifier.width(84.dp).height(50.dp).background(if (selected) colors.softOrange else colors.white, shape)
        .border(1.dp, colors.grey, shape)
        .then(if (onClick != null) Modifier.clickable(role = Role.RadioButton, onClick = onClick) else Modifier)
        .testTag(tag), contentAlignment = Alignment.Center) {
        Text(text, style = AppText.caption, color = colors.blackWhite)
    }
}

/** fragment_detail_pet: the read-only view; "수정하기" opens the form. */
@Composable
fun PetDetailScreen(dog: DogProfile?, onBack: () -> Unit, onEdit: () -> Unit) {
    val colors = AppTheme.colors
    Column(Modifier.fillMaxSize().background(colors.white)) {
        AppBar("반려견 정보", onBack = onBack, trailing = {
            if (dog != null) BarAction("수정하기", "pet-edit", colors.text, onEdit)
        })
        if (dog == null) return@Column
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 36.dp)) {
            PhotoSlot(null)
            FieldLabel("이름*", 40)
            Text(dog.name, style = AppText.caption, color = colors.text, modifier = Modifier.padding(start = 20.dp, top = 10.dp))
            Box(Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp).fillMaxWidth().height(1.dp).background(colors.brownTrans))
            FieldLabel("성별*", 30)
            Row(Modifier.padding(top = 8.dp)) {
                Box(Modifier.padding(start = 20.dp)) { GenderChip("남아", dog.gender == DogGender.Male, "gender-male", null) }
                Box(Modifier.padding(start = 20.dp)) { GenderChip("여아", dog.gender == DogGender.Female, "gender-female", null) }
            }
            FieldLabel("나이", 30)
            BoxField(dog.age?.toString().orEmpty(), {}, "", "pet-age", readOnly = true)
            FieldLabel("견종", 30)
            BoxField(dog.breed.orEmpty(), {}, "", "pet-breed", readOnly = true)
            FieldLabel("메모", 30)
            BoxField(dog.memo.orEmpty(), {}, "", "pet-memo", singleLine = false, minHeight = 120, readOnly = true)
        }
    }
}

/**
 * activity_add / activity_edit_pet: name*, gender*, age, breed, memo and a submit button pinned
 * to the bottom. [initial] is null when adding. Only the name is required, as in the original.
 */
@Composable
fun PetFormScreen(
    initial: DogProfile?,
    busy: Boolean,
    onBack: () -> Unit,
    onSubmit: (PetDraft) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val toast = LocalToast.current
    var name by rememberSaveable(initial?.userId) { mutableStateOf(initial?.name.orEmpty()) }
    var female by rememberSaveable(initial?.userId) { mutableStateOf(initial?.gender == DogGender.Female) }
    var age by rememberSaveable(initial?.userId) { mutableStateOf(initial?.age?.toString().orEmpty()) }
    var breed by rememberSaveable(initial?.userId) { mutableStateOf(initial?.breed.orEmpty()) }
    var memo by rememberSaveable(initial?.userId) { mutableStateOf(initial?.memo.orEmpty()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val editing = initial != null
    Column(Modifier.fillMaxSize().background(colors.white)) {
        AppBar("반려견 정보", onBack = onBack, trailing = {
            if (editing && onDelete != null) BarAction("삭제하기", "pet-delete", colors.text) { confirmDelete = true }
        })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            PhotoSlot { toast.show("사진 등록은 아직 연결되지 않았어요.") }
            FieldLabel("이름*", 40)
            Box(Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp).fillMaxWidth()) {
                if (name.isEmpty()) Text("이름", style = AppText.caption, color = colors.brownTrans,
                    modifier = Modifier.padding(vertical = 12.dp))
                BasicTextField(name, { name = it.take(MAX_NAME) }, Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    .testTag("dog-name-field"), singleLine = true, cursorBrush = SolidColor(colors.brownTrans),
                    textStyle = AppText.caption.copy(color = colors.text))
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp).background(colors.brownTrans))
            }
            FieldLabel("성별*", 30)
            Row(Modifier.padding(top = 10.dp)) {
                Box(Modifier.padding(start = 20.dp)) { GenderChip("남아", !female, "gender-male") { female = false } }
                Box(Modifier.padding(start = 20.dp)) { GenderChip("여아", female, "gender-female") { female = true } }
            }
            FieldLabel("나이", 30)
            BoxField(age, { age = it.filter(Char::isDigit).take(2) }, "나이를 입력해주세요.", "dog-age-field", keyboard = KeyboardType.Number)
            FieldLabel("견종", 30)
            BoxField(breed, { breed = it.take(MAX_BREED) }, "견종을 입력해주세요", "dog-breed-field")
            FieldLabel("메모", 30)
            BoxField(memo, { memo = it }, "반려견의 성격이나 메모를 입력해주세요.", "dog-memo-field", singleLine = false, minHeight = 120)
            Box(Modifier.height(20.dp))
        }
        // The add button is grey until a name exists; both buttons stay tappable and explain themselves.
        val ready = name.isNotBlank()
        BrownButton(if (busy) "저장 중…" else if (editing) "수정 완료" else "완료",
            onClick = {
                if (name.isBlank()) toast.show("이름은 필수 작성 항목입니다!")
                else onSubmit(PetDraft(name.trim(), if (female) DogGender.Female else DogGender.Male,
                    age.toIntOrNull(), breed.trim().ifEmpty { null }, memo.trim().ifEmpty { null }))
            },
            modifier = Modifier.padding(20.dp).fillMaxWidth().testTag("primary-action"),
            shape = RoundedCornerShape(8.dp), textStyle = AppText.bodyBold, enabled = !busy,
            color = if (ready) colors.brown else colors.grey)
    }
    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false },
            text = { Text("정말 삭제하시겠습니까?") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete?.invoke() }) { Text("확인") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } })
    }
}
