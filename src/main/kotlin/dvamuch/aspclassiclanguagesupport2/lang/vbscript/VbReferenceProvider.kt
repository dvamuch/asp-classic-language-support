package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbResolveUtil
import com.intellij.psi.util.PsiTreeUtil
import java.util.concurrent.ConcurrentHashMap

class VbReferenceProvider : PsiReferenceProvider() {
    private val logger = Logger.getInstance(VbReferenceProvider::class.java)
    private val loggedElements = ConcurrentHashMap.newKeySet<String>()

    init {
        logger.warn("VBScript ref debug: VbReferenceProvider instantiated")
    }

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val id = if (element is VbId) element
        else PsiTreeUtil.getParentOfType(element, VbId::class.java, false)
            ?: return PsiReference.EMPTY_ARRAY
        logOnce(element, id)
        return arrayOf(VbLeafReference(element, id))
    }

    private class VbLeafReference(
        element: PsiElement,
        private val id: VbId
    ) : com.intellij.psi.PsiReferenceBase<PsiElement>(element, TextRange(0, element.textLength), false) {
        override fun resolve(): PsiElement? = VbResolveUtil.resolve(id)
    }

    private fun logOnce(element: PsiElement, id: VbId) {
        val file = element.containingFile ?: return
        val key = file.virtualFile?.path + ":" + element.textOffset
        if (!loggedElements.add(key)) return
        logger.warn(
            "VBScript ref debug: element=${element.javaClass.simpleName} text='${element.text}' " +
                "offset=${element.textOffset} file=${file.virtualFile?.path} lang=${file.language.id} " +
                "parentId=${id.javaClass.simpleName} parentText='${id.text}'"
        )
    }
}
