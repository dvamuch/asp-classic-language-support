package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.application.options.CodeStyle
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.source.codeStyle.PreFormatProcessor

/** Normalize code widths before the HTML formatter decides where to wrap lines. */
class AspPreFormatProcessor : PreFormatProcessor {
    override fun process(element: ASTNode, range: TextRange): TextRange {
        val file = element.psi.containingFile ?: return range
        if (file.language != AspLanguage || range.startOffset != 0 || range.endOffset < file.textLength) return range
        val oldLength = file.textLength
        AspPostFormatProcessor().prepareCodeSpacing(file, CodeStyle.getSettings(file))
        return TextRange(0, (range.endOffset + file.textLength - oldLength).coerceAtMost(file.textLength))
    }

    override fun changesWhitespacesOnly(): Boolean = true
}
