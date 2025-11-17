package com.dmitry.aspclassic.parser

import com.dmitry.aspclassic.psi.AspBlock
import com.dmitry.aspclassic.psi.AspContent
import com.dmitry.aspclassic.psi.impl.AspBlockImpl
import com.dmitry.aspclassic.psi.impl.AspContentImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

/**
 * Factory for creating ASP PSI elements
 */
object AspPsiElementFactory {

    fun createElement(node: ASTNode): PsiElement {
        return when (node.elementType) {
            AspTokenType.HTML_CONTENT -> AspContentImpl(node)
            AspTokenType.ASP_OPEN_TAG, AspTokenType.ASP_CLOSE_TAG, AspTokenType.ASP_SCRIPT_CONTENT ->
                AspBlockImpl(node)
            else -> AspBlockImpl(node) // fallback
        }
    }
}