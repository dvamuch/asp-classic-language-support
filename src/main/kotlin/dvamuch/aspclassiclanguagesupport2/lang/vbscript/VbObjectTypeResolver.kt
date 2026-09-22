package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbAssignmentStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbClassStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbConstDecl
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclarationUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFileSymbolTable
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFunctionStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbIncludeSymbolResolver
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPostfixRefExpr
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPostfixSuffix
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPropertyStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbQualifiedIdentifier
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbResolveUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbSubStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbVarDecl
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbVisibilityStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbWithMemberRefExpr
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbWithQualifiedIdentifier
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbWithStmt
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

internal data class VbUserClassMember(
    val id: VbId,
    val kind: String
)

/** Resolves the useful statically inferable slice of VBScript's dynamic user-class model. */
internal object VbUserClassResolver {
    fun isMemberReference(id: VbId): Boolean = when (val parent = id.parent) {
        is VbPostfixSuffix -> parent.id === id
        is VbQualifiedIdentifier -> parent.idList.firstOrNull() !== id
        is VbWithMemberRefExpr -> true
        is VbWithQualifiedIdentifier -> true
        else -> false
    }

    fun resolve(memberPath: List<String>, place: PsiElement): VbClassStmt? {
        if (memberPath.isEmpty()) return null
        val file = place.containingFile ?: return null
        val scopes = VbResolveUtil.visibleScopes(place)
        return Resolver(file, scopes, VbAssignmentIndex.get(file), place)
            .resolvePath(memberPath, place.textOffset, mutableSetOf())
    }

    fun resolveNewClass(className: String, place: PsiElement): VbClassStmt? {
        return findClass(className, place)
    }

    fun members(classStatement: VbClassStmt, place: PsiElement): List<VbUserClassMember> {
        return VbFileSymbolTable.get(classStatement.containingFile).declarations()
            .asSequence()
            .filter { declaration -> declaration.scope === classStatement }
            .filter { declaration -> isMemberVisible(declaration.id, classStatement, place) }
            .mapNotNull { declaration ->
                val kind = when (declaration.id.parent) {
                    is VbFunctionStmt -> "function"
                    is VbSubStmt -> "procedure"
                    is VbPropertyStmt -> "property"
                    is VbVarDecl -> "field"
                    else -> null
                }
                kind?.let { VbUserClassMember(declaration.id, it) }
            }
            .distinctBy { member -> (member.id as? VbNamedElement)?.name?.lowercase(Locale.ROOT) }
            .toList()
    }

    fun resolveMember(id: VbId): PsiElement? {
        when (val parent = id.parent) {
            is VbPostfixSuffix -> {
                when (val reference = parent.parent) {
                    is VbPostfixRefExpr -> {
                        val suffixIndex = reference.postfixSuffixList.indexOfFirst { it === parent }
                        if (suffixIndex < 0) return null
                        val rootName = (reference.id as? VbNamedElement)?.name ?: return null
                        val ownerPath = buildList<String> {
                            add(rootName)
                            reference.postfixSuffixList.take(suffixIndex)
                                .mapNotNullTo(this) { suffix -> (suffix.id as? VbNamedElement)?.name }
                        }
                        return resolvePathMember(ownerPath, id)
                    }

                    is VbWithMemberRefExpr -> return resolveWithSuffixMember(reference, parent, id)
                    else -> {
                        val classStatement = directNewClassBefore(id) ?: return null
                        return member(classStatement, (id as? VbNamedElement)?.name, id)?.id
                    }
                }
            }

            is VbQualifiedIdentifier -> {
                val index = parent.idList.indexOfFirst { it === id }
                if (index <= 0) return null
                val ownerPath = parent.idList.take(index).mapNotNull { (it as? VbNamedElement)?.name }
                return resolvePathMember(ownerPath, id)
            }

            is VbWithMemberRefExpr,
            is VbWithQualifiedIdentifier -> return resolveWithMember(id)

            else -> return null
        }
    }

    fun resolveEnclosingWith(place: PsiElement): VbClassStmt? {
        val statement = PsiTreeUtil.getParentOfType(place, VbWithStmt::class.java, false) ?: return null
        return resolveWithStatement(statement, place)
    }

    fun enclosingWithOwnerPath(place: PsiElement): List<String>? {
        val withStatement = PsiTreeUtil.getParentOfType(place, VbWithStmt::class.java, false) ?: return null
        return parseMemberPath(withStatement.expr.text.trim())
    }

    private fun resolveWithMember(id: VbId): PsiElement? {
        val parent = id.parent
        val path = when (parent) {
            is VbWithMemberRefExpr -> listOfNotNull((parent.id as? VbNamedElement)?.name)
            is VbWithQualifiedIdentifier -> {
                val index = parent.idList.indexOfFirst { it === id }
                if (index < 0) return null
                parent.idList.take(index + 1).mapNotNull { (it as? VbNamedElement)?.name }
            }
            else -> return null
        }
        val baseClass = resolveEnclosingWith(id)
            ?: ownerPathBefore(id)?.let { ownerPath -> resolve(ownerPath, id) }
            ?: directNewClassBefore(id)
            ?: return null
        return resolveMemberFromClass(baseClass, path, id)
    }

