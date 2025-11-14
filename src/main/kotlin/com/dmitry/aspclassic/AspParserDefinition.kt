package com.dmitry.aspclassic

import com.dmitry.aspclassic.lexer.AspLexerAdapter
import com.dmitry.aspclassic.parser.AspParser
import com.dmitry.aspclassic.psi.AspFile
import com.dmitry.aspclassic.psi.AspTypes
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
 * Parser definition for ASP Classic
 */
class AspParserDefinition : ParserDefinition {
    companion object {
        val FILE = IFileElementType(AspLanguage.INSTANCE)
        val COMMENTS = TokenSet.create(AspTypes.COMMENT)
        val STRINGS = TokenSet.create(AspTypes.STRING)
    }

    override fun createLexer(project: Project?): Lexer = AspLexerAdapter()

    override fun createParser(project: Project?): PsiParser = AspParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun createElement(node: ASTNode): PsiElement = AspTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = AspFile(viewProvider)
}

