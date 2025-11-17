package com.dmitry.aspclassic.lang.parser

import com.dmitry.aspclassic.parser.AspFlexLexerAdapter
import org.junit.Test

class AspLexerTest {
    @Test
    fun testJFlexLexer() {
        val testCode = """
            <%@LANGUAGE="VBSCRIPT" CODEPAGE="65001"%>
            <html>
            <head><title>test</title>
            <%
            sub test()
            end sub
            %>
            </head>
            </html>
        """.trimIndent()

        println("=== TESTING JFlex ASP LEXER ===")

        val lexer = AspFlexLexerAdapter()
        lexer.start(testCode, 0, testCode.length, 0)

        var tokenCount = 0
        var tokenType = lexer.tokenType
        while (tokenType != null) {
            val tokenText = testCode.substring(lexer.tokenStart, lexer.tokenEnd)
            println("TOKEN #${++tokenCount}: ${tokenType} [${lexer.tokenStart}-${lexer.tokenEnd}] '${tokenText.replace("\n", "\\n")}'")
            lexer.advance()
            tokenType = lexer.tokenType
        }
    }
}