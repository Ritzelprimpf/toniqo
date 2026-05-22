package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BpmInputDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchDialog(
        initialBpm: Int = 120,
        onConfirm: (Int) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ToniqoTheme {
                BpmInputDialog(
                    initialBpm = initialBpm,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss,
                )
            }
        }
    }

    @Test
    fun `initial BPM pre-populates the text field`() {
        launchDialog(initialBpm = 120)
        composeTestRule.onNodeWithText("120").assertExists()
    }

    @Test
    fun `OK enabled for valid value 1`() {
        launchDialog(initialBpm = 120)
        composeTestRule.onNodeWithText("120").performTextReplacement("1")
        composeTestRule.onNodeWithText("OK").assertIsEnabled()
    }

    @Test
    fun `OK enabled for valid value 300`() {
        launchDialog(initialBpm = 120)
        composeTestRule.onNodeWithText("120").performTextReplacement("300")
        composeTestRule.onNodeWithText("OK").assertIsEnabled()
    }

    @Test
    fun `OK disabled when field is empty`() {
        launchDialog(initialBpm = 120)
        composeTestRule.onNodeWithText("120").performTextReplacement("")
        composeTestRule.onNodeWithText("OK").assertIsNotEnabled()
    }

    @Test
    fun `OK disabled for out-of-range value 0`() {
        launchDialog(initialBpm = 120)
        composeTestRule.onNodeWithText("120").performTextReplacement("0")
        composeTestRule.onNodeWithText("OK").assertIsNotEnabled()
    }

    @Test
    fun `OK invokes onConfirm with the parsed value`() {
        var confirmed: Int? = null
        launchDialog(initialBpm = 120, onConfirm = { confirmed = it })
        composeTestRule.onNodeWithText("120").performTextReplacement("90")
        composeTestRule.onNodeWithText("OK").performClick()
        assertEquals(90, confirmed)
    }

    @Test
    fun `Cancel invokes onDismiss`() {
        var dismissed = false
        launchDialog(onDismiss = { dismissed = true })
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertEquals(true, dismissed)
    }
}
