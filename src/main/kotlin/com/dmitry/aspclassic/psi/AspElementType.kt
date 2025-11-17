package com.dmitry.aspclassic.psi

import com.intellij.psi.tree.IElementType
import com.dmitry.aspclassic.AspLanguage

/**
 * ASP element types for PSI tree
 */
class AspElementType(debugName: String) : IElementType(debugName, AspLanguage.INSTANCE) {
    companion object {
        // Root element of ASP file
        @JvmStatic
        val FILE = AspElementType("ASP_FILE")

        // ASP block: <% ... %>
        @JvmStatic
        val ASP_BLOCK = AspElementType("ASP_BLOCK")

        // Content outside ASP blocks (will be injected with HTML)
        @JvmStatic
        val CONTENT = AspElementType("CONTENT")
    }
}