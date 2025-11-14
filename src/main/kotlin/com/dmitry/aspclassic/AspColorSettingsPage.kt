package com.dmitry.aspclassic

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

/**
 * Color settings page for ASP Classic
 */
class AspColorSettingsPage : ColorSettingsPage {
    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("ASP Tags", AspSyntaxHighlighter.ASP_TAG),
            AttributesDescriptor("Keywords", AspSyntaxHighlighter.KEYWORD),
            AttributesDescriptor("String", AspSyntaxHighlighter.STRING),
            AttributesDescriptor("Number", AspSyntaxHighlighter.NUMBER),
            AttributesDescriptor("Comment", AspSyntaxHighlighter.COMMENT),
            AttributesDescriptor("Operator", AspSyntaxHighlighter.OPERATOR),
            AttributesDescriptor("Identifier", AspSyntaxHighlighter.IDENTIFIER),
            AttributesDescriptor("ASP Object", AspSyntaxHighlighter.ASP_OBJECT),
            AttributesDescriptor("Parentheses", AspSyntaxHighlighter.PARENTHESES),
            AttributesDescriptor("Braces", AspSyntaxHighlighter.BRACES),
            AttributesDescriptor("Bad Character", AspSyntaxHighlighter.BAD_CHARACTER)
        )

        private const val DEMO_TEXT = """
<!DOCTYPE html>
<html>
<head>
    <title>ASP Classic Example</title>
</head>
<body>
    <%
    ' This is a comment
    Dim userName, userAge
    userName = Request.Form("name")
    userAge = CInt(Request.QueryString("age"))
    
    If Not IsEmpty(userName) Then
        Response.Write("Hello, " & userName & "!")
    Else
        Response.Write("Hello, Guest!")
    End If
    
    For i = 1 To 10 Step 2
        Response.Write("Count: " & i & "<br>")
    Next
    
    Function CalculateSum(a, b)
        CalculateSum = a + b
    End Function
    
    Dim result
    result = CalculateSum(5, 3)
    %>
    
    <h1>Welcome to ASP Classic</h1>
    <p>User: <%= userName %></p>
    <p>Age: <%= userAge %></p>
    <p>Sum Result: <%= result %></p>
    
    <%
    ' Working with Session and Application
    Session("lastVisit") = Now()
    Application.Lock()
    Application("visitCount") = Application("visitCount") + 1
    Application.Unlock()
    
    Set conn = Server.CreateObject("ADODB.Connection")
    Server.MapPath("/database.mdb")
    %>
</body>
</html>
"""
    }

    override fun getIcon(): Icon = AspIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = AspSyntaxHighlighter()

    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "ASP Classic"
}

