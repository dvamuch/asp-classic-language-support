package com.dmitry.aspclassic

import com.dmitry.aspclassic.psi.AspTypes
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/**
 * Code completion contributor for ASP Classic
 */
class AspCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(AspTypes.IDENTIFIER),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    // VBScript keywords
                    val keywords = listOf(
                        "Dim", "Set", "If", "Then", "Else", "ElseIf", "End", "For", "To", "Step", "Next",
                        "While", "Wend", "Do", "Loop", "Until", "Function", "Sub", "Exit", "Call",
                        "Select", "Case", "With", "New", "Class", "Public", "Private", "Property",
                        "Get", "Let", "Default", "On", "Error", "Resume", "GoTo", "ReDim", "Preserve",
                        "And", "Or", "Not", "Xor", "Eqv", "Imp", "Mod", "Is", "Nothing", "Empty", "Null",
                        "True", "False", "Option", "Explicit", "ByVal", "ByRef", "Const"
                    )
                    
                    keywords.forEach { keyword ->
                        result.addElement(
                            LookupElementBuilder.create(keyword)
                                .bold()
                                .withTypeText("keyword")
                        )
                    }

                    // ASP Objects
                    val aspObjects = mapOf(
                        "Request" to "Retrieves information from the user",
                        "Response" to "Sends output to the client",
                        "Session" to "Stores user session information",
                        "Application" to "Stores application-level information",
                        "Server" to "Provides server-side functionality",
                        "ObjectContext" to "Used for transaction processing"
                    )
                    
                    aspObjects.forEach { (name, description) ->
                        result.addElement(
                            LookupElementBuilder.create(name)
                                .withTypeText("ASP Object")
                                .withTailText(" - $description", true)
                        )
                    }

                    // Request object members
                    val requestMembers = listOf(
                        "Form", "QueryString", "Cookies", "ServerVariables", "ClientCertificate",
                        "TotalBytes", "BinaryRead"
                    )
                    
                    requestMembers.forEach { member ->
                        result.addElement(
                            LookupElementBuilder.create("Request.$member")
                                .withPresentableText(member)
                                .withTypeText("Request")
                        )
                    }

                    // Response object members
                    val responseMembers = listOf(
                        "Write", "Redirect", "End", "Clear", "Flush", "Cookies",
                        "Buffer", "ContentType", "Expires", "ExpiresAbsolute", "Status",
                        "AddHeader", "AppendToLog", "BinaryWrite", "CacheControl", "Charset"
                    )
                    
                    responseMembers.forEach { member ->
                        result.addElement(
                            LookupElementBuilder.create("Response.$member")
                                .withPresentableText(member)
                                .withTypeText("Response")
                        )
                    }

                    // Session object members
                    val sessionMembers = listOf(
                        "Abandon", "Contents", "SessionID", "Timeout", "CodePage", "LCID"
                    )
                    
                    sessionMembers.forEach { member ->
                        result.addElement(
                            LookupElementBuilder.create("Session.$member")
                                .withPresentableText(member)
                                .withTypeText("Session")
                        )
                    }

                    // Server object members
                    val serverMembers = listOf(
                        "CreateObject", "MapPath", "URLEncode", "HTMLEncode", "Execute",
                        "Transfer", "GetLastError", "ScriptTimeout"
                    )
                    
                    serverMembers.forEach { member ->
                        result.addElement(
                            LookupElementBuilder.create("Server.$member")
                                .withPresentableText(member)
                                .withTypeText("Server")
                        )
                    }

                    // Common VBScript functions
                    val vbscriptFunctions = listOf(
                        "CInt", "CLng", "CDbl", "CStr", "CBool", "CDate",
                        "IsEmpty", "IsNull", "IsNumeric", "IsArray", "IsObject",
                        "Len", "Left", "Right", "Mid", "InStr", "Replace", "Trim", "LTrim", "RTrim",
                        "UCase", "LCase", "Split", "Join",
                        "Now", "Date", "Time", "Year", "Month", "Day", "Hour", "Minute", "Second",
                        "DateAdd", "DateDiff", "FormatDateTime",
                        "Round", "Int", "Fix", "Abs", "Sgn", "Sqr", "Rnd",
                        "Array", "UBound", "LBound",
                        "CreateObject", "GetObject"
                    )
                    
                    vbscriptFunctions.forEach { func ->
                        result.addElement(
                            LookupElementBuilder.create(func)
                                .withTypeText("function")
                                .withInsertHandler(ParenthesesInsertHandler)
                        )
                    }
                }
            }
        )
    }

    companion object {
        private val ParenthesesInsertHandler = InsertHandler<LookupElement> { context, _ ->
            val editor = context.editor
            val document = editor.document
            val offset = editor.caretModel.offset
            document.insertString(offset, "()")
            editor.caretModel.moveToOffset(offset + 1)
        }
    }
}

