package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbAssignmentStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbConstDecl
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclarationUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFileSymbolTable
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbResolveUtil
import java.util.Locale

internal object VbObjectTypeResolver {
    fun resolve(memberPath: List<String>, place: PsiElement): VbObjectType? {
        val rootName = memberPath.firstOrNull() ?: return null
        val file = place.containingFile ?: return null
        val scopes = VbResolveUtil.visibleScopes(place)
        val index = VbAssignmentIndex.get(file)
        val resolver = Resolver(file, scopes, index)
        var objectType = resolver.resolveVariable(rootName, place.textOffset, mutableSetOf()) ?: return null
        for (memberName in memberPath.drop(1)) {
            val returnType = objectType.member(memberName)?.returnType ?: return null
            objectType = VbComTypeCatalog.byId(returnType) ?: return null
        }
        return objectType
    }

    private class Resolver(
        private val file: PsiFile,
        private val visibleScopes: List<PsiElement>,
        private val index: VbAssignmentIndex
    ) {
        fun resolveVariable(
            variableName: String,
            usageOffset: Int,
            visiting: MutableSet<String>
        ): VbObjectType? {
            val normalizedName = normalize(variableName)
            val visitKey = "$normalizedName@$usageOffset"
            if (!visiting.add(visitKey)) return null
            try {
                val scope = declarationScope(normalizedName, usageOffset) ?: return null
                val fact = index.latest(normalizedName, scope, usageOffset) ?: return null
                return resolveExpression(fact.expressionText, fact.offset, visiting)
            } finally {
                visiting.remove(visitKey)
            }
        }

        private fun resolveExpression(
            expressionText: String,
            expressionOffset: Int,
            visiting: MutableSet<String>
        ): VbObjectType? {
            val text = expressionText.trim()

            createObjectRegex.matchEntire(text)?.let { match ->
                val argument = match.groupValues[1].trim()
                val progId = stringLiteralValue(argument)
                    ?: identifierRegex.matchEntire(argument)?.value?.let { identifier ->
                        resolveStringValue(identifier, expressionOffset)
                    }
                return progId?.let(VbComTypeCatalog::byProgId)
            }

            newExpressionRegex.matchEntire(text)?.let { match ->
                return VbComTypeCatalog.byNewExpression(match.groupValues[1])
            }

            memberExpressionRegex.matchEntire(text)?.let { match ->
                val ownerName = match.groupValues[1]
                val memberName = match.groupValues[2]
                val ownerType = resolveVariable(ownerName, expressionOffset, visiting) ?: return null
                val returnType = ownerType.member(memberName)?.returnType ?: return null
                return VbComTypeCatalog.byId(returnType)
            }

            identifierRegex.matchEntire(text)?.let { match ->
                return resolveVariable(match.value, expressionOffset, visiting)
            }

            return null
        }

        private fun resolveStringValue(variableName: String, usageOffset: Int): String? {
            val normalizedName = normalize(variableName)
            val scope = declarationScope(normalizedName, usageOffset) ?: return null
            val fact = index.latest(normalizedName, scope, usageOffset) ?: return null
            return stringLiteralValue(fact.expressionText.trim())
        }

        private fun declarationScope(normalizedName: String, usageOffset: Int): PsiElement? {
            return VbFileSymbolTable.get(file).declarations(normalizedName)
                .asSequence()
                .filter { declaration -> declaration.scope in visibleScopes }
                .filter { declaration -> !declaration.implicit || declaration.id.textOffset < usageOffset }
                .minByOrNull { declaration -> visibleScopes.indexOf(declaration.scope) }
                ?.scope
        }
    }
}

private data class VbAssignmentFact(
    val name: String,
    val scope: PsiElement,
    val offset: Int,
    val expressionText: String
)

private class VbAssignmentIndex private constructor(
    private val factsByName: Map<String, List<VbAssignmentFact>>
) {
    fun latest(name: String, scope: PsiElement, beforeOffset: Int): VbAssignmentFact? {
        return factsByName[name].orEmpty()
            .asSequence()
            .filter { fact -> fact.scope == scope && fact.offset < beforeOffset }
            .maxByOrNull { fact -> fact.offset }
    }

    companion object {
        fun get(file: PsiFile): VbAssignmentIndex {
            return CachedValuesManager.getCachedValue(file) {
                CachedValueProvider.Result.create(build(file), file)
            }
        }

        private fun build(file: PsiFile): VbAssignmentIndex {
            val assignments = PsiTreeUtil.collectElementsOfType(file, VbAssignmentStmt::class.java)
                .mapNotNull(::assignmentFact)
            val constants = PsiTreeUtil.collectElementsOfType(file, VbConstDecl::class.java)
                .mapNotNull(::constantFact)
            return VbAssignmentIndex((assignments + constants).groupBy { it.name })
        }

        private fun assignmentFact(assignment: VbAssignmentStmt): VbAssignmentFact? {
            val reference = assignment.lvalue.postfixRefExpr ?: return null
            if (reference.postfixSuffixList.isNotEmpty()) return null
            val name = (reference.id as? VbNamedElement)?.name ?: return null
            val scope = nearestScope(assignment) ?: assignment.containingFile
            return VbAssignmentFact(normalize(name), scope, assignment.textOffset, assignment.expr.text)
        }

        private fun constantFact(constant: VbConstDecl): VbAssignmentFact? {
            val name = (constant.id as? VbNamedElement)?.name ?: return null
            val scope = nearestScope(constant) ?: constant.containingFile
            return VbAssignmentFact(normalize(name), scope, constant.textOffset, constant.expr.text)
        }

        private fun nearestScope(element: PsiElement): PsiElement? {
            var current: PsiElement? = element.parent
            while (current != null) {
                if (VbDeclarationUtil.isScope(current)) return current
                current = current.parent
            }
            return null
        }
    }
}

private val createObjectRegex = Regex(
    pattern = """^(?:(?:Server)\s*\.\s*)?CreateObject\s*\(\s*(.+?)\s*\)\s*$""",
    option = RegexOption.IGNORE_CASE
)
private val newExpressionRegex = Regex(
    pattern = """^New\s+([A-Za-z_][A-Za-z0-9_.]*)\s*$""",
    option = RegexOption.IGNORE_CASE
)
private val memberExpressionRegex = Regex(
    pattern = """^([A-Za-z_][A-Za-z0-9_]*)\s*\.\s*([A-Za-z_][A-Za-z0-9_]*)(?:\s*\(.*\))?\s*$""",
    option = RegexOption.IGNORE_CASE
)
private val identifierRegex = Regex("""[A-Za-z_][A-Za-z0-9_]*""")
private val stringLiteralRegex = Regex("""^"((?:""|[^"])*)"$""")

private fun stringLiteralValue(text: String): String? {
    return stringLiteralRegex.matchEntire(text)?.groupValues?.get(1)?.replace("\"\"", "\"")
}

private fun normalize(value: String): String = value.lowercase(Locale.ROOT)
