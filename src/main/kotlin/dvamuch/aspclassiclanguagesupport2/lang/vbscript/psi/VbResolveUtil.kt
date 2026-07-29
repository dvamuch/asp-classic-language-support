package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

object VbResolveUtil {
    fun resolve(id: VbId): PsiElement? {
        val name = (id as? VbNamedElement)?.name ?: return null
        val file = id.containingFile ?: return null
        val visibleScopes = visibleScopes(id)

        val declarations = PsiTreeUtil.collectElementsOfType(file, VbId::class.java)
            .asSequence()
            .filter { it !== id }
            .filter { (it as? VbNamedElement)?.name.equals(name, ignoreCase = true) }
            .mapNotNull { declaration(it) }
            .filter { it.scope in visibleScopes }
            .toList()

        val nearestScope = declarations.minOfOrNull { visibleScopes.indexOf(it.scope) } ?: return null
        val scopedDeclarations = declarations.filter { visibleScopes.indexOf(it.scope) == nearestScope }
        val explicitDeclarations = scopedDeclarations.filter { !it.implicit }
        if (explicitDeclarations.isNotEmpty()) {
            return chooseExplicitDeclaration(explicitDeclarations, id.textOffset)?.id
        }

        return scopedDeclarations
            .filter { it.implicit && it.id.textOffset < id.textOffset }
            .maxByOrNull { it.id.textOffset }
            ?.id
    }

    fun isReferenceCandidate(id: VbId): Boolean {
        val declaration = declaration(id)
        if (declaration != null && !declaration.implicit) return false

        val parent = id.parent
        if (parent is VbPostfixSuffix) return false
        if (parent is VbQualifiedIdentifier && parent.idList.firstOrNull() != id) return false
        return true
    }

    private data class Declaration(
        val id: VbId,
        val scope: PsiElement,
        val implicit: Boolean
    )

    private fun declaration(id: VbId): Declaration? {
        val implicit = when (val parent = id.parent) {
            is VbVarDecl,
            is VbConstDecl,
            is VbParam,
            is VbFunctionStmt,
            is VbSubStmt,
            is VbPropertyStmt,
            is VbClassStmt -> false

            is VbForStmt,
            is VbForeachStmt -> true

            else -> if (isImplicitAssignmentDeclaration(id)) true else return null
        }

        return Declaration(id, declarationScope(id), implicit)
    }

    private fun declarationScope(id: VbId): PsiElement {
        val parent = id.parent
        val scopeSearchStart = when (parent) {
            is VbFunctionStmt,
            is VbSubStmt,
            is VbPropertyStmt,
            is VbClassStmt -> parent.parent

            else -> parent
        }
        return nearestScope(scopeSearchStart) ?: id.containingFile
    }

    private fun visibleScopes(id: VbId): List<PsiElement> {
        val scopes = mutableListOf<PsiElement>()
        var current: PsiElement? = id.parent
        while (current != null) {
            if (isScope(current)) scopes.add(current)
            if (current is PsiFile) break
            current = current.parent
        }
        return scopes
    }

    private fun nearestScope(start: PsiElement?): PsiElement? {
        var current = start
        while (current != null) {
            if (isScope(current)) return current
            current = current.parent
        }
        return null
    }

    private fun isScope(element: PsiElement): Boolean {
        return element is VbFunctionStmt ||
            element is VbSubStmt ||
            element is VbPropertyStmt ||
            element is VbClassStmt ||
            element is PsiFile
    }

    private fun chooseExplicitDeclaration(
        declarations: List<Declaration>,
        usageOffset: Int
    ): Declaration? {
        return declarations
            .filter { it.id.textOffset < usageOffset }
            .maxByOrNull { it.id.textOffset }
            ?: declarations.minByOrNull { it.id.textOffset }
    }

    private fun isImplicitAssignmentDeclaration(id: VbId): Boolean {
        val reference = id.parent as? VbPostfixRefExpr ?: return false
        val lvalue = reference.parent as? VbLvalue ?: return false
        val assignment = lvalue.parent as? VbAssignmentStmt ?: return false

        if (reference.id != id || reference.postfixSuffixList.isNotEmpty()) return false

        // Treat bare assignments (including SET/LET) as implicit declarations.
        return assignment.textOffset <= id.textOffset
    }
}
