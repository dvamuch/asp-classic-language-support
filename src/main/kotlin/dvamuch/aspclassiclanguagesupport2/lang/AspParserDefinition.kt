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

class AspParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?) = AspLexer()

    override fun createParser(project: Project?): PsiParser {
        return PsiParser { root, builder ->
            val marker = builder.mark()
            while (!builder.eof()) {
                builder.advanceLexer()
            }
            marker.done(root)
            builder.treeBuilt
        }
    }

    override fun getFileNodeType(): IFileElementType = AspTokenTypes.FILE

    override fun getWhitespaceTokens(): TokenSet = TokenSet.EMPTY

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = AspFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(
        left: ASTNode,
        right: ASTNode
    ): ParserDefinition.SpaceRequirements = ParserDefinition.SpaceRequirements.MAY
}
