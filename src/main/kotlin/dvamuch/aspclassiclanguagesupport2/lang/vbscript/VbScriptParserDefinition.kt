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
import com.intellij.openapi.diagnostic.Logger
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.parser.VbScriptParser
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

class VbScriptParserDefinition : ParserDefinition {
    private val logger = Logger.getInstance(VbScriptParserDefinition::class.java)

    override fun createLexer(project: Project?) = VbScriptLexerAdapter().also {
        logger.warn("VBScript parser debug: createLexer")
    }

    override fun createParser(project: Project?): PsiParser {
        logger.warn("VBScript parser debug: createParser")
        return VbScriptParser()
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet = TokenSet.create(
        com.intellij.psi.TokenType.WHITE_SPACE,
        VbTypes.COMMENT
    )

    override fun getCommentTokens(): TokenSet = TokenSet.create(VbTypes.COMMENT)

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(VbTypes.STRING)

    override fun createElement(node: ASTNode): PsiElement = VbTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        logOnce(viewProvider)
        return VbScriptFile(viewProvider)
    }

    override fun spaceExistenceTypeBetweenTokens(
        left: ASTNode,
        right: ASTNode
    ): ParserDefinition.SpaceRequirements = ParserDefinition.SpaceRequirements.MAY

    companion object {
        private val FILE = IFileElementType(VbScriptLanguage)
        private val loggedFiles = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        private fun logOnce(viewProvider: FileViewProvider) {
            val file = viewProvider.virtualFile ?: return
            val key = file.path + ":" + viewProvider.baseLanguage.id + ":" + viewProvider.languages.joinToString { it.id }
            if (!loggedFiles.add(key)) return
            Logger.getInstance(VbScriptParserDefinition::class.java).warn(
                "VBScript parser debug: file=${file.path} viewProvider=${viewProvider.javaClass.simpleName} " +
                    "baseLang=${viewProvider.baseLanguage.id} langs=${viewProvider.languages.joinToString { it.id }}"
            )
        }
    }
}
