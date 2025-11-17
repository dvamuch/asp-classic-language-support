package com.dmitry.aspclassic.psi.impl

import com.dmitry.aspclassic.psi.AspContent
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost

class AspContentImpl(node: ASTNode) : ASTWrapperPsiElement(node), AspContent {

    override fun isValidHost(): Boolean {
        val text = text
        val isValid = text.isNotBlank() && !text.trim().startsWith("<%")
        println("AspContentImpl.isValidHost: $isValid for text: '${text.take(30)}...'")
        return isValid
    }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        println("AspContentImpl.updateText: '${text.take(30)}...'")
        return this
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        return LiteralTextEscaper.createSimple(this)
    }
}