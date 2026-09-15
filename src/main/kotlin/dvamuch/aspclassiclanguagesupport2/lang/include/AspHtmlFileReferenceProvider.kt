package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ProcessingContext
import dvamuch.aspclassiclanguagesupport2.lang.AspFileViewProvider

/**
 * Keeps platform file references away from the broken XmlAttributeValue
 * manipulator when an ASP scriptlet contributes to the same attribute value.
 */
internal class AspHtmlFileReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val value = element as? XmlAttributeValue ?: return PsiReference.EMPTY_ARRAY
        if (value.containingFile.viewProvider !is AspFileViewProvider) return PsiReference.EMPTY_ARRAY
        val attribute = value.parent as? XmlAttribute ?: return PsiReference.EMPTY_ARRAY
        if (attribute.name.lowercase() !in FILE_ATTRIBUTES) return PsiReference.EMPTY_ARRAY

        val elementText = value.text
        val valueStart = value.valueTextRange.startOffset - value.textRange.startOffset
        val scriptletStart = elementText.indexOf("<%", valueStart)
        val documentText = value.containingFile.viewProvider.document?.charsSequence
        val scriptletStartsAfterValue = documentText?.startsWith("<%", value.textRange.endOffset) == true
        if (scriptletStart <= valueStart && !scriptletStartsAfterValue) return PsiReference.EMPTY_ARRAY

        val staticPrefix = elementText.substring(
            valueStart,
            scriptletStart.takeIf { it > valueStart } ?: elementText.length
        )
        val pathLength = staticPrefix.indexOfAny(charArrayOf('?', '#'))
            .takeIf { it >= 0 } ?: staticPrefix.length
        val path = staticPrefix.substring(0, pathLength)
        if (path.isBlank() || path.startsWith("#") || path.startsWith("//") || URL_SCHEME.matches(path)) {
            return PsiReference.EMPTY_ARRAY
        }

        val references = SafeAspFileReferenceSet(path, value, valueStart, this).allReferences
        return Array(references.size) { index -> references[index] }
    }

    private companion object {
        val FILE_ATTRIBUTES = setOf("href", "src", "action", "formaction", "background")
        val URL_SCHEME = Regex("[A-Za-z][A-Za-z0-9+.-]*:.*")
    }
}

private class SafeAspFileReferenceSet(
    path: String,
    element: PsiElement,
    startInElement: Int,
    provider: PsiReferenceProvider
) : FileReferenceSet(path, element, startInElement, provider, true) {
    override fun createFileReference(range: TextRange, index: Int, text: String): FileReference {
        return SafeAspFileReference(this, range, index, text)
    }
}

private class SafeAspFileReference(
    referenceSet: FileReferenceSet,
    range: TextRange,
    index: Int,
    text: String
) : FileReference(referenceSet, range, index, text) {
    override fun handleElementRename(newElementName: String): PsiElement {
        return replacePhysicalRange(rangeInElement, newElementName)
    }

    override fun bindToElement(element: PsiElement): PsiElement {
        val target = element as? PsiFileSystemItem
            ?: throw IncorrectOperationException("Cannot bind ASP file reference to $element")
        return super.bindToElement(target)
    }

    override fun rename(newName: String): PsiElement {
        return replacePhysicalRange(
            TextRange(fileReferenceSet.startInElement, rangeInElement.endOffset),
            newName
        )
    }

    override fun fixRefText(newText: String): PsiElement {
        return replacePhysicalRange(rangeInElement, newText)
    }

    private fun replacePhysicalRange(relativeRange: TextRange, replacement: String): PsiElement {
        val currentElement = element
        val file = currentElement.containingFile
        val documentManager = PsiDocumentManager.getInstance(currentElement.project)
        val document = documentManager.getDocument(file)
            ?: throw IncorrectOperationException("No document for ${file.name}")
        val elementStart = currentElement.textRange.startOffset
        val absoluteRange = relativeRange.shiftRight(elementStart)
        if (absoluteRange.startOffset < 0 || absoluteRange.endOffset > document.textLength) {
            throw IncorrectOperationException("Invalid ASP file reference range: $absoluteRange")
        }
        document.replaceString(absoluteRange.startOffset, absoluteRange.endOffset, replacement)
        return currentElement
    }
}
