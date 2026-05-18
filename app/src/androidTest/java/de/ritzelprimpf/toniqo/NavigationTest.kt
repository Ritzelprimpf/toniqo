package de.ritzelprimpf.toniqo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.ritzelprimpf.toniqo.ui.MainScreen
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that all five top-level destinations are reachable via the bottom
 * navigation bar, and that the Info nested graph's start screen is shown when
 * the More tab is tapped.
 *
 * Uses [createComposeRule] (not [createAndroidComposeRule]) so the test does
 * not require Hilt instrumented-test setup. Phase 4 placeholder screens have
 * no DI dependencies.
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allFiveBottomNavDestinationsAreReachable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            ToniqoTheme {
                MainScreen()
            }
        }

        // Tuner is the start destination — its title should be visible immediately.
        composeRule
            .onNodeWithText(context.getString(R.string.tuner_title))
            .assertIsDisplayed()

        // Tap Metronome tab via content description (robust to label text changes).
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.nav_cd_metronome))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.metronome_title))
            .assertIsDisplayed()

        // Tap Key Finder tab.
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.nav_cd_keyfinder))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.keyfinder_title))
            .assertIsDisplayed()

        // Tap Chord Finder tab.
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.nav_cd_chordfinder))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.chordfinder_title))
            .assertIsDisplayed()

        // Tap More (Info) tab — InfoHomeScreen shows the "Info" heading.
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.nav_cd_more))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.info_title))
            .assertIsDisplayed()
    }
}
