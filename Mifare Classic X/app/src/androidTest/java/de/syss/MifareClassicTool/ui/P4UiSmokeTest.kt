package de.syss.MifareClassicTool.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.syss.MifareClassicTool.data.model.OperationLogEntity
import de.syss.MifareClassicTool.data.model.OperationOutcome
import de.syss.MifareClassicTool.data.model.OperationSource
import de.syss.MifareClassicTool.data.model.OperationType
import de.syss.MifareClassicTool.ui.history.OperationHistoryContent
import de.syss.MifareClassicTool.ui.theme.MctxTheme
import org.junit.Rule
import org.junit.Test

class P4UiSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyHistoryExplainsWhatWillAppear() {
        composeRule.setContent {
            MctxTheme { OperationHistoryContent(entries = emptyList()) }
        }

        composeRule.onNodeWithText("Audit locale").assertIsDisplayed()
        composeRule.onNodeWithText("Nessuna operazione registrata").assertIsDisplayed()
    }

    @Test
    fun historyShowsMaskedUidOnlyInExpandedDetails() {
        composeRule.setContent {
            MctxTheme {
                OperationHistoryContent(
                    entries = listOf(
                        OperationLogEntity(
                            id = "event-1",
                            timestamp = 1_700_000_000_000,
                            type = OperationType.MANUAL_WRITE,
                            outcome = OperationOutcome.SUCCESS,
                            source = OperationSource.OPERATOR,
                            vendorName = "Vendor Demo",
                            uidSuffix = "••C3D4",
                            summary = "Scrittura completata"
                        )
                    )
                )
            }
        }

        composeRule.onNodeWithText("Scrittura completata").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("UID · ••C3D4").assertIsDisplayed()
    }
}
