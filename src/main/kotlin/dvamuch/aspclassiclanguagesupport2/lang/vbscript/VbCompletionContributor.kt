package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
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

        val memberOwner = memberOwner(position)
        if (memberOwner != null) {
            builtInMembers[memberOwner.lowercase(Locale.ROOT)].orEmpty().forEach { member ->
                result.addElement(lookup(member, "ASP built-in member"))
            }
            return
        }

        keywords.forEach { keyword -> result.addElement(lookup(keyword, "VBScript keyword")) }
        builtInGlobals.forEach { name -> result.addElement(lookup(name, "VBScript/ASP built-in")) }

        visibleDeclarations(position).forEach { declaration ->
            val name = (declaration.id as? VbNamedElement)?.name.orEmpty()
            if (name.isEmpty()) return@forEach
            result.addElement(
                LookupElementBuilder.createWithSmartPointer(name, declaration.id)
                    .withCaseSensitivity(false)
                    .withTypeText(declarationKind(declaration), true)
            )
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

    private fun memberOwner(position: PsiElement): String? {
        val id = PsiTreeUtil.getParentOfType(position, VbId::class.java, false)
        val owner = when (val parent = id?.parent) {
            is VbPostfixSuffix -> {
                val reference = parent.parent as? VbPostfixRefExpr
                (reference?.id as? VbNamedElement)?.name
            }

            is VbQualifiedIdentifier -> {
                if (parent.idList.firstOrNull() == id) null
                else (parent.idList.firstOrNull() as? VbNamedElement)?.name
            }

            else -> null
        } ?: memberOwnerFromText(position)
        return owner
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

    private fun memberOwnerFromText(position: PsiElement): String? {
        val text = position.containingFile?.text ?: return null
        val beforePosition = text.substring(0, position.textOffset.coerceAtMost(text.length))
        return Regex("([A-Za-z_][A-Za-z0-9_]*)\\.\\s*$")
            .find(beforePosition)
            ?.groupValues
            ?.get(1)
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

private val builtInGlobals = listOf(
    "Request", "Response", "Session", "Application", "Server", "ObjectContext", "Err",
    "Array", "Asc", "CBool", "CByte", "CCur", "CDate", "CDbl", "Chr", "CInt", "CLng",
    "CSng", "CStr", "Date", "DateAdd", "DateDiff", "DatePart", "DateSerial", "DateValue",
    "Day", "Eval", "Filter", "FormatCurrency", "FormatDateTime", "FormatNumber",
    "FormatPercent", "GetLocale", "GetObject", "Hex", "Hour", "InStr", "InStrRev",
    "IsArray", "IsDate", "IsEmpty", "IsNull", "IsNumeric", "IsObject", "Join", "LBound",
    "LCase", "Left", "Len", "Log", "LTrim", "Mid", "Minute", "Month", "MonthName",
    "Now", "Oct", "Replace", "Right", "Rnd", "Round", "RTrim", "Second", "SetLocale",
    "Space", "Split", "Sqr", "StrComp", "String", "StrReverse", "Time", "Timer",
    "TimeSerial", "TimeValue", "Trim", "TypeName", "UBound", "UCase", "VarType",
    "Weekday", "WeekdayName", "Year"
)

private val builtInMembers = mapOf(
    "request" to listOf(
        "BinaryRead", "ClientCertificate", "Cookies", "Form", "QueryString",
        "ServerVariables", "TotalBytes"
    ),
    "response" to listOf(
        "AddHeader", "AppendToLog", "BinaryWrite", "Buffer", "CacheControl", "Charset",
        "Clear", "ContentType", "Cookies", "End", "Expires", "ExpiresAbsolute", "Flush",
        "IsClientConnected", "PICS", "Redirect", "Status", "Write"
    ),
    "server" to listOf(
        "CreateObject", "Execute", "GetLastError", "HTMLEncode", "MapPath", "ScriptTimeout",
        "Transfer", "URLEncode"
    ),
    "session" to listOf(
        "Abandon", "CodePage", "Contents", "LCID", "SessionID", "StaticObjects", "Timeout"
    ),
    "application" to listOf("Contents", "Lock", "StaticObjects", "Unlock"),
    "objectcontext" to listOf("SetAbort", "SetComplete"),
    "err" to listOf("Clear", "Description", "HelpContext", "HelpFile", "Number", "Raise", "Source")
)
