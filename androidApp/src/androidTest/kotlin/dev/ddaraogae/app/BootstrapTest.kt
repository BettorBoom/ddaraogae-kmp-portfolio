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

    private fun reveal(node: SemanticsNodeInteraction) {
        if (runCatching { node.assertIsDisplayed() }.isFailure) node.performScrollTo()
        node.assertIsDisplayed()
    }

    private fun expect(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        reveal(compose.onNodeWithText(text))
    }

    private fun expectTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        reveal(compose.onNodeWithTag(tag))
    }

    private fun expectDescription(description: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun tap(text: String) {
        expect(text)
        compose.onNodeWithText(text).assertIsEnabled().performClick()
    }

    private fun tapTag(tag: String) {
        expectTag(tag)
        compose.onNodeWithTag(tag).assertIsEnabled().performClick()
    }

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    // The DB survives between tests, so every test first makes sure the dog exists.
    private fun ensureDog() {
        compose.waitUntil(10_000) { exists("home-dog-add") || exists("home-dog") }
        if (exists("home-dog-add")) {
            tapTag("home-dog-add")
            expectTag("dog-name-field")
            compose.onNodeWithTag("dog-name-field").performTextInput("보리")
            tapTag("primary-action")
            expectTag("home-dog")
        }
    }

    @Test fun walkSurvivesNavigationAndActivityRecreation() {
        // This test owns app navigation; a separate permission probe covers the OS dialog.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.grantRuntimePermission("dev.ddaraogae.app", Manifest.permission.ACCESS_COARSE_LOCATION)
        automation.grantRuntimePermission("dev.ddaraogae.app", Manifest.permission.ACCESS_FINE_LOCATION)
        ensureDog()
        tapTag("tab-walk")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("산책하기").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithContentDescription("산책 종료").fetchSemanticsNodes().isNotEmpty()
        }
        // A walk left over from an earlier run is ended and acknowledged first.
        if (compose.onAllNodesWithContentDescription("산책 종료").fetchSemanticsNodes().isNotEmpty()) {
            tapTag("primary-action"); tap("산책 끝!")
        }
        tap("산책하기")
        expectDescription("산책 종료")
        tapTag("tab-home")
        expect("반가워요!")
        tapTag("tab-walk")
        expectDescription("산책 종료")
        compose.activityRule.scenario.recreate()
        expectDescription("산책 종료")
        tapTag("primary-action")
        expect("산책 완료")
        tap("산책 끝!")
        expect("산책하기")
        tapTag("tab-home")
        expect("금방 산책을 다녀왔어요!")
        tapTag("primary-action")
        expect("산책 모아보기")
        expect("산책기록")
        tapTag("back")
        expect("반가워요!")
    }

    @Test fun originalBottomNavigationOpensMyPageAndPetScreens() {
        ensureDog()
        tapTag("tab-mypage")
        expect("정보 수정")
        expect("나의 반려견 정보")
        tapTag("mypage-dogs")
        expectTag("pet-row")
        tapTag("pet-row")
        tapTag("pet-edit")
        expectTag("dog-name-field")
        tapTag("back")
        tapTag("back")
        tapTag("back")
        expect("정보 수정")
    }
}
