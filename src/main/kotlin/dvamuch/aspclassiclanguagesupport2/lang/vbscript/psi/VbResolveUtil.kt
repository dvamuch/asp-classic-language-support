package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dvamuch.aspclassiclanguagesupport2.lang.AspVbScriptContext
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbUserClassResolver

object VbResolveUtil {
    fun resolve(id: VbId): PsiElement? {
        if (VbUserClassResolver.isMemberReference(id)) {
            return VbUserClassResolver.resolveMember(id)
        }
        val name = (id as? VbNamedElement)?.name ?: return null
        resolveInFile(id, name)?.let { return it }

        val hostLocation = VbAspPsiUtil.hostLocation(id)
        val aspContext = AspVbScriptContext.get(id)
        if (hostLocation != null && aspContext != null) {
            val analysisId = aspContext.analysisIdAtHostOffset(hostLocation.offset)
            if (analysisId != null) {
                val analysisTarget = resolveInFile(analysisId, name)
                if (analysisTarget is VbId) {
                    aspContext.hostIdForAnalysis(analysisTarget)?.let { return it }
                }
            }
        }

        return VbIncludeSymbolResolver.resolve(id, name)
    }

    private fun resolveInFile(id: VbId, name: String): PsiElement? {
        val file = id.containingFile ?: return null
        val visibleScopes = visibleScopes(id)

        val declarations = VbFileSymbolTable.get(file).declarations(name)
            .asSequence()
            .filter { it.id !== id }
            .filter { it.scope in visibleScopes }
            .toList()

        val nearestScope = declarations.minOfOrNull { visibleScopes.indexOf(it.scope) }
        if (nearestScope != null) {
            val scopedDeclarations = declarations.filter { visibleScopes.indexOf(it.scope) == nearestScope }
            val explicitDeclarations = scopedDeclarations.filter { !it.implicit }
            if (explicitDeclarations.isNotEmpty()) {
                return chooseExplicitDeclaration(explicitDeclarations, id.textOffset)?.id
            }

            val implicitDeclaration = scopedDeclarations
                .filter { it.implicit && it.id.textOffset < id.textOffset }
                .maxByOrNull { it.id.textOffset }
            if (implicitDeclaration != null) return implicitDeclaration.id
        }

        return null
    }

    fun isReferenceCandidate(id: VbId): Boolean {
        val declaration = VbDeclarationUtil.declaration(id)
        if (declaration != null && !declaration.implicit) return false

        val parent = id.parent
        if (parent is VbPostfixSuffix) return parent.id === id
        if (parent is VbWithMemberRefExpr) return true
        if (parent is VbWithQualifiedIdentifier) return true
        if (parent is VbQualifiedIdentifier && parent.idList.firstOrNull() != id) return true
        return true
    }

    internal fun visibleScopes(place: PsiElement): List<PsiElement> {
        val scopes = mutableListOf<PsiElement>()
        var current: PsiElement? = place.parent
        while (current != null) {
            if (VbDeclarationUtil.isScope(current)) scopes.add(current)
            if (current is PsiFile) break
            current = current.parent
        }
        return scopes
    }

    private fun chooseExplicitDeclaration(
        declarations: List<VbDeclaration>,
        usageOffset: Int
    ): VbDeclaration? {
        return declarations
            .filter { it.id.textOffset < usageOffset }
            .maxByOrNull { it.id.textOffset }
            ?: declarations.minByOrNull { it.id.textOffset }
    }
}
