package com.dmitry.aspclassic.parser

import com.dmitry.aspclassic.AspLanguage
import com.intellij.psi.tree.IElementType

/**
 * ASP element types for AST nodes
 */
class AspElementType(debugName: String) : IElementType(debugName, AspLanguage.INSTANCE) {
    companion object {
        val HTML_CONTENT = AspElementType("HTML_CONTENT")
        val ASP_OPEN_TAG = AspElementType("ASP_OPEN_TAG")
        val ASP_CLOSE_TAG = AspElementType("ASP_CLOSE_TAG")
        val ASP_SCRIPT_CONTENT = AspElementType("ASP_SCRIPT_CONTENT")
    }
}