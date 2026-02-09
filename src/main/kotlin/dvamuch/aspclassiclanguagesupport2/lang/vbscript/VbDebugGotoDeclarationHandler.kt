package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId

class VbDebugGotoDeclarationHandler : GotoDeclarationHandler {
    private val logger = Logger.getInstance(VbDebugGotoDeclarationHandler::class.java)

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        val element = sourceElement ?: return null
        if (!element.language.isKindOf(VbScriptLanguage)) return null

        val elementType = element.node?.elementType?.toString() ?: "<no-node>"
        val elementText = shorten(element.text)
        val elementRefs = element.references
        val providerRefs = ReferenceProvidersRegistry.getReferencesFromProviders(element)
        val parentId = PsiTreeUtil.getParentOfType(element, VbId::class.java, false)
        val parentRefs = parentId?.references ?: PsiElement.EMPTY_ARRAY
        val parentProviderRefs = if (parentId != null) {
            ReferenceProvidersRegistry.getReferencesFromProviders(parentId)
        } else {
            PsiElement.EMPTY_ARRAY
        }
        val parentChain = buildParentChain(element)

        logger.warn(
            "VBScript ref debug: goto element=${element.javaClass.simpleName} type=$elementType " +
                "text='$elementText' offset=$offset file=${element.containingFile?.virtualFile?.path} " +
                "refs=${elementRefs.size} providerRefs=${providerRefs.size} " +
                "parentId=${parentId?.javaClass?.simpleName} parentText='${parentId?.text}' " +
                "parentChain=$parentChain " +
                "parentRefs=${parentRefs.size} parentProviderRefs=${parentProviderRefs.size}"
        )

        return null
    }

    override fun getActionText(context: com.intellij.openapi.actionSystem.DataContext): String? = null

    private fun shorten(text: String, max: Int = 80): String {
        if (text.length <= max) return text
        return text.substring(0, max) + "..."
    }

    private fun buildParentChain(element: PsiElement, max: Int = 6): String {
        val chain = ArrayList<String>(max)
        var current: PsiElement? = element
        while (current != null && chain.size < max) {
            val type = current.node?.elementType?.toString() ?: "<no-node>"
            chain.add("${current.javaClass.simpleName}:$type")
            current = current.parent
        }
        return chain.joinToString(" > ")
    }
}
