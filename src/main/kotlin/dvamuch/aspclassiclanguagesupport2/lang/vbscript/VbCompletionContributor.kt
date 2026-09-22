package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.application.options.CodeStyle
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import dvamuch.aspclassiclanguagesupport2.lang.AspVbScriptContext
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbClassStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbConstDecl
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclaration
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFileSymbolTable
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFunctionStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbIncludeSymbolResolver
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbParam
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPostfixRefExpr
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPostfixSuffix
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPropertyStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbQualifiedIdentifier
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbResolveUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbSubStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbVarDecl
import java.util.Locale

class VbCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(VbScriptLanguage),
            VbCompletionProvider()
        )
    }
}

private class VbCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        if (isInsideCommentOrString(parameters)) return

        directNewClassName(position)?.let { className ->
            VbUserClassResolver.resolveNewClass(className, position)?.let { classStatement ->
                addUserClassMembers(result, classStatement, position)
            }
            return
        }

        val memberPath = memberPath(position)
        if (memberPath != null) {
            val builtIn = memberPath.singleOrNull()?.let { owner ->
                builtInMembers[owner.lowercase(Locale.ROOT)]
            }
            if (builtIn != null) {
                builtIn.forEach { member ->
                    result.addElement(lookup(member, "ASP built-in member"))
                }
                return
            }

            VbObjectTypeResolver.resolve(memberPath, position)?.let { objectType ->
                objectType.members.forEach { member ->
                    result.addElement(
                        objectMemberLookup(member, objectType)
                    )
                }
                return
            }
            VbUserClassResolver.resolve(memberPath, position)?.let { classStatement ->
                addUserClassMembers(result, classStatement, position)
            }
            return
        }

        if (isWithMemberCompletion(position)) {
            val ownerPath = VbUserClassResolver.enclosingWithOwnerPath(position)
            val builtIn = ownerPath?.singleOrNull()?.let { owner ->
                builtInMembers[owner.lowercase(Locale.ROOT)]
            }
            if (builtIn != null) {
                builtIn.forEach { member -> result.addElement(lookup(member, "ASP built-in member")) }
            } else {
                ownerPath?.let { VbObjectTypeResolver.resolve(it, position) }?.let { objectType ->
                    objectType.members.forEach { member ->
                        result.addElement(objectMemberLookup(member, objectType))
                    }
                } ?: VbUserClassResolver.resolveEnclosingWith(position)?.let { classStatement ->
                    addUserClassMembers(result, classStatement, position)
                }
            }
            return
        }

        val keywordCase = position.containingFile?.let(CodeStyle::getSettings)
            ?.let(VbScriptKeywordCaseSupport::mode)
            ?: VbScriptCodeStyleSettings.KEYWORD_CASE_TITLE
        keywords.forEach { keyword ->
            result.addElement(
                lookup(VbScriptKeywordCaseSupport.normalizeCompletion(keyword, keywordCase), "VBScript keyword")
            )
        }
        builtInGlobals.forEach { name -> result.addElement(lookup(name, "VBScript/ASP built-in")) }

        val expectedParameterType = VbCallSignatureSupport.contextAt(position, parameters.offset)
            ?.parameter
            ?.typeText
        visibleDeclarations(position).forEach { declaration ->
            val name = (declaration.id as? VbNamedElement)?.name.orEmpty()
            if (name.isEmpty()) return@forEach
            val lookup = LookupElementBuilder.createWithSmartPointer(name, declaration.id)
                .withCaseSensitivity(false)
                .withTypeText(declarationKind(declaration), true)
            val rankedLookup = if (
                declaration.id.parent is VbConstDecl && VbAdoEnumCatalog.contains(expectedParameterType, name)
            ) {
                PrioritizedLookupElement.withPriority(lookup, ADO_ENUM_COMPLETION_PRIORITY)
            } else {
                lookup
            }
            result.addElement(rankedLookup)
        }
    }

    private fun visibleDeclarations(position: PsiElement): List<VbDeclaration> {
        val currentFile = position.containingFile ?: return emptyList()
        val scopes = VbResolveUtil.visibleScopes(position)
        val usageOffset = position.textOffset
        val result = mutableListOf<VbDeclaration>()
        val seenNames = mutableSetOf<String>()

        val localDeclarations = VbFileSymbolTable.get(currentFile).declarations()
            .filter { declaration ->
                declaration.scope in scopes && (!declaration.implicit || declaration.id.textOffset < usageOffset)
            }
            .sortedWith(
                compareBy<VbDeclaration> { scopes.indexOf(it.scope) }
                    .thenBy { if (it.implicit) 1 else 0 }
                    .thenByDescending { it.id.textOffset }
            )
        addDistinct(localDeclarations, seenNames, result)

        val aspDeclarations = AspVbScriptContext.get(position)
            ?.hostDeclarationIds()
            .orEmpty()
            .mapNotNull { id -> dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclarationUtil.declaration(id) }
            .filter { declaration -> declaration.scope is PsiFile }
            .sortedWith(
                compareBy<VbDeclaration> { if (it.implicit) 1 else 0 }
                    .thenByDescending { it.id.textOffset }
            )
        addDistinct(aspDeclarations, seenNames, result)

        for (includedFile in VbIncludeSymbolResolver.includedVbScriptFiles(position)) {
            val declarations = VbFileSymbolTable.get(includedFile).declarations()
                .filter { it.scope is PsiFile }
                .sortedWith(
                    compareBy<VbDeclaration> { if (it.implicit) 1 else 0 }
                        .thenByDescending { it.id.textOffset }
                )
            addDistinct(declarations, seenNames, result)
        }
        return result
    }

    private fun addDistinct(
        declarations: List<VbDeclaration>,
        seenNames: MutableSet<String>,
        result: MutableList<VbDeclaration>
    ) {
        declarations.forEach { declaration ->
            val name = (declaration.id as? VbNamedElement)?.name ?: return@forEach
            if (seenNames.add(name.lowercase(Locale.ROOT))) result.add(declaration)
        }
    }

    private fun memberPath(position: PsiElement): List<String>? {
        val id = PsiTreeUtil.getParentOfType(position, VbId::class.java, false)
        val path = when (val parent = id?.parent) {
            is VbPostfixSuffix -> {
                val reference = parent.parent as? VbPostfixRefExpr
                val rootName = (reference?.id as? VbNamedElement)?.name
                if (rootName == null) {
                    null
                } else {
                    listOf(rootName) + reference.postfixSuffixList
                        .takeWhile { suffix -> suffix !== parent }
                        .mapNotNull { suffix -> (suffix.id as? VbNamedElement)?.name }
                }
            }

            is VbQualifiedIdentifier -> {
                val currentIndex = parent.idList.indexOfFirst { candidate -> candidate === id }
                if (currentIndex <= 0) {
                    null
                } else {
                    parent.idList.take(currentIndex)
                        .mapNotNull { pathId -> (pathId as? VbNamedElement)?.name }
                }
            }

            else -> null
        } ?: memberPathFromText(position)
        return path?.takeIf { it.isNotEmpty() }
    }

    private fun isWithMemberCompletion(position: PsiElement): Boolean {
        val text = position.containingFile?.text ?: return false
        val offset = position.textOffset.coerceIn(0, text.length)
        val before = text.substring(0, offset).trimEnd()
        return before.endsWith('.') &&
            PsiTreeUtil.getParentOfType(position, dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbWithStmt::class.java, false) != null
    }

    private fun directNewClassName(position: PsiElement): String? {
        val text = position.containingFile?.text ?: return null
        val prefix = text.substring(0, position.textOffset.coerceAtMost(text.length))
        return directNewCompletionRegex.find(prefix)?.groupValues?.get(1)
    }

    private fun addUserClassMembers(
        result: CompletionResultSet,
        classStatement: VbClassStmt,
        position: PsiElement
    ) {
        VbUserClassResolver.members(classStatement, position).forEach { member ->
            val name = (member.id as? VbNamedElement)?.name ?: return@forEach
            result.addElement(
                LookupElementBuilder.createWithSmartPointer(name, member.id)
                    .withCaseSensitivity(false)
                    .withTypeText("user class ${member.kind}", true)
            )
        }
    }

    private fun isInsideCommentOrString(parameters: CompletionParameters): Boolean {
        val file = parameters.originalFile
        val caretOffset = parameters.offset.coerceIn(0, file.textLength)
        val tokenOffset = (caretOffset - 1).coerceAtLeast(0)
        val originalType = file.findElementAt(tokenOffset)?.node?.elementType
            ?: parameters.originalPosition?.node?.elementType
        if (originalType == VbTypes.COMMENT || originalType == VbTypes.STRING) return true

        val text = file.text
        val lineStart = text.lastIndexOf('\n', (caretOffset - 1).coerceAtLeast(0)).let { it + 1 }
        var inString = false
        var index = lineStart
        while (index < caretOffset) {
            when (text[index]) {
                '\'' -> if (!inString) return true
                '"' -> {
                    if (inString && index + 1 < caretOffset && text[index + 1] == '"') {
                        index++
                    } else {
                        inString = !inString
                    }
                }
            }
            index++
        }
        return inString
    }

    private fun memberPathFromText(position: PsiElement): List<String>? {
        val text = position.containingFile?.text ?: return null
        val beforePosition = text.substring(0, position.textOffset.coerceAtMost(text.length))
        val lineStart = beforePosition.lastIndexOf('\n').let { index -> index + 1 }
        val line = beforePosition.substring(lineStart)
        return memberPathStartRegex.findAll(line)
            .mapNotNull { match -> parseMemberPath(line, match.range.first) }
            .maxByOrNull { path -> path.size }
    }

    private fun parseMemberPath(text: String, startOffset: Int): List<String>? {
        val path = mutableListOf<String>()
        var offset = startOffset
        while (offset < text.length) {
            val identifier = memberPathStartRegex.find(text, offset)
                ?.takeIf { match -> match.range.first == offset }
                ?: return null
            path.add(identifier.value)
            offset = identifier.range.last + 1
            offset = skipWhitespace(text, offset)

            while (offset < text.length && text[offset] == '(') {
                offset = skipCallArguments(text, offset) ?: return null
                offset = skipWhitespace(text, offset)
            }

            if (offset >= text.length || text[offset] != '.') return null
            offset = skipWhitespace(text, offset + 1)
            if (offset == text.length) return path
        }
        return null
    }

    private fun skipCallArguments(text: String, openingOffset: Int): Int? {
        var depth = 0
        var inString = false
        var offset = openingOffset
        while (offset < text.length) {
            when (text[offset]) {
                '"' -> {
                    if (inString && offset + 1 < text.length && text[offset + 1] == '"') {
                        offset++
                    } else {
                        inString = !inString
                    }
                }

                '(' -> if (!inString) depth++
                ')' -> if (!inString) {
                    depth--
                    if (depth == 0) return offset + 1
                }
            }
            offset++
        }
        return null
    }

    private fun skipWhitespace(text: String, startOffset: Int): Int {
        var offset = startOffset
        while (offset < text.length && text[offset].isWhitespace()) offset++
        return offset
    }

    private fun declarationKind(declaration: VbDeclaration): String = when (declaration.id.parent) {
        is VbParam -> "parameter"
        is VbVarDecl -> "variable"
        is VbConstDecl -> "constant"
        is VbFunctionStmt -> "function"
        is VbSubStmt -> "procedure"
        is VbPropertyStmt -> "property"
        is VbClassStmt -> "class"
        else -> if (declaration.implicit) "implicit variable" else "symbol"
    }

    private fun lookup(name: String, typeText: String) = LookupElementBuilder.create(name)
        .withCaseSensitivity(false)
        .withTypeText(typeText, true)

    private fun objectMemberLookup(member: VbObjectMember, objectType: VbObjectType): LookupElementBuilder {
        var lookup = lookup(member.name, "${objectType.displayName} ${member.kind.label}")
        member.signatureText()?.let { signature ->
            lookup = lookup.withTailText(" $signature", true)
        }
        return lookup
    }
}

