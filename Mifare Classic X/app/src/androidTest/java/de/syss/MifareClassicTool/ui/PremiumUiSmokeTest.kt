package de.syss.MifareClassicTool.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import de.syss.MifareClassicTool.ui.adminmode.AdminHubScreen
import de.syss.MifareClassicTool.ui.auth.LockScreen
import de.syss.MifareClassicTool.ui.components.NfcProgressStepper
import de.syss.MifareClassicTool.ui.onboarding.OnboardingScreen
import de.syss.MifareClassicTool.ui.theme.MctxTheme
import org.junit.Rule
import org.junit.Test

class PremiumUiSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboarding_exposesBrandAndPrimaryAction() {
        composeRule.setContent {
            MctxTheme(darkTheme = true) { OnboardingScreen(onFinish = {}) }
        }

        composeRule.onNodeWithText("MiFareX").assertIsDisplayed()
        composeRule.onNodeWithText("Continua").assertIsDisplayed()
    }

    @Test
    fun lockScreen_explainsProtectedAccess() {
        composeRule.setContent {
            MctxTheme(darkTheme = true) { LockScreen(onUnlockClick = {}) }
        }

        composeRule.onNodeWithText("Conferma la tua identità").assertIsDisplayed()
        composeRule.onNodeWithText("Sblocca con biometria").assertIsDisplayed()
    }

    @Test
    fun adminHub_keepsExpertModeSeparate() {
        composeRule.setContent {
            MctxTheme {
                AdminHubScreen({}, {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithText("Control Center").assertIsDisplayed()
        composeRule.onNodeWithText("Nuovo vendor").assertIsDisplayed()
        composeRule.onNodeWithText("Expert Mode resta separata", substring = true).assertIsDisplayed()
    }

    @Test
    fun writeStepper_namesAllSafetyStages() {
        composeRule.setContent {
            MctxTheme { NfcProgressStepper(activeStep = 1) }
        }

        composeRule.onNodeWithText("Verifica").assertIsDisplayed()
        composeRule.onNodeWithText("Scrittura").assertIsDisplayed()
        composeRule.onNodeWithText("Conferma").assertIsDisplayed()
    }
}
