package dev.ddaraogae.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ddaraogae.designsystem.resources.Res
import dev.ddaraogae.designsystem.resources.legacy_nav_home
import dev.ddaraogae.designsystem.resources.legacy_nav_mypage
import dev.ddaraogae.designsystem.resources.legacy_nav_walk
import org.jetbrains.compose.resources.painterResource

enum class LegacyTab(val label: String, val tag: String) {
    Home("홈", "tab-home"), Walk("산책", "tab-walk"), MyPage("마이페이지", "tab-mypage"),
}

@Composable
fun LegacyNavIcon(tab: LegacyTab, color: Color) {
    val drawable = when (tab) {
        LegacyTab.Home -> Res.drawable.legacy_nav_home
        LegacyTab.Walk -> Res.drawable.legacy_nav_walk
        LegacyTab.MyPage -> Res.drawable.legacy_nav_mypage
    }
    Image(painterResource(drawable), contentDescription = null,
        modifier = Modifier.size(24.dp), colorFilter = ColorFilter.tint(color))
}

/**
 * The original BottomNavigationView: 80dp white bar under a 1dp light-grey line, labeled items,
 * brown when checked and #7C7C7C otherwise, with no active-indicator pill (rec_main_nav.xml).
 */
@Composable
fun LegacyBottomBar(selected: LegacyTab?, onSelect: (LegacyTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(modifier.fillMaxWidth().background(colors.white)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.lightGrey))
        // 80dp like the original, but it grows with large text instead of clipping the label (ADR-006).
        Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).selectableGroup()) {
            LegacyTab.entries.forEach { tab ->
                val active = tab == selected
                val tint = if (active) colors.brownTrans else colors.darkGrey
                Column(Modifier.weight(1f).heightIn(min = 80.dp).testTag(tab.tag)
                    .selectable(active, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(top = 16.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
                    LegacyNavIcon(tab, tint)
                    Spacer(Modifier.height(8.dp))
                    Text(tab.label, color = tint, textAlign = TextAlign.Center,
                        style = AppText.mini.copy(fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium))
                }
            }
        }
    }
}
