package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.injected.editor.DocumentWindow
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFoldingBuilder
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage

class AspFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor> {
        val manager = InjectedLanguageManager.getInstance(root.project)
        val injectedFiles = injectedVbScriptFiles(root, manager)
        if (injectedFiles.isEmpty()) return emptyArray()

        val psiDocumentManager = PsiDocumentManager.getInstance(root.project)
        val descriptors = mutableListOf<FoldingDescriptor>()
        for (injectedFile in injectedFiles) {
            val injectedDocument = psiDocumentManager.getDocument(injectedFile) as? DocumentWindow ?: continue
            val injectedDescriptors = vbFoldingBuilder.buildFoldRegions(injectedFile, injectedDocument, quick)
            for (descriptor in injectedDescriptors) {
                val injectedRange = descriptor.range
                val editableRanges = manager.intersectWithAllEditableFragments(injectedFile, injectedRange)
                if (editableRanges.isEmpty()) continue

                val hostRange = injectedDocument.injectedToHost(injectedRange)
                if (hostRange.length <= 0 || !root.textRange.contains(hostRange)) continue
                descriptors.add(FoldingDescriptor(root.node, hostRange))
            }
        }
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = " ... "

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private fun injectedVbScriptFiles(
        root: PsiElement,
        manager: InjectedLanguageManager
    ): Set<PsiFile> {
        val result = linkedSetOf<PsiFile>()
        val firstScriptlet = PsiTreeUtil.collectElementsOfType(root, AspOuterPsiElement::class.java)
            .firstOrNull { host -> aspScriptletInfo(host) != null }
            ?: return result
        manager.enumerate(firstScriptlet) { injectedFile, _ ->
            if (injectedFile.language == VbScriptLanguage) result.add(injectedFile)
        }
        return result
    }

    private companion object {
        val vbFoldingBuilder = VbScriptFoldingBuilder()
    }
}
