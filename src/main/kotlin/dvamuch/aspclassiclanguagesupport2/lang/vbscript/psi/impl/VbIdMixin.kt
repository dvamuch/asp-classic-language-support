package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPsiFactory
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbReference
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbResolveUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

abstract class VbIdMixin(node: ASTNode) : ASTWrapperPsiElement(node), VbId, VbNamedElement {
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
        return if (VbResolveUtil.isReferenceCandidate(this)) VbReference(this) else null
    }

    override fun getReferences(): Array<PsiReference> {
        val refs = ReferenceProvidersRegistry.getReferencesFromProviders(this)
        if (refs.isNotEmpty()) return refs
        val reference = getReference() ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(reference)
    }
}