    private fun resolveWithSuffixMember(
        reference: VbWithMemberRefExpr,
        suffix: VbPostfixSuffix,
        id: VbId
    ): PsiElement? {
        val suffixIndex = reference.postfixSuffixList.indexOfFirst { it === suffix }
        if (suffixIndex < 0) return null
        val path = buildList {
            add((reference.id as? VbNamedElement)?.name ?: return null)
            reference.postfixSuffixList.take(suffixIndex + 1)
                .mapNotNullTo(this) { item -> (item.id as? VbNamedElement)?.name }
        }
        return resolveMemberFromClass(resolveEnclosingWith(id) ?: return null, path, id)
    }

    private fun resolvePathMember(ownerPath: List<String>, id: VbId): PsiElement? {
        val classStatement = resolve(ownerPath, id) ?: return null
        return member(classStatement, (id as? VbNamedElement)?.name, id)?.id
    }

    private fun resolveMemberFromClass(
        baseClass: VbClassStmt,
        path: List<String>,
        place: PsiElement
    ): PsiElement? {
        if (path.isEmpty()) return null
        var owner = baseClass
        path.dropLast(1).forEach { memberName ->
            owner = inferMemberReturnClass(owner, memberName, place, mutableSetOf()) ?: return null
        }
        return member(owner, path.last(), place)?.id
    }

    private fun resolveWithStatement(statement: VbWithStmt, place: PsiElement): VbClassStmt? {
        val expression = statement.expr.text.trim()
        if (expression.startsWith('.')) {
            val outer = PsiTreeUtil.getParentOfType(statement.parent, VbWithStmt::class.java, false) ?: return null
            val outerClass = resolveWithStatement(outer, place) ?: return null
            val relativePath = parseMemberPath(expression.removePrefix(".")) ?: return null
            var current = outerClass
            relativePath.forEach { memberName ->
                current = inferMemberReturnClass(current, memberName, place, mutableSetOf()) ?: return null
            }
            return current
        }
        return resolve(parseMemberPath(expression) ?: return null, place)
    }

    private fun directNewClassBefore(id: VbId): VbClassStmt? {
        val text = id.containingFile?.text ?: return null
        val prefix = text.substring(0, id.textOffset.coerceAtMost(text.length))
        val className = directNewMemberRegex.find(prefix)?.groupValues?.get(1) ?: return null
        return findClass(className, id)
    }

    private fun ownerPathBefore(id: VbId): List<String>? {
        val text = id.containingFile?.text ?: return null
        val offset = id.textOffset.coerceAtMost(text.length)
        val lineStart = text.lastIndexOf('\n', (offset - 1).coerceAtLeast(0)) + 1
        val prefix = text.substring(lineStart, offset)
        val ownerText = ownerPathBeforeMemberRegex.find(prefix)?.groupValues?.get(1) ?: return null
        return parseMemberPath(ownerText)
    }

    private fun member(classStatement: VbClassStmt, name: String?, place: PsiElement): VbUserClassMember? {
        if (name == null) return null
        return members(classStatement, place)
            .firstOrNull { candidate ->
                (candidate.id as? VbNamedElement)?.name.equals(name, ignoreCase = true)
            }
    }

    private fun inferMemberReturnClass(
        classStatement: VbClassStmt,
        memberName: String,
        place: PsiElement,
        visiting: MutableSet<String>
    ): VbClassStmt? {
        val key = "${classStatement.textOffset}:${memberName.lowercase(Locale.ROOT)}"
        if (!visiting.add(key)) return null
        try {
            val targets = VbFileSymbolTable.get(classStatement.containingFile).declarations(memberName)
                .asSequence()
                .filter { declaration -> declaration.scope === classStatement }
                .filter { declaration -> isMemberVisible(declaration.id, classStatement, place) }
                .map { declaration -> declaration.id.parent }
                .filter { target -> target is VbFunctionStmt || target is VbPropertyStmt }
            for (target in targets) {
                val assignments = PsiTreeUtil.collectElementsOfType(target, VbAssignmentStmt::class.java)
                    .asSequence()
                    .filter { assignment ->
                        val reference = assignment.lvalue.postfixRefExpr
                        reference?.postfixSuffixList?.isEmpty() == true &&
                            (reference.id as? VbNamedElement)?.name.equals(memberName, ignoreCase = true)
                    }
                    .sortedByDescending(PsiElement::getTextOffset)
                for (assignment in assignments) {
                    val className = newExpressionRegex.matchEntire(assignment.expr.text.trim())
                        ?.groupValues?.get(1)
                        ?.takeIf { '.' !in it }
                        ?: continue
                    findClass(className, place)?.let { return it }
                }
            }
            return null
        } finally {
            visiting.remove(key)
        }
    }

