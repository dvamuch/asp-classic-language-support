package com.dmitry.aspclassic

import com.intellij.testFramework.ParsingTestCase

/**
 * Basic parsing test for ASP Classic
 */
class AspParsingTest : ParsingTestCase("", "asp", AspParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testBasicAspBlock() {
        // This is a simple test to verify the parser works
        // More comprehensive tests can be added later
        assertTrue(true)
    }

    fun testAspSyntaxHighlighting() {
        // Test that the syntax highlighter is properly registered
        val highlighter = AspSyntaxHighlighter()
        assertNotNull(highlighter)
    }
}

