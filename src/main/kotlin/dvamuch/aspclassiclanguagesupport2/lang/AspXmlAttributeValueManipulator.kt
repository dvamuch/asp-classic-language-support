package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.source.resolve.reference.impl.manipulators.XmlAttributeValueManipulator
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.IncorrectOperationException

/**
 * Prevents generic HTML quick-fixes from rebuilding a compound ASP attribute
 * as plain HTML and interpreting quotes inside the scriptlet as delimiters.
 */
class AspXmlAttributeValueManipulator : XmlAttributeValueManipulator() {
    override fun handleContentChange(
        element: XmlAttributeValue,
        range: TextRange,
        newContent: String
    ): XmlAttributeValue {
        if (!isCompoundAspValue(element)) {
            return super.handleContentChange(element, range, newContent) ?: element
        }
        if (!element.isWritable) {
            throw IncorrectOperationException("ASP attribute value is not writable")
        }
        if (range.startOffset < 0 || range.endOffset > element.textLength) {
            throw IncorrectOperationException("Invalid ASP attribute range: $range")
        }

        val elementStart = element.textRange.startOffset
        val aspRanges = element.children
            .asSequence()
            .filter { it.node.elementType == AspTokenTypes.OUTER }
            .map { it.textRange.shiftLeft(elementStart) }
            .toList()
        if (aspRanges.any { intersects(range, it) }) {
            throw IncorrectOperationException("HTML quick-fix cannot replace embedded ASP code")
        }

        val documentManager = PsiDocumentManager.getInstance(element.project)
        val document = documentManager.getDocument(element.containingFile)
            ?: throw IncorrectOperationException("No document for ${element.containingFile.name}")
        val absoluteRange = range.shiftRight(elementStart)
        document.replaceString(
            absoluteRange.startOffset,
            absoluteRange.endOffset,
            escapeForAttributeDelimiter(element.text, newContent)
        )
        return element
    }

    private fun isCompoundAspValue(element: XmlAttributeValue): Boolean {
        return element.containingFile.viewProvider is AspFileViewProvider &&
            element.children.any { it.node.elementType == AspTokenTypes.OUTER }
    }

    private fun intersects(change: TextRange, asp: TextRange): Boolean {
        return if (change.isEmpty) {
            change.startOffset > asp.startOffset && change.startOffset < asp.endOffset
        } else {
            change.startOffset < asp.endOffset && asp.startOffset < change.endOffset
        }
    }

    private fun escapeForAttributeDelimiter(elementText: String, content: String): String {
        return when (elementText.firstOrNull()) {
            '\'' -> content.replace("'", if (elementText.contains("&#39;")) "&#39;" else "&apos;")
            '\"' -> content.replace("\"", if (elementText.contains("&#34;")) "&#34;" else "&quot;")
            else -> content
        }
    }
}
