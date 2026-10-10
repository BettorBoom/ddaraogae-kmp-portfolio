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
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("screen-title").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        compose.onNode(hasText("개발용 예시 데이터", substring = true)).assertIsDisplayed()
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

    @Test fun matrixHasNoClippedTextAndActionsRemainReachable() {
        File(context.getExternalFilesDir(null), "m05").listFiles()?.forEach { it.delete() }
        for (screen in listOf("home", "walk", "record")) {
            for (state in listOf("ready", "loading", "empty", "error", "denied", "long")) {
                for (dark in listOf(false, true)) {
                    launch(screen, state, dark).use {
                        ready()
                        capture("$screen-$state-${if (dark) "dark" else "light"}-2x")
                        val texts = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult))
                        for (index in texts.fetchSemanticsNodes().indices) {
                            val results = mutableListOf<TextLayoutResult>()
                            texts[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
                            // A paragraph retains its parent's max width while Text
                            // can shrink-wrap. Check laid-out lines, not that box (ADR-006).
                            results.forEach { result ->
                                val overflow = (0 until result.lineCount).any { line ->
                                    result.isLineEllipsized(line) || result.getLineLeft(line) < -1f ||
                                        result.getLineRight(line) > result.size.width + 1f || result.getLineBottom(line) > result.size.height + 1f
                                }
                                assertFalse("$screen/$state/$dark text clipped: ${result.layoutInput.text}; size=${result.size}, lineRight=${result.getLineRight(0)}, paragraph=${result.multiParagraph.width}x${result.multiParagraph.height}", overflow)
                            }
                        }
                        compose.onNodeWithTag("primary-action").performScrollTo().assertIsDisplayed()
                        if (screen == "walk" && state in listOf("loading", "error", "denied"))
                            compose.onNodeWithTag("primary-action").assertIsNotEnabled()
                    }
                }
            }
        }
        for (screen in listOf("home", "walk", "record")) {
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
        for (screen in listOf("home", "walk", "record")) launch(screen, "error", scale = 1f).use {
            ready()
            compose.onNodeWithText("다시 시도").performScrollTo().performClick()
            compose.onNodeWithText("불러오지 못했어요").assertDoesNotExist()
        }
        launch("walk", "ready", scale = 1f).use {
            ready()
            compose.onNodeWithTag("primary-action").performScrollTo().performClick()
            compose.onNodeWithText("산책 시작하기").assertExists()
        }
    }
}
