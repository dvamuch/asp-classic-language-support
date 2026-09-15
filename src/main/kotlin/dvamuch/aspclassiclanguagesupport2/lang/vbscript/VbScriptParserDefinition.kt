package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.parser.VbScriptParser
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

class VbScriptParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?) = VbScriptLexerAdapter()

    override fun createParser(project: Project?): PsiParser = VbScriptParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet = TokenSet.create(com.intellij.psi.TokenType.WHITE_SPACE)

    override fun getCommentTokens(): TokenSet = TokenSet.create(VbTypes.COMMENT)

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(VbTypes.STRING)

    override fun createElement(node: ASTNode): PsiElement = VbTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = VbScriptFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(
        left: ASTNode,
        right: ASTNode
    ): ParserDefinition.SpaceRequirements = ParserDefinition.SpaceRequirements.MAY

    companion object {
        private val FILE = IFileElementType(VbScriptLanguage)
    }
}
