package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.Language
import com.intellij.lexer.Lexer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.templateLanguages.TemplateDataModifications
import com.intellij.psi.tree.IElementType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

/**
 * Presents every ASP scriptlet to the template-data parser as one outer range.
 *
 * Keeping the scriptlet whole is important inside quoted HTML attributes. If
 * individual VBScript tokens are inserted into the HTML tree separately, a
 * quote in a VBScript string can split [com.intellij.psi.xml.XmlAttributeValue]
 * and make platform quick-fixes replace only the truncated prefix.
 */
internal class AspTemplateDataElementType(
    debugName: String,
    language: Language,
    private val templateDataElementType: IElementType,
    outerElementType: IElementType
) : TemplateDataElementType(debugName, language, templateDataElementType, outerElementType) {
    override fun collectTemplateModifications(
        sourceCode: CharSequence,
        baseLexer: Lexer
    ): TemplateDataModifications {
        val modifications = TemplateDataModifications()
        var outerStart = -1
        var outerEnd = -1
        var insertionRange = false

        fun flushOuterRange() {
            if (outerStart >= 0) {
                modifications.addOuterRange(TextRange(outerStart, outerEnd), insertionRange)
                outerStart = -1
                outerEnd = -1
                insertionRange = false
            }
        }

        baseLexer.start(sourceCode)
        while (baseLexer.tokenType != null) {
            val tokenType = baseLexer.tokenType
            if (tokenType == templateDataElementType) {
                flushOuterRange()
            } else {
                if (outerStart < 0 || outerEnd != baseLexer.tokenStart) {
                    flushOuterRange()
                    outerStart = baseLexer.tokenStart
                    insertionRange = tokenType == VbTypes.ASP_EXPR_OPEN
                }
                outerEnd = baseLexer.tokenEnd
            }
            baseLexer.advance()
        }
        flushOuterRange()
        return modifications
    }
}
