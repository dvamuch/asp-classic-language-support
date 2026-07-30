package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbClassStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDoStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbForStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbForeachStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFunctionStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbIfBlockStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPropertyStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbSelectStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbSubStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbWhileStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbWithStmt

class VbScriptFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        PsiTreeUtil.processElements(root) { element ->
            if (isFoldableBlock(element)) {
                bodyRange(element, document)?.let { range ->
                    descriptors.add(FoldingDescriptor(element.node, range))
                }
            }
            true
        }
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = " ... "

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private fun isFoldableBlock(element: PsiElement): Boolean = when (element) {
        is VbClassStmt,
        is VbFunctionStmt,
        is VbSubStmt,
        is VbPropertyStmt,
        is VbIfBlockStmt,
        is VbSelectStmt,
        is VbForStmt,
        is VbForeachStmt,
        is VbDoStmt,
        is VbWhileStmt,
        is VbWithStmt -> true

        else -> false
    }

    private fun bodyRange(element: PsiElement, document: Document): TextRange? {
        val elementRange = element.textRange
        if (elementRange.isEmpty || elementRange.endOffset <= elementRange.startOffset) return null

        val startLine = document.getLineNumber(elementRange.startOffset)
        val endLine = document.getLineNumber((elementRange.endOffset - 1).coerceAtLeast(elementRange.startOffset))
        if (endLine <= startLine) return null

        val foldStart = document.getLineEndOffset(startLine)
        val foldEnd = document.getLineStartOffset(endLine)
        return TextRange(foldStart, foldEnd).takeIf { range -> range.length > 0 }
    }
}
