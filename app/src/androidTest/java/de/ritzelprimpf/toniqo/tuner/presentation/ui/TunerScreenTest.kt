package de.ritzelprimpf.toniqo.tuner.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus
import de.ritzelprimpf.toniqo.tuner.fakes.FakeTunerScreenViewModel
import de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerUiState
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TunerScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun launchScreen(state: TunerUiState): FakeTunerScreenViewModel {
        val fake = FakeTunerScreenViewModel(state)
        composeTestRule.setContent {
            ToniqoTheme { TunerScreen(viewModel = fake) }
        }
        return fake
    }

    @Test
    fun `idle state renders LISTENING status`() {
        launchScreen(TunerUiState(status = TuningStatus.LISTENING))
        composeTestRule.onNodeWithText("LISTENING").assertIsDisplayed()
    }

    @Test
    fun `flat detection renders status word and cents`() {
        launchScreen(TunerUiState(status = TuningStatus.FLAT, centsOffTarget = -18.0))
        composeTestRule.onNodeWithText("FLAT").assertIsDisplayed()
        composeTestRule.onNodeWithText("−18¢").assertIsDisplayed()
    }

    @Test
    fun `permission denied renders card heading and button`() {
        launchScreen(TunerUiState(status = TuningStatus.PERMISSION_DENIED))
        composeTestRule.onNodeWithText("Microphone access needed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant access").assertIsDisplayed()
    }
}
