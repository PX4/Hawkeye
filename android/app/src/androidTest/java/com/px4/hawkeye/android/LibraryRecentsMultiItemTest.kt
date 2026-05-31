package com.px4.hawkeye.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.px4.hawkeye.feature.replay.data.db.LibraryEntryEntity
import com.px4.hawkeye.feature.replay.data.db.ReplayLibraryDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Seeds several library entries directly into the real Room DAO (exercising the live
 * `ORDER BY imported_at_millis DESC` query), then verifies multi-item behavior on a
 * device: the Home peek shows only the three newest while the Replay library shows every
 * entry, and the library keeps its items across rotation.
 *
 * Uses an empty compose rule so seeding happens before the activity launches, making the
 * first library emission deterministic. The DAO is resolved from the app's Koin graph.
 */
@RunWith(AndroidJUnit4::class)
class LibraryRecentsMultiItemTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val dao: ReplayLibraryDao get() = GlobalContext.get().get()

    @Before
    fun seed() = runBlocking {
        clearLibrary()
        SEED.forEach { dao.insert(it) }
    }

    @After
    fun cleanup() = runBlocking { clearLibrary() }

    private suspend fun clearLibrary() {
        dao.observeAll().first().forEach { dao.deleteById(it.id) }
    }

    @Test
    fun homeShowsTheThreeNewestRecents() {
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText(NEWEST)
            composeRule.onNodeWithText(NEWEST).assertIsDisplayed()
            composeRule.onNodeWithText(SECOND).assertIsDisplayed()
            composeRule.onNodeWithText(THIRD).assertIsDisplayed()
            // The fourth (oldest) entry is beyond the three-item Home peek.
            composeRule.onNodeWithText(OLDEST).assertDoesNotExist()
        }
    }

    @Test
    fun replayLibraryShowsEveryItem() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText(HOME_REPLAY_CARD).performClick()
            awaitText(OLDEST)
            listOf(NEWEST, SECOND, THIRD, OLDEST).forEach {
                composeRule.onNodeWithText(it).assertIsDisplayed()
            }
        }
    }

    @Test
    fun replayLibraryKeepsItemsAcrossRotation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.onNodeWithText(HOME_REPLAY_CARD).performClick()
            awaitText(OLDEST)

            scenario.recreate()
            composeRule.waitForIdle()

            awaitText(OLDEST)
            composeRule.onNodeWithText(NEWEST).assertIsDisplayed()
        }
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val NEWEST = "newest.ulg"
        const val SECOND = "second.ulg"
        const val THIRD = "third.ulg"
        const val OLDEST = "oldest.ulg"
        const val HOME_REPLAY_CARD = "Replay a flight"

        // Same payload, different names/timestamps. Newest first by imported-at millis.
        val SEED = listOf(
            LibraryEntryEntity("e1", NEWEST, 1024L, 40L, "e1.ulg"),
            LibraryEntryEntity("e2", SECOND, 1024L, 30L, "e2.ulg"),
            LibraryEntryEntity("e3", THIRD, 1024L, 20L, "e3.ulg"),
            LibraryEntryEntity("e4", OLDEST, 1024L, 10L, "e4.ulg"),
        )
    }
}
