package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbClassStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDoStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbForStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbForeachStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFunctionStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbIfBlockStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPropertyStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbSelectStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbSubStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbWhileStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbWithStmt

internal object VbKeywordPairMatcher {
    fun match(file: PsiFile, offset: Int): Pair<TextRange, TextRange>? {
        if (file.textLength == 0) return null
        val boundedOffset = offset.coerceIn(0, file.textLength)
        val candidateOffsets = buildList {
            if (boundedOffset < file.textLength) add(boundedOffset)
            if (boundedOffset > 0) add(boundedOffset - 1)
        }

        for (candidateOffset in candidateOffsets) {
            val leaf = file.findElementAt(candidateOffset) ?: continue
            val block = nearestBlock(leaf) ?: continue
            val pair = markerRanges(block) ?: continue
            if (touches(pair.first, candidateOffset) || touches(pair.second, candidateOffset)) return pair
        }
        return null
    }

    private fun markerRanges(block: PsiElement): Pair<TextRange, TextRange>? {
        val spec = markerSpec(block) ?: return null
        val children = directChildren(block.node)
        val opening = children.firstOrNull { child -> child.elementType == spec.opening } ?: return null
        val closingStart = children.lastOrNull { child -> child.elementType == spec.closingStart } ?: return null
        val closingEnd = if (spec.closingEnd == null) {
            closingStart
        } else {
            children.asSequence()
                .dropWhile { child -> child !== closingStart }
                .drop(1)
                .firstOrNull { child -> child.elementType == spec.closingEnd }
                ?: return null
        }
        return Pair.create(
            opening.textRange,
            TextRange(closingStart.startOffset, closingEnd.startOffset + closingEnd.textLength)
        )
    }

    private fun markerSpec(block: PsiElement): MarkerSpec? = when (block) {
        is VbIfBlockStmt -> MarkerSpec(VbTypes.IF, VbTypes.END, VbTypes.IF)
        is VbFunctionStmt -> MarkerSpec(VbTypes.FUNCTION, VbTypes.END, VbTypes.FUNCTION)
        is VbSubStmt -> MarkerSpec(VbTypes.SUB, VbTypes.END, VbTypes.SUB)
        is VbPropertyStmt -> MarkerSpec(VbTypes.PROPERTY, VbTypes.END, VbTypes.PROPERTY)
        is VbClassStmt -> MarkerSpec(VbTypes.CLASS, VbTypes.END, VbTypes.CLASS)
        is VbSelectStmt -> MarkerSpec(VbTypes.SELECT, VbTypes.END, VbTypes.SELECT)
        is VbWithStmt -> MarkerSpec(VbTypes.WITH, VbTypes.END, VbTypes.WITH)
        is VbForStmt, is VbForeachStmt -> MarkerSpec(VbTypes.FOR, VbTypes.NEXT)
        is VbDoStmt -> MarkerSpec(VbTypes.DO, VbTypes.LOOP)
        is VbWhileStmt -> MarkerSpec(VbTypes.WHILE, VbTypes.WEND)
        else -> null
    }

    private fun nearestBlock(element: PsiElement): PsiElement? {
        var current: PsiElement? = element
        while (current != null) {
            if (markerSpec(current) != null) return current
            current = current.parent
        }
        return null
    }

    private fun directChildren(node: ASTNode): List<ASTNode> = buildList {
        var child = node.firstChildNode
        while (child != null) {
            add(child)
            child = child.treeNext
        }
    }

    private fun touches(range: TextRange, offset: Int): Boolean {
        return offset >= range.startOffset && offset <= range.endOffset
    }
}

private data class MarkerSpec(
    val opening: IElementType,
    val closingStart: IElementType,
    val closingEnd: IElementType? = null
)