    private fun findClass(name: String, place: PsiElement): VbClassStmt? {
        val candidates = buildList {
            place.containingFile?.let(::add)
            addAll(VbIncludeSymbolResolver.includedVbScriptFiles(place))
        }
        return candidates.asSequence()
            .flatMap { file -> VbFileSymbolTable.get(file).declarations(name).asSequence() }
            .mapNotNull { declaration -> declaration.id.parent as? VbClassStmt }
            .firstOrNull()
    }

    private fun isPrivateMember(id: VbId): Boolean {
        val visibility = PsiTreeUtil.getParentOfType(id, VbVisibilityStmt::class.java, false)
        if (visibility != null) return visibility.text.trimStart().startsWith("Private", ignoreCase = true)
        // Class-level Dim declarations are private in VBScript.
        return id.parent is VbVarDecl
    }

    private fun isMemberVisible(id: VbId, classStatement: VbClassStmt, place: PsiElement): Boolean {
        val insideSameClass = PsiTreeUtil.getParentOfType(place, VbClassStmt::class.java, false) === classStatement
        return insideSameClass || !isPrivateMember(id)
    }

    private class Resolver(
        private val file: PsiFile,
        private val visibleScopes: List<PsiElement>,
        private val index: VbAssignmentIndex,
        private val place: PsiElement
    ) {
        fun resolvePath(
            memberPath: List<String>,
            usageOffset: Int,
            visiting: MutableSet<String>
        ): VbClassStmt? {
            var classStatement = resolveRoot(memberPath.first(), usageOffset, visiting) ?: return null
            memberPath.drop(1).forEach { memberName ->
                classStatement = inferMemberReturnClass(classStatement, memberName, place, visiting) ?: return null
            }
            return classStatement
        }

        private fun resolveRoot(
            variableName: String,
            usageOffset: Int,
            visiting: MutableSet<String>
        ): VbClassStmt? {
            if (variableName.equals("Me", ignoreCase = true)) {
                return PsiTreeUtil.getParentOfType(place, VbClassStmt::class.java, false)
            }
            val normalizedName = normalize(variableName)
            val visitKey = "$normalizedName@$usageOffset"
            if (!visiting.add(visitKey)) return null
            try {
                val declaration = VbFileSymbolTable.get(file).declarations(normalizedName)
                    .asSequence()
                    .filter { it.scope in visibleScopes }
                    .filter { !it.implicit || it.id.textOffset < usageOffset }
                    .minByOrNull { visibleScopes.indexOf(it.scope) }
                    ?: return null
                val assignment = index.latest(normalizedName, declaration.scope, usageOffset) ?: return null
                return resolveExpression(assignment.expressionText, assignment.offset, visiting)
            } finally {
                visiting.remove(visitKey)
            }
        }

        private fun resolveExpression(
            expressionText: String,
            expressionOffset: Int,
            visiting: MutableSet<String>
        ): VbClassStmt? {
            val text = expressionText.trim()
            newExpressionRegex.matchEntire(text)?.let { match ->
                return findClass(match.groupValues[1], place)
            }
            memberExpressionRegex.matchEntire(text)?.let { match ->
                val owner = resolveRoot(match.groupValues[1], expressionOffset, visiting) ?: return null
                return inferMemberReturnClass(owner, match.groupValues[2], place, visiting)
            }
            identifierRegex.matchEntire(text)?.let { match ->
                return resolveRoot(match.value, expressionOffset, visiting)
            }
            return null
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
private val qualifiedMemberPathRegex = Regex(
    """[A-Za-z_][A-Za-z0-9_]*(?:\s*\(\s*\))?(?:\s*\.\s*[A-Za-z_][A-Za-z0-9_]*(?:\s*\(\s*\))?)*"""
)
private val directNewMemberRegex = Regex(
    """\(\s*New\s+([A-Za-z_][A-Za-z0-9_]*)\s*\)\s*\.\s*$""",
    RegexOption.IGNORE_CASE
)
private val ownerPathBeforeMemberRegex = Regex(
    """([A-Za-z_][A-Za-z0-9_]*(?:\s*\(\s*\))?(?:\s*\.\s*[A-Za-z_][A-Za-z0-9_]*(?:\s*\(\s*\))?)*)\s*\.\s*$"""
)
private val stringLiteralRegex = Regex("""^"((?:""|[^"])*)"$""")

private fun parseMemberPath(text: String): List<String>? {
    if (!qualifiedMemberPathRegex.matches(text)) return null
    return identifierRegex.findAll(text).map(MatchResult::value).toList()
}

private fun stringLiteralValue(text: String): String? {
    return stringLiteralRegex.matchEntire(text)?.groupValues?.get(1)?.replace("\"\"", "\"")
}

private fun normalize(value: String): String = value.lowercase(Locale.ROOT)
