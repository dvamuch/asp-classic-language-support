package com.dmitry.aspclassic.parser

import com.intellij.psi.tree.IElementType
import com.dmitry.aspclassic.AspLanguage

/**
 * ASP token types
 */
class AspTokenType(debugName: String) : IElementType(debugName, AspLanguage.INSTANCE) {
    companion object {
        // ASP tags
        @JvmField
        val ASP_OPEN_TAG = AspTokenType("ASP_OPEN_TAG")  // <%

        @JvmField
        val ASP_CLOSE_TAG = AspTokenType("ASP_CLOSE_TAG") // %>

        // ASP content inside tags
        @JvmField
        val ASP_SCRIPT_CONTENT = AspTokenType("ASP_SCRIPT_CONTENT") // VBScript code inside <% %>

        // HTML and text content outside ASP tags
        @JvmField
        val HTML_CONTENT = AspTokenType("HTML_CONTENT")
    }
}