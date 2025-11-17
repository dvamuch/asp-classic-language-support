package com.dmitry.aspclassic.psi

import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost

/**
 * Text escaper for ASP content - handles positions between original and injected text
 */
class AspContentTextEscaper(host: PsiLanguageInjectionHost) : LiteralTextEscaper<PsiLanguageInjectionHost>(host) {

    override fun decode(rangeInsideHost: com.intellij.openapi.util.TextRange, outChars: StringBuilder): Boolean {
        val text = myHost.text
        if (rangeInsideHost.endOffset > text.length) {
            return false
        }

        outChars.append(text, rangeInsideHost.startOffset, rangeInsideHost.endOffset)
        return true
    }

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: com.intellij.openapi.util.TextRange): Int {
        val offset = rangeInsideHost.startOffset + offsetInDecoded
        if (offset < rangeInsideHost.startOffset || offset > rangeInsideHost.endOffset) {
            return -1
        }
        return offset
    }

    override fun isOneLine(): Boolean {
        return false // HTML can be multi-line
    }
}