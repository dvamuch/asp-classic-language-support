package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPsiFactory
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbReference
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

abstract class VbIdMixin(node: ASTNode) : ASTWrapperPsiElement(node), VbId, VbNamedElement {
    private val logger = Logger.getInstance(VbIdMixin::class.java)

    override fun getNameIdentifier(): PsiElement? = node.findChildByType(VbTypes.IDENTIFIER)?.psi

    override fun getName(): String? = text

    override fun setName(name: String): PsiElement {
        val newId = VbPsiFactory(project).createId(name)
        val newNode = newId.node.findChildByType(VbTypes.IDENTIFIER) ?: newId.node
        val oldNode = node.findChildByType(VbTypes.IDENTIFIER) ?: node
        node.replaceChild(oldNode, newNode)
        return this
    }

    override fun getReference(): PsiReference? {
        logger.warn("VBScript ref debug: getReference id='${text}' file=${containingFile?.virtualFile?.path}")
        return VbReference(this)
    }

    override fun getReferences(): Array<PsiReference> {
        logger.warn(
            "VBScript ref debug: getReferences element=${javaClass.simpleName} text='${text}' " +
                "offset=${textOffset} file=${containingFile?.virtualFile?.path}"
        )
        val refs = ReferenceProvidersRegistry.getReferencesFromProviders(this)
        logger.warn("VBScript ref debug: providerRefs count=${refs.size} id='${text}'")
        if (refs.isNotEmpty()) return refs
        val single = getReference() ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(single)
    }
}
