package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
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
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbElementType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

class AspParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?) = AspLexer()

    override fun createParser(project: Project?): PsiParser = VbScriptParser()

    override fun getFileNodeType(): IFileElementType = AspTokenTypes.FILE

    override fun getWhitespaceTokens(): TokenSet = TokenSet.create(com.intellij.psi.TokenType.WHITE_SPACE)

    override fun getCommentTokens(): TokenSet = TokenSet.create(VbTypes.COMMENT)

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(VbTypes.STRING)

    override fun createElement(node: ASTNode): PsiElement {
        return when (node.elementType) {
            AspTokenTypes.OUTER -> AspOuterPsiElement(node.elementType, node.text)
            is VbElementType -> VbTypes.Factory.createElement(node)
            else -> ASTWrapperPsiElement(node)
        }
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = AspFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(
        left: ASTNode,
        right: ASTNode
    ): ParserDefinition.SpaceRequirements = ParserDefinition.SpaceRequirements.MAY
}
