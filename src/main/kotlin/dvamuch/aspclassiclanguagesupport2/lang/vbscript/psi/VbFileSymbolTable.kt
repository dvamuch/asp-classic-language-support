package dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import java.util.Locale

internal data class VbDeclaration(
    val id: VbId,
    val scope: PsiElement,
    val implicit: Boolean
)

internal class VbFileSymbolTable private constructor(
    private val declarationsByName: Map<String, List<VbDeclaration>>
) {
    fun declarations(name: String): List<VbDeclaration> {
        return declarationsByName[normalizeName(name)].orEmpty()
    }

    companion object {
        fun get(file: PsiFile): VbFileSymbolTable {
            return CachedValuesManager.getCachedValue(file) {
                CachedValueProvider.Result.create(build(file), file)
            }
        }

        private fun build(file: PsiFile): VbFileSymbolTable {
            val declarations = PsiTreeUtil.collectElementsOfType(file, VbId::class.java)
                .mapNotNull(VbDeclarationUtil::declaration)
                .groupBy { declaration ->
                    normalizeName((declaration.id as? VbNamedElement)?.name.orEmpty())
                }
            return VbFileSymbolTable(declarations)
        }
    }
}

internal object VbDeclarationUtil {
    fun declaration(id: VbId): VbDeclaration? {
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

        return VbDeclaration(id, declarationScope(id), implicit)
    }

    fun isScope(element: PsiElement): Boolean {
        return element is VbFunctionStmt ||
            element is VbSubStmt ||
            element is VbPropertyStmt ||
            element is VbClassStmt ||
            element is PsiFile
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

    private fun nearestScope(start: PsiElement?): PsiElement? {
        var current = start
        while (current != null) {
            if (isScope(current)) return current
            current = current.parent
        }
        return null
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

private fun normalizeName(name: String): String = name.lowercase(Locale.ROOT)
