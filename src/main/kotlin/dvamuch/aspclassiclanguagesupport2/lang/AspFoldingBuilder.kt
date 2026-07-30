package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.injected.editor.DocumentWindow
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFoldingBuilder

class AspFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor> {
        val manager = InjectedLanguageManager.getInstance(root.project)
        val injectedFile = AspInjectedVbScript.findFile(root) ?: return emptyArray()

        val psiDocumentManager = PsiDocumentManager.getInstance(root.project)
        val descriptors = mutableListOf<FoldingDescriptor>()
        val injectedDocument = psiDocumentManager.getDocument(injectedFile) as? DocumentWindow ?: return emptyArray()
        val injectedDescriptors = vbFoldingBuilder.buildFoldRegions(injectedFile, injectedDocument, quick)
        for (descriptor in injectedDescriptors) {
            val injectedRange = descriptor.range
            val editableRanges = manager.intersectWithAllEditableFragments(injectedFile, injectedRange)
            if (editableRanges.isEmpty()) continue

            val hostRange = injectedDocument.injectedToHost(injectedRange)
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
