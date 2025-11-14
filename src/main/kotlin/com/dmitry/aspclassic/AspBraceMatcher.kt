package com.dmitry.aspclassic

import com.dmitry.aspclassic.psi.AspTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

/**
 * Brace matcher for ASP Classic
 */
class AspBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(AspTypes.LPAREN, AspTypes.RPAREN, false),
        BracePair(AspTypes.LBRACE, AspTypes.RBRACE, false),
        BracePair(AspTypes.ASP_START, AspTypes.ASP_END, true),
        BracePair(AspTypes.ASP_OUTPUT_START, AspTypes.ASP_END, true)
    )

    override fun getPairs(): Array<BracePair> = pairs

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}

