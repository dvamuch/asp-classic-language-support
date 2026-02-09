package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object VbResolveUtil {
    fun resolve(id: VbId): PsiElement? {
        val name = (id as? VbNamedElement)?.name ?: return null
        val file = id.containingFile ?: return null
        val offset = id.textOffset

        val candidates = PsiTreeUtil.collectElementsOfType(file, VbId::class.java)
            .filter { it.textOffset < offset }
            .filter { (it as? VbNamedElement)?.name.equals(name, ignoreCase = true) }
            .filter { isDeclaration(it) }

        return candidates.maxByOrNull { it.textOffset }
    }

    private fun isDeclaration(id: VbId): Boolean {
        val parent = id.parent
        return parent is VbVarDecl ||
            parent is VbConstDecl ||
            parent is VbParam ||
            parent is VbFunctionStmt ||
            parent is VbSubStmt ||
            parent is VbPropertyStmt ||
            parent is VbClassStmt ||
            isImplicitAssignmentDeclaration(id)
    }

    private fun isImplicitAssignmentDeclaration(id: VbId): Boolean {
        val qualified = id.parent as? VbQualifiedIdentifier ?: return false
        val lvalue = qualified.parent as? VbLvalue ?: return false
        val assignment = lvalue.parent as? VbAssignmentStmt ?: return false

        val ids = qualified.idList
        if (ids.size != 1 || ids[0] != id) return false

        // Treat bare assignments (including SET/LET) as implicit declarations.
        return assignment.textOffset <= id.textOffset
    }
}
