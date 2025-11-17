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

class AspParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = AspLexer()

    override fun createParser(project: Project?): PsiParser = AspParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode): PsiElement {
        println("PARSER_DEF: Creating element for node type: ${node.elementType}")
        return AspPsiElementFactory.createElement(node)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        println("PARSER_DEF: Creating ASP file")
        return AspFile(viewProvider)
    }

    override fun getWhitespaceTokens(): TokenSet = TokenSet.WHITE_SPACE

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements {
        return ParserDefinition.SpaceRequirements.MAY
    }

    companion object {
        val FILE = IFileElementType(AspLanguage.INSTANCE)

        // Define which element types we create
        val ELEMENT_TYPES = TokenSet.create(
            AspTokenType.HTML_CONTENT,
            AspTokenType.ASP_OPEN_TAG,
            AspTokenType.ASP_CLOSE_TAG,
            AspTokenType.ASP_SCRIPT_CONTENT
        )
    }
}