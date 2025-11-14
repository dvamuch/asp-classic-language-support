package com.dmitry.aspclassic.psi

import com.dmitry.aspclassic.AspLanguage
import com.intellij.psi.tree.IElementType

/**
 * ASP PSI element type
 */
class AspElementType(debugName: String) : IElementType(debugName, AspLanguage.INSTANCE)

