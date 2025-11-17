package com.dmitry.aspclassic.psi

import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost

/**
 * Content outside ASP blocks (HTML/text) that can host injected languages
 */
interface AspContent : PsiLanguageInjectionHost {
    override fun isValidHost(): Boolean = true
    override fun updateText(text: String): PsiLanguageInjectionHost

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost>
}