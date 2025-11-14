package com.dmitry.aspclassic

import com.dmitry.aspclassic.lexer.AspLexerAdapter
import com.dmitry.aspclassic.psi.AspTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font

/**
 * Syntax highlighter for ASP Classic
 */
class AspSyntaxHighlighter : SyntaxHighlighterBase() {
    companion object {
        val ASP_TAG = TextAttributesKey.createTextAttributesKey(
            "ASP.TAG",
            TextAttributes(
                JBColor(Color(255, 140, 0), Color(255, 165, 0)), // Orange color (darker in light theme, lighter in dark theme)
                null,
                null,
                null,
                Font.BOLD
            )
        )
        val KEYWORD = TextAttributesKey.createTextAttributesKey(
            "ASP.KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD
        )
        val STRING = TextAttributesKey.createTextAttributesKey(
            "ASP.STRING",
            DefaultLanguageHighlighterColors.STRING
        )
        val NUMBER = TextAttributesKey.createTextAttributesKey(
            "ASP.NUMBER",
            DefaultLanguageHighlighterColors.NUMBER
        )
        val COMMENT = TextAttributesKey.createTextAttributesKey(
            "ASP.COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )
        val OPERATOR = TextAttributesKey.createTextAttributesKey(
            "ASP.OPERATOR",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        val IDENTIFIER = TextAttributesKey.createTextAttributesKey(
            "ASP.IDENTIFIER",
            DefaultLanguageHighlighterColors.IDENTIFIER
        )
        val ASP_OBJECT = TextAttributesKey.createTextAttributesKey(
            "ASP.ASP_OBJECT",
            DefaultLanguageHighlighterColors.INSTANCE_FIELD
        )
        val PARENTHESES = TextAttributesKey.createTextAttributesKey(
            "ASP.PARENTHESES",
            DefaultLanguageHighlighterColors.PARENTHESES
        )
        val BRACES = TextAttributesKey.createTextAttributesKey(
            "ASP.BRACES",
            DefaultLanguageHighlighterColors.BRACES
        )
        val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey(
            "ASP.BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER
        )

        private val ASP_TAG_KEYS = arrayOf(ASP_TAG)
        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val STRING_KEYS = arrayOf(STRING)
        private val NUMBER_KEYS = arrayOf(NUMBER)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val OPERATOR_KEYS = arrayOf(OPERATOR)
        private val IDENTIFIER_KEYS = arrayOf(IDENTIFIER)
        private val ASP_OBJECT_KEYS = arrayOf(ASP_OBJECT)
        private val PARENTHESES_KEYS = arrayOf(PARENTHESES)
        private val BRACES_KEYS = arrayOf(BRACES)
        private val BAD_CHARACTER_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }

    override fun getHighlightingLexer(): Lexer = AspLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            AspTypes.ASP_START, AspTypes.ASP_END, AspTypes.ASP_OUTPUT_START, AspTypes.ASP_DIRECTIVE_START -> ASP_TAG_KEYS
            AspTypes.KEYWORD -> KEYWORD_KEYS
            AspTypes.STRING -> STRING_KEYS
            AspTypes.NUMBER -> NUMBER_KEYS
            AspTypes.COMMENT -> COMMENT_KEYS
            AspTypes.OPERATOR -> OPERATOR_KEYS
            AspTypes.ASP_OBJECT -> ASP_OBJECT_KEYS
            AspTypes.IDENTIFIER -> IDENTIFIER_KEYS
            AspTypes.LPAREN, AspTypes.RPAREN -> PARENTHESES_KEYS
            AspTypes.LBRACE, AspTypes.RBRACE -> BRACES_KEYS
            TokenType.BAD_CHARACTER -> BAD_CHARACTER_KEYS
            else -> EMPTY_KEYS
        }
    }
}

