package com.dmitry.aspclassic.parser

import com.dmitry.aspclassic.AspLanguage
import com.dmitry.aspclassic.psi.AspFile
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Parser definition for ASP language
 */
class AspParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer {
        return AspLexer()
    }

    override fun createParser(project: Project?): PsiParser {
        return AspParser()
    }

    override fun getFileNodeType(): IFileElementType {
        return FILE
    }

    override fun getCommentTokens(): TokenSet {
        return TokenSet.EMPTY
    }

    override fun getStringLiteralElements(): TokenSet {
        return TokenSet.EMPTY
    }

    override fun createElement(node: ASTNode): PsiElement {
        return AspPsiElementFactory.createElement(node)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return AspFile(viewProvider)
    }

    override fun getWhitespaceTokens(): TokenSet {
        return TokenSet.WHITE_SPACE
    }

    companion object {
        val FILE = IFileElementType(AspLanguage.INSTANCE)

        // HTML_CONTENT должен быть здесь, чтобы парсер создавал для него элементы
        val HTML_ELEMENTS = TokenSet.create(AspTokenType.HTML_CONTENT)

        // Все токены, которые должны создавать PSI элементы
        val ELEMENT_TYPES = TokenSet.create(
            AspTokenType.ASP_OPEN_TAG,
            AspTokenType.ASP_CLOSE_TAG,
            AspTokenType.ASP_SCRIPT_CONTENT,
            AspTokenType.HTML_CONTENT
        )
    }
}