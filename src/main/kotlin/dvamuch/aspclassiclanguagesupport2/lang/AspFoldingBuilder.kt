package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFoldingBuilder

class AspFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor> {
        val aspFile = root.containingFile?.viewProvider?.getPsi(AspLanguage) ?: return emptyArray()
        val context = AspVbScriptContext.getForAspFile(aspFile)
        val analysisFile = context.analysisFile

        val descriptors = mutableListOf<FoldingDescriptor>()
        val analysisDocument = PsiDocumentManager.getInstance(root.project).getDocument(analysisFile)
            ?: EditorFactory.getInstance().createDocument(analysisFile.text)
        val injectedDescriptors = vbFoldingBuilder.buildFoldRegions(analysisFile, analysisDocument, quick)
        for (descriptor in injectedDescriptors) {
            val hostRange = context.analysisSpanToHost(descriptor.range) ?: continue
            if (hostRange.length <= 0 || !root.textRange.contains(hostRange)) continue
            descriptors.add(FoldingDescriptor(root.node, hostRange))
        }
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = " ... "

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private companion object {
        val vbFoldingBuilder = VbScriptFoldingBuilder()
    }
}
