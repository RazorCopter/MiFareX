package de.syss.MifareClassicTool.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Visually formats a hex string by adding a space every two characters.
 * E.g., "FFFFFFFFFFFF" becomes "FF FF FF FF FF FF".
 */
class HexVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Strip any existing spaces in case the raw text already has them
        val rawText = text.text.replace(" ", "")
        
        var formatted = ""
        for (i in rawText.indices) {
            formatted += rawText[i]
            // Add a space after every 2 characters, except for the last one
            if (i % 2 == 1 && i != rawText.length - 1) {
                formatted += " "
            }
        }
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // For every 2 characters in the original text, there is 1 space added
                // We need to map the original cursor offset (without spaces) to the transformed offset (with spaces)
                
                // Cap the offset to rawText length to avoid out of bounds
                val safeOffset = offset.coerceAtMost(rawText.length)
                
                // Number of spaces added before this offset
                val spacesBefore = safeOffset / 2
                
                val actualSpaces = if (safeOffset > 0 && safeOffset % 2 == 0 && safeOffset == rawText.length) {
                    spacesBefore - 1
                } else {
                    spacesBefore
                }
                
                return safeOffset + actualSpaces.coerceAtLeast(0)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceAtMost(formatted.length)
                var spacesCount = 0
                for (i in 0 until safeOffset) {
                    if (formatted[i] == ' ') spacesCount++
                }
                
                return safeOffset - spacesCount
            }
        }
        
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
