package com.dmitry.aspclassic.parser

import com.dmitry.aspclassic.psi.AspBlock
import com.dmitry.aspclassic.psi.AspContent
import com.dmitry.aspclassic.psi.impl.AspBlockImpl
import com.dmitry.aspclassic.psi.impl.AspContentImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

object AspPsiElementFactory {

    fun createElement(node: ASTNode): PsiElement {
        println("PSI_FACTORY: Creating element for: ${node.elementType} '${node.text.take(30)}...'")

        return when (node.elementType) {
            AspTokenType.HTML_CONTENT -> {
                println("PSI_FACTORY: Creating AspContent for HTML")
                AspContentImpl(node)
            }
            AspTokenType.ASP_OPEN_TAG,
            AspTokenType.ASP_CLOSE_TAG,
            AspTokenType.ASP_SCRIPT_CONTENT -> {
                println("PSI_FACTORY: Creating AspBlock for ASP token")
                AspBlockImpl(node)
            }
            else -> {
                println("PSI_FACTORY: Unknown element type, fallback to AspBlock")
                AspBlockImpl(node)
            }
        }
    }
}