package com.dmitry.aspclassic.psi

import com.dmitry.aspclassic.AspLanguage
import com.intellij.psi.tree.IElementType

/**
 * ASP token type
 */
class AspTokenType(debugName: String) : IElementType(debugName, AspLanguage.INSTANCE) {
    override fun toString(): String = "AspTokenType.${super.toString()}"
}

