package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

object VbResolveUtil {
    fun resolve(id: VbId): PsiElement? {
        val name = (id as? VbNamedElement)?.name ?: return null
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

        return VbIncludeSymbolResolver.resolve(id, name)
    }

    fun isReferenceCandidate(id: VbId): Boolean {
        val declaration = VbDeclarationUtil.declaration(id)
        if (declaration != null && !declaration.implicit) return false

        val parent = id.parent
        if (parent is VbPostfixSuffix) return false
        if (parent is VbWithMemberRefExpr || parent is VbWithQualifiedIdentifier) return false
        if (parent is VbQualifiedIdentifier && parent.idList.firstOrNull() != id) return false
        return true
    }

    private fun visibleScopes(id: VbId): List<PsiElement> {
        val scopes = mutableListOf<PsiElement>()
        var current: PsiElement? = id.parent
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
