package com.dmitry.aspclassic.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.dmitry.aspclassic.psi.AspContent
import com.dmitry.aspclassic.psi.AspContentTextEscaper

/**
 * Implementation of content PSI element
 */
class AspContentImpl(node: ASTNode) : ASTWrapperPsiElement(node), AspContent {

    override fun isValidHost(): Boolean = true

    override fun updateText(text: String): PsiLanguageInjectionHost {
        // For now, we don't support modifying ASP content through injection
        // This would require creating a new PSI element with the updated text
        throw UnsupportedOperationException("ASP content modification not supported")
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        return AspContentTextEscaper(this)
    }
}