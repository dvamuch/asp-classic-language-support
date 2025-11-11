// AspSyntaxHighlighter.kt
package com.dvamuch.asp

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.intellij.psi.TokenType

class AspSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = AspLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            AspTypes.ASP_TAG_START, AspTypes.ASP_TAG_END ->
                arrayOf(ASP_TAGS)

            AspTypes.RESPONSE, AspTypes.REQUEST, AspTypes.SESSION,
            AspTypes.APPLICATION, AspTypes.SERVER ->
                arrayOf(BUILT_IN_OBJECTS)

            AspTypes.IF, AspTypes.THEN, AspTypes.ELSE, AspTypes.END,
            AspTypes.FOR, AspTypes.NEXT, AspTypes.WHILE, AspTypes.WEND,
            AspTypes.DO, AspTypes.LOOP, AspTypes.DIM, AspTypes.SET,
            AspTypes.FUNCTION, AspTypes.SUB ->
                arrayOf(KEYWORD)

            AspTypes.STRING ->
                arrayOf(STRING)

            AspTypes.NUMBER ->
                arrayOf(NUMBER)

            AspTypes.COMMENT ->
                arrayOf(COMMENT)

            TokenType.BAD_CHARACTER ->
                arrayOf(BAD_CHARACTER)

            else ->
                emptyArray()
        }
    }

    companion object {
        val ASP_TAGS = TextAttributesKey.createTextAttributesKey("ASP.TAGS",
            DefaultLanguageHighlighterColors.KEYWORD)
        val BUILT_IN_OBJECTS = TextAttributesKey.createTextAttributesKey("ASP.BUILT_IN_OBJECTS",
            DefaultLanguageHighlighterColors.INSTANCE_METHOD)
        val KEYWORD = TextAttributesKey.createTextAttributesKey("ASP.KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD)
        val STRING = TextAttributesKey.createTextAttributesKey("ASP.STRING",
            DefaultLanguageHighlighterColors.STRING)
        val NUMBER = TextAttributesKey.createTextAttributesKey("ASP.NUMBER",
            DefaultLanguageHighlighterColors.NUMBER)
        val COMMENT = TextAttributesKey.createTextAttributesKey("ASP.COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey("ASP.BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER)
    }
}