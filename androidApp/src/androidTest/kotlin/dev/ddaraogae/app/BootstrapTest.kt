package dev.ddaraogae.app

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BootstrapTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun expect(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        val node = compose.onNodeWithText(text)
        if (runCatching { node.assertIsDisplayed() }.isFailure) node.performScrollTo()
        node.assertIsDisplayed()
    }

    private fun tap(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        val node = compose.onNodeWithText(text)
        if (runCatching { node.assertIsDisplayed() }.isFailure) node.performScrollTo()
        node.assertIsDisplayed().assertIsEnabled().performClick()
    }

    @Test fun walkSurvivesNavigationAndActivityRecreation() {
        // This test owns app navigation; a separate permission probe covers the OS dialog.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.grantRuntimePermission("dev.ddaraogae.app", Manifest.permission.ACCESS_COARSE_LOCATION)
        automation.grantRuntimePermission("dev.ddaraogae.app", Manifest.permission.ACCESS_FINE_LOCATION)
        if (compose.onAllNodesWithText("반려견 이름 등록하기").fetchSemanticsNodes().isNotEmpty()) {
            tap("반려견 이름 등록하기")
            compose.onNodeWithText("반려견 이름").performTextInput("보리")
            tap("이름 저장하기")
            expect("반려견 이름 바꾸기")
        }
        tap("산책하러 가기")
        compose.waitUntil(10_000) {
            runCatching {
                compose.onAllNodesWithText("산책 시작하기").fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodesWithText("산책 마치기").fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        if (compose.onAllNodesWithText("산책 마치기").fetchSemanticsNodes().isNotEmpty()) tap("산책 마치기")
        tap("산책 시작하기")
        expect("산책 마치기")
        tap("홈으로")
        tap("산책하러 가기")
        expect("산책 마치기")
        compose.activityRule.scenario.recreate()
        expect("산책 마치기")
        tap("산책 마치기")
        tap("산책 기록 보기")
        expect("함께한 시간을 저장했어요.")
        tap("산책 목록으로")
        expect("1번째 산책 자세히 보기")
        tap("홈으로")
        expect("산책하러 가기")
    }
}
