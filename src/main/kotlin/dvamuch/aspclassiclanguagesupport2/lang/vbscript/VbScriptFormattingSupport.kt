package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.formatting.SpacingBuilder
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

internal object VbScriptFormattingSupport {
    fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
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

    fun indentationLevel(node: ASTNode): Int {
        var level = 0
        var child = node
        var parent = child.treeParent
        while (parent != null) {
            level += indentationContribution(parent.elementType, child.elementType)
            child = parent
            parent = parent.treeParent
        }
        return level
    }

    fun isLineContinuation(text: CharSequence): Boolean {
        return text.indexOf('_') >= 0 && (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0)
    }

    private fun indentationContribution(parent: IElementType, child: IElementType): Int {
        if (child == VbTypes.STATEMENT_LIST && parent in STATEMENT_BODY_PARENTS) return 1
        if (parent == VbTypes.CLASS_STMT && child == VbTypes.CLASS_BODY) return 1
        if (parent == VbTypes.SELECT_STMT &&
            (child == VbTypes.CASE_BLOCK || child == VbTypes.CASE_ELSE_BLOCK)
        ) return 1
        if ((parent == VbTypes.ELSEIF_BLOCK || parent == VbTypes.ELSE_BLOCK) &&
            child == VbTypes.IF_BLOCK_BRANCH
        ) return 1
        return 0
    }

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
    private val STATEMENT_BODY_PARENTS = setOf(
        VbTypes.FUNCTION_STMT,
        VbTypes.SUB_STMT,
        VbTypes.PROPERTY_STMT,
        VbTypes.IF_BLOCK_STMT,
        VbTypes.FOR_STMT,
        VbTypes.FOREACH_STMT,
        VbTypes.DO_STMT,
        VbTypes.WHILE_STMT,
        VbTypes.WITH_STMT,
        VbTypes.CASE_BLOCK,
        VbTypes.CASE_ELSE_BLOCK
    )
}
