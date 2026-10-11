package dev.ddaraogae.app

import android.content.Intent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class ScenarioTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun launch(screen: String, state: String, dark: Boolean = false, scale: Float = 2f) =
        ActivityScenario.launch<ScenarioActivity>(Intent(context, ScenarioActivity::class.java)
            .putExtra("screen", screen).putExtra("state", state).putExtra("dark", dark).putExtra("fontScale", scale))

    private fun ready() {
        // Every scenario screen carries the fixed fixture banner; wait for it, not a per-screen title.
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("개발용 예시 데이터", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        compose.onNode(hasText("개발용 예시 데이터", substring = true)).assertIsDisplayed()
    }

    // Screens that fit on one page have no scroll container, so scroll only when needed.
    private fun reach(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (runCatching { node.assertIsDisplayed() }.isFailure) node.performScrollTo()
        node.assertIsDisplayed()
    }

    private fun capture(name: String) {
        // Compose idling does not cover native window transitions/accessibility.
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(500, 10_000)
        val directory = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: context.getExternalFilesDir(null)!!.absolutePath, "m05").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            // Artifacts include the whole window and native system bars.
            requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertNoClippedText(label: String) {
        // A label the original ellipsizes on purpose is tagged and skipped; everything else must fit.
        val texts = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult) and !hasTestTag("ellipsized-by-design"))
        for (index in texts.fetchSemanticsNodes().indices) {
            val results = mutableListOf<TextLayoutResult>()
            texts[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            // A centered Text keeps its parent's max width as the paragraph width while the node
            // shrink-wraps, so lines are compared with the paragraph, not the node (ADR-006).
            results.forEach { result ->
                val overflow = (0 until result.lineCount).any { line ->
                    result.isLineEllipsized(line) || result.getLineLeft(line) < -1f ||
                        result.getLineRight(line) > result.multiParagraph.width + 1f || result.getLineBottom(line) > result.size.height + 1f
                }
                assertFalse("$label text clipped: ${result.layoutInput.text}; size=${result.size}, lineRight=${result.getLineRight(0)}, paragraph=${result.multiParagraph.width}x${result.multiParagraph.height}", overflow)
            }
        }
    }

    // One reachable control per screen (ADR-013 adds screens beyond the first three).
    private val keyControl = mapOf("home" to "primary-action", "walk" to "primary-action", "finish" to "primary-action",
        "history" to "primary-action", "stamps" to "stamp-open-1", "stamp-detail" to "back", "mypage" to "mypage-dogs",
        "pets" to "pet-add", "pet-form" to "primary-action", "alarm" to "primary-action", "login" to "primary-action",
        "signup" to "primary-action")

    @Test fun matrixHasNoClippedTextAndActionsRemainReachable() {
        File(context.getExternalFilesDir(null), "m05").listFiles()?.forEach { it.delete() }
        for (screen in listOf("home", "walk", "finish")) {
            for (state in listOf("ready", "loading", "empty", "error", "denied", "long")) {
                for (dark in listOf(false, true)) {
                    launch(screen, state, dark).use {
                        ready()
                        capture("$screen-$state-${if (dark) "dark" else "light"}-2x")
                        assertNoClippedText("$screen/$state/$dark")
                        reach(keyControl.getValue(screen))
                        if (screen == "walk" && state in listOf("loading", "error", "denied"))
                            compose.onNodeWithTag("primary-action").assertIsNotEnabled()
                    }
                }
            }
        }
        // Screens added by ADR-013: one default capture and the worst case (dark, 2x text, long names).
        for (screen in keyControl.keys - setOf("home", "walk", "finish")) {
            launch(screen, "ready", scale = 1f).use {
                ready(); capture("$screen-ready-light-1x")
                reach(keyControl.getValue(screen))
            }
            launch(screen, "long", dark = true).use {
                ready()
                capture("$screen-long-dark-2x")
                assertNoClippedText("$screen/long/dark")
                reach(keyControl.getValue(screen))
            }
        }
        for (screen in listOf("home", "walk", "finish")) {
            val original = launch(screen, "ready", scale = 1f).use {
                ready(); capture("$screen-ready-light-1x")
                compose.onRoot().captureToImage().asAndroidBitmap()
            }
            launch(screen, "ready", scale = 1f).use {
                ready()
                assertTrue("$screen changed on identical rerender", original.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
            }
        }
    }

    @Test fun retryAndWalkActionsChangeTheDisplayedState() {
        for (screen in listOf("home", "walk", "finish")) launch(screen, "error", scale = 1f).use {
            ready()
            val retry = compose.onNodeWithText("다시 시도")
            if (runCatching { retry.assertIsDisplayed() }.isFailure) retry.performScrollTo()
            retry.performClick()
            compose.onNodeWithText("불러오지 못했어요").assertDoesNotExist()
        }
        launch("walk", "ready", scale = 1f).use {
            ready()
            // The ready walk is in progress; the stop button ends it and the start pill returns.
            reach("primary-action"); compose.onNodeWithTag("primary-action").performClick()
            compose.onNodeWithText("산책하기").assertExists()
        }
    }

    @Test fun stampDialogOpensFromFinishAndClosesToTheGallery() {
        launch("finish", "ready", scale = 1f).use {
            ready()
            reach("primary-action")
            compose.onNodeWithTag("primary-action").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("오늘 모은 산책 도장").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("stamp-done").performClick()
            compose.onNodeWithText("화면 갤러리").assertExists()
        }
    }
}
