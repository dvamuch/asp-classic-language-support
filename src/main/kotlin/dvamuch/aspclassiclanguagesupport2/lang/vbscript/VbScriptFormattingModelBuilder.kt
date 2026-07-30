package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

class VbScriptFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val spacingBuilder = createSpacingBuilder(settings)
        val indentSize = settings.getCommonSettings(VbScriptLanguage).indentOptions?.INDENT_SIZE ?: 4
        val rootBlock = VbScriptFormattingBlock(
            node = formattingContext.node,
            indent = null,
            spacingBuilder = spacingBuilder,
            indentSize = indentSize
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            rootBlock,
            settings
        )
    }

    private fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
        return SpacingBuilder(settings, VbScriptLanguage)
            .around(VbTypes.EQ).spaces(1)
            .around(COMPARISON_OPERATORS).spaces(1)
            .aroundInside(ADDITIVE_OPERATORS, VbTypes.ADD_EXPR).spaces(1)
            .aroundInside(MULTIPLICATIVE_OPERATORS, VbTypes.MULT_EXPR).spaces(1)
            .aroundInside(VbTypes.POW, VbTypes.POW_EXPR).spaces(1)
            .aroundInside(VbTypes.AMP, VbTypes.CONCAT_EXPR).spaces(1)
            .aroundInside(LOGICAL_OPERATORS, VbTypes.OR_EXPR).spaces(1)
            .aroundInside(VbTypes.AND, VbTypes.AND_EXPR).spaces(1)
            .afterInside(VbTypes.NOT, VbTypes.NOT_EXPR).spaces(1)
            .before(VbTypes.COMMA).none()
            .after(VbTypes.COMMA).spaces(1)
            .after(VbTypes.LPAREN).none()
            .before(VbTypes.RPAREN).none()
            .around(VbTypes.DOT).none()
    }

    companion object {
        private val COMPARISON_OPERATORS = TokenSet.create(
            VbTypes.NEQ,
            VbTypes.LT,
            VbTypes.GT,
            VbTypes.LE,
            VbTypes.GE,
            VbTypes.IS
        )
        private val ADDITIVE_OPERATORS = TokenSet.create(VbTypes.PLUS, VbTypes.MINUS)
        private val MULTIPLICATIVE_OPERATORS = TokenSet.create(
            VbTypes.STAR,
            VbTypes.SLASH,
            VbTypes.IDIV,
            VbTypes.MOD
        )
        private val LOGICAL_OPERATORS = TokenSet.create(
            VbTypes.OR,
            VbTypes.XOR,
            VbTypes.EQV,
            VbTypes.IMP
        )
    }
}
