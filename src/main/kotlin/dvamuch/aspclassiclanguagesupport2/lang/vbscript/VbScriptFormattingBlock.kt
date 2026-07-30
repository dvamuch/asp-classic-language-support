package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.formatting.Alignment
import com.intellij.formatting.ASTBlock
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

internal class VbScriptFormattingBlock(
    node: ASTNode,
    wrap: Wrap? = null,
    alignment: Alignment? = null,
    private val indent: Indent?,
    private val spacingBuilder: SpacingBuilder,
    private val indentSize: Int
) : AbstractBlock(node, wrap, alignment) {
    override fun buildChildren(): List<Block> = buildList {
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.textLength > 0 && !isFormattingWhitespace(child)) {
                add(
                    VbScriptFormattingBlock(
                        node = child,
                        indent = Indent.getNoneIndent(),
                        spacingBuilder = spacingBuilder,
                        indentSize = indentSize
                    )
                )
            }
            child = child.treeNext
        }
    }

    override fun getIndent(): Indent? = indent

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        if (isLineContinuationBlock(child1) || isLineContinuationBlock(child2)) {
            return Spacing.getReadOnlySpacing()
        }
        if (child1 != null && endsWithLineBreak(child1) && !startsWithLineBreak(child2)) {
            val node = (child2 as? ASTBlock)?.node
            val spaces = if (node == null) 0 else indentationLevel(node) * indentSize
            return Spacing.createSpacing(spaces, spaces, 0, true, 0)
        }
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val childIndent = when (myNode.elementType) {
            VbTypes.CLASS_STMT,
            VbTypes.FUNCTION_STMT,
            VbTypes.SUB_STMT,
            VbTypes.PROPERTY_STMT,
            VbTypes.IF_BLOCK_STMT,
            VbTypes.ELSEIF_BLOCK,
            VbTypes.ELSE_BLOCK,
            VbTypes.SELECT_STMT,
            VbTypes.CASE_BLOCK,
            VbTypes.CASE_ELSE_BLOCK,
            VbTypes.FOR_STMT,
            VbTypes.FOREACH_STMT,
            VbTypes.DO_STMT,
            VbTypes.WHILE_STMT,
            VbTypes.WITH_STMT -> Indent.getNormalIndent()

            else -> Indent.getNoneIndent()
        }
        return ChildAttributes(childIndent, null)
    }

    override fun isLeaf(): Boolean = myNode.firstChildNode == null

    private fun isFormattingWhitespace(node: ASTNode): Boolean {
        return node.elementType == TokenType.WHITE_SPACE && !isLineContinuation(node.text)
    }

    private fun isLineContinuationBlock(block: Block?): Boolean {
        val text = (block as? ASTBlock)?.node?.text ?: return false
        return isLineContinuation(text)
    }

    private fun isLineContinuation(text: CharSequence): Boolean {
        return text.indexOf('_') >= 0 && (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0)
    }

    private fun endsWithLineBreak(block: Block): Boolean {
        var node = (block as? ASTBlock)?.node ?: return false
        while (true) node = node.lastChildNode ?: break
        val text = node.text
        return text.endsWith('\n') || text.endsWith('\r')
    }

    private fun startsWithLineBreak(block: Block): Boolean {
        var node = (block as? ASTBlock)?.node ?: return false
        while (true) node = node.firstChildNode ?: break
        val text = node.text
        return text.startsWith('\n') || text.startsWith('\r')
    }

    private fun indentationLevel(node: ASTNode): Int {
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

    companion object {
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
}