private val keywords = listOf(
    "Option Explicit", "Dim", "Const", "Public", "Private", "Default", "Class",
    "Function", "Sub", "Property", "Get", "Let", "Set", "If", "Then", "ElseIf",
    "Else", "End If", "Select Case", "Case", "Case Else", "End Select", "For",
    "Each", "In", "To", "Step", "Next", "Do", "Loop", "While", "Until", "Wend",
    "With", "End With", "Exit", "On Error Resume Next", "On Error GoTo 0", "ReDim",
    "Preserve", "Erase", "Execute", "ExecuteGlobal", "Call", "New", "ByVal", "ByRef",
    "Optional", "True", "False", "Null", "Empty", "Nothing", "And", "Or", "Not",
    "Xor", "Eqv", "Imp", "Is", "Mod"
)

private val builtInGlobals = VbBuiltInSymbols.globals
private val builtInMembers = VbBuiltInSymbols.members

private val memberPathStartRegex = Regex("[A-Za-z_][A-Za-z0-9_]*")
private val directNewCompletionRegex = Regex(
    """\(\s*New\s+([A-Za-z_][A-Za-z0-9_]*)\s*\)\s*\.\s*$""",
    RegexOption.IGNORE_CASE
)
private const val ADO_ENUM_COMPLETION_PRIORITY = 100.0
