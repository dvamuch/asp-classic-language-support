package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclarationUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbOptionStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPostfixSuffix
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbQualifiedIdentifier
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbResolveUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbWithMemberRefExpr
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbWithQualifiedIdentifier

/**
 * Reports undeclared root identifiers only when the file explicitly opts in to
 * VBScript's strict declaration rules. Member names are deliberately left to
 * object-aware resolution so that COM and other dynamic APIs do not produce a
 * stream of false positives.
 */
class VbUnresolvedIdentifierInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.file
        if (PsiTreeUtil.findChildOfType(file, VbOptionStmt::class.java) == null) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val id = element as? VbId ?: return
                if (!isRootReference(id) || isInsideMalformedPsi(id)) return
                if (VbDeclarationUtil.declaration(id, implicitDeclarationsAllowed = false) != null) return

                val name = (id as? VbNamedElement)?.name?.takeIf { it.isNotBlank() } ?: return
                if (VbBuiltInSymbols.isKnownGlobal(name) || VbResolveUtil.resolve(id) != null) return

                holder.registerProblem(
                    id,
                    "Undeclared identifier: $name",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                )
            }
        }
    }

    private fun isRootReference(id: VbId): Boolean {
        return when (val parent = id.parent) {
            is VbPostfixSuffix,
            is VbWithMemberRefExpr,
            is VbWithQualifiedIdentifier -> false

            is VbQualifiedIdentifier -> parent.idList.firstOrNull() === id
            else -> true
        }
    }

    private fun isInsideMalformedPsi(id: VbId): Boolean {
        var current: PsiElement? = id.parent
        while (current != null && current !is com.intellij.psi.PsiFile) {
            if (current is PsiErrorElement) return true
            current = current.parent
        }
        return false
    }
}
