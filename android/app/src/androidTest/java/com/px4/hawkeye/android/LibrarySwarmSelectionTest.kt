package com.px4.hawkeye.android

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.px4.hawkeye.feature.replay.data.db.LibraryEntryEntity
import com.px4.hawkeye.feature.replay.data.db.ReplayLibraryDao
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * End-to-end library multi-select on a device: seeds real Room rows plus their on-disk
 * payloads, walks the Select -> check -> "Play together" flow through the live UI, and
 * verifies the staged inbox the native renderer reads (current.ulg + swarm_1.ulg in click
 * order, .ready carrying the batch count).
 */
@RunWith(AndroidJUnit4::class)
class LibrarySwarmSelectionTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val dao: ReplayLibraryDao get() = GlobalContext.get().get()
    private val filesDir: File =
        ApplicationProvider.getApplicationContext<Context>().filesDir

    private val robot by lazy { LibrarySelectionRobot(composeRule) }

    @Before
    fun seed() {
        runBlocking {
            clearLibrary()
            val libraryDir = File(filesDir, "library").apply { mkdirs() }
            SEED.forEach { (entity, payload) ->
                File(libraryDir, entity.fileName).writeText(payload)
                dao.insert(entity)
            }
        }
    }

    @After
    fun cleanup() {
        runBlocking {
            clearLibrary()
            SEED.forEach { (entity, _) -> File(File(filesDir, "library"), entity.fileName).delete() }
            File(filesDir, "inbox").listFiles()?.forEach { it.delete() }
        }
    }

    private suspend fun clearLibrary() {
        dao.observeAll().first().forEach { dao.deleteById(it.id) }
    }

    @Test
    fun selectionModeShowsCountAndPlayTogetherCta() {
        ActivityScenario.launch(MainActivity::class.java).use {
            robot
                .openLibrary()
                .enterSelectionMode()
                .clickEntry(ALPHA)
                .assertTitle("1 selected")
                .clickEntry(BRAVO)
                .assertTitle("2 selected")
                .assertCta("Play together (2)")
        }
    }

    @Test
    fun playTogetherStagesTheBatchInClickOrder() {
        ActivityScenario.launch(MainActivity::class.java).use {
            robot
                .openLibrary()
                .enterSelectionMode()
                .clickEntry(BRAVO) // clicked first: becomes drone 0 / current.ulg
                .clickEntry(ALPHA)
                .clickCta("Play together (2)")

            val inbox = File(filesDir, "inbox")
            val ready = File(inbox, ".ready")
            composeRule.waitUntil(timeoutMillis = 10_000) {
                ready.exists() && ready.readText().endsWith(" 2")
            }
            assertEquals(BRAVO_PAYLOAD, File(inbox, "current.ulg").readText())
            assertEquals(ALPHA_PAYLOAD, File(inbox, "swarm_1.ulg").readText())
            assertFalse(File(inbox, "swarm_2.ulg").exists())
            assertTrue(ready.readText().endsWith(" 2"))
        }
    }

    @Test
    fun closingSelectionModeRestoresTheLibraryChrome() {
        ActivityScenario.launch(MainActivity::class.java).use {
            robot
                .openLibrary()
                .enterSelectionMode()
                .clickEntry(ALPHA)
                .exitSelectionMode()
                .assertTitle("Replay library")
                .assertCta("Open file")
        }
    }

    @Test
    fun systemBackExitsSelectionModeBeforeLeavingTheLibrary() {
        ActivityScenario.launch(MainActivity::class.java).use {
            robot
                .openLibrary()
                .enterSelectionMode()
                .clickEntry(ALPHA)
                .pressBack()
                .assertTitle("Replay library")
                .assertCta("Open file")
        }
    }

    private companion object {
        const val ALPHA = "swarm_alpha.ulg"
        const val BRAVO = "swarm_bravo.ulg"
        const val CHARLIE = "swarm_charlie.ulg"
        const val ALPHA_PAYLOAD = "payload-alpha"
        const val BRAVO_PAYLOAD = "payload-bravo"

        // Distinct payloads so the staged-order assertion can tell the files apart.
        val SEED = listOf(
            LibraryEntryEntity("s1", ALPHA, 13L, 30L, "s1.ulg") to ALPHA_PAYLOAD,
            LibraryEntryEntity("s2", BRAVO, 13L, 20L, "s2.ulg") to BRAVO_PAYLOAD,
            LibraryEntryEntity("s3", CHARLIE, 13L, 10L, "s3.ulg") to "payload-charlie",
        )
    }
}

private class LibrarySelectionRobot(private val rule: ComposeTestRule) {

    fun openLibrary() = apply {
        awaitText("Replay a flight")
        rule.onNodeWithText("Replay a flight").performClick()
        awaitText("Select")
    }

    fun enterSelectionMode() = apply {
        rule.onNodeWithText("Select").performClick()
        awaitText("0 selected")
    }

    fun exitSelectionMode() = apply {
        rule.onNodeWithContentDescription("Exit selection").performClick()
    }

    fun pressBack() = apply {
        rule.waitForIdle()
        androidx.test.espresso.Espresso.pressBack()
        rule.waitForIdle()
    }

    fun clickEntry(displayName: String) = apply {
        rule.onNodeWithText(displayName).performClick()
    }

    fun clickCta(label: String) = apply {
        awaitText(label)
        rule.onNodeWithText(label).performClick()
    }

    fun assertTitle(title: String) = apply {
        rule.onNodeWithText(title).assertIsDisplayed()
    }

    fun assertCta(label: String) = apply {
        rule.onNodeWithText(label).assertIsDisplayed()
    }

    private fun awaitText(text: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
