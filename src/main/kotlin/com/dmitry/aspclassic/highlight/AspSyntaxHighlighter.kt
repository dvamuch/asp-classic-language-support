package com.dmitry.aspclassic.highlight

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.dmitry.aspclassic.parser.AspLexer
import com.dmitry.aspclassic.parser.AspTokenType

class AspSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer {
        return AspLexer()
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            AspTokenType.ASP_OPEN_TAG, AspTokenType.ASP_CLOSE_TAG -> ASP_TAG_KEYS
            AspTokenType.ASP_SCRIPT_CONTENT -> ASP_SCRIPT_KEYS
            // HTML_CONTENT не подсвечиваем - оставляем для HTML инжектора
            else -> EMPTY_KEYS
        }
    }

    companion object {
        val ASP_TAG: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "ASP.TAG",
            DefaultLanguageHighlighterColors.BRACES
        )

        val ASP_SCRIPT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "ASP.SCRIPT",
            DefaultLanguageHighlighterColors.KEYWORD
        )

        val ASP_TAG_KEYS = arrayOf(ASP_TAG)
        val ASP_SCRIPT_KEYS = arrayOf(ASP_SCRIPT)
        val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}