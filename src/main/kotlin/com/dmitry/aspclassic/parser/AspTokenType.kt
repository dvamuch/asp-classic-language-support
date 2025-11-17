package com.dmitry.aspclassic.parser

import com.intellij.psi.tree.IElementType
import com.dmitry.aspclassic.AspLanguage

/**
 * ASP token types
 */
class AspTokenType(debugName: String) : IElementType(debugName, AspLanguage.INSTANCE) {
    companion object {
        // ASP tags
        @JvmStatic
        val ASP_OPEN_TAG = AspTokenType("ASP_OPEN_TAG")  // <%
        @JvmStatic
        val ASP_CLOSE_TAG = AspTokenType("ASP_CLOSE_TAG") // %>

        // ASP content inside tags
        @JvmStatic
        val ASP_SCRIPT_CONTENT = AspTokenType("ASP_SCRIPT_CONTENT") // VBScript code inside <% %>

        // HTML and text content outside ASP tags
        @JvmStatic
        val HTML_CONTENT = AspTokenType("HTML_CONTENT")
    }
}