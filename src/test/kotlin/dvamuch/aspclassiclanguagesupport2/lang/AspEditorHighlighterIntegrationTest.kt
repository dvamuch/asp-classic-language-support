package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AspEditorHighlighterIntegrationTest : BasePlatformTestCase() {
    fun testHtmlLayerAndAspDelimitersUseIndependentHighlighting() {
        val file = myFixture.configureByText(
            AspFileType,
            "<div class=\"sample\">Text</div><% Dim value %>"
        )
        val highlighter = myFixture.editor.highlighter

        val htmlToken = highlighter.createIterator(file.text.indexOf("div"))
        val attributeToken = highlighter.createIterator(file.text.indexOf("class"))
        val aspToken = highlighter.createIterator(file.text.indexOf("<%"))
        val aspHtmlAttributes = htmlToken.textAttributes.clone()
        val aspAttributeAttributes = attributeToken.textAttributes.clone()

        println("ASP editor tokens: html=${htmlToken.tokenType}, attribute=${attributeToken.tokenType}, asp=${aspToken.tokenType}")
        assertFalse("HTML name must be tokenized by the HTML layer", htmlToken.tokenType.toString().contains("ASP_TEMPLATE_DATA"))
        assertFalse("HTML attribute must be tokenized by the HTML layer", attributeToken.tokenType.toString().contains("ASP_TEMPLATE_DATA"))
        assertEquals("VBScriptToken.ASP_OPEN", aspToken.tokenType.toString())
        assertEquals("PHP_TAG", aspToken.textAttributesKeyNames().single())

        val htmlFile = myFixture.configureByText(HtmlFileType.INSTANCE, "<div class=\"sample\">Text</div>")
        val nativeHtmlHighlighter = myFixture.editor.highlighter
        assertEquals(
            "HTML tag colors in ASP must match a regular HTML file",
            nativeHtmlHighlighter.createIterator(htmlFile.text.indexOf("div")).textAttributes,
            aspHtmlAttributes
        )
        assertEquals(
            "HTML attribute colors in ASP must match a regular HTML file",
            nativeHtmlHighlighter.createIterator(htmlFile.text.indexOf("class")).textAttributes,
            aspAttributeAttributes
        )
    }

    private fun com.intellij.openapi.editor.highlighter.HighlighterIterator.textAttributesKeyNames(): List<String> {
        val scheme = myFixture.editor.colorsScheme
        val attributes = textAttributes
        return listOfNotNull(
            "PHP_TAG".takeIf { scheme.getAttributes(com.intellij.openapi.editor.colors.TextAttributesKey.find(it)) == attributes }
        )
    }
}
