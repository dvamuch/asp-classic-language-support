package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.xml.XmlFormattingPolicy
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.xml.XmlElementType
import com.intellij.xml.template.formatter.AbstractXmlTemplateFormattingModelBuilder
import com.intellij.xml.template.formatter.TemplateLanguageBlock
import com.intellij.xml.template.formatter.TemplateXmlBlock

class AspFormattingModelBuilder : AbstractXmlTemplateFormattingModelBuilder() {
    override fun isTemplateFile(file: PsiFile): Boolean = file is AspFile

    override fun isOuterLanguageElement(element: PsiElement): Boolean {
        return element.node?.elementType == AspTokenTypes.OUTER
    }

    override fun isMarkupLanguageElement(element: PsiElement): Boolean {
        return element.node?.elementType == AspTokenTypes.TEMPLATE_DATA
    }

    override fun createTemplateLanguageBlock(
        node: ASTNode,
        settings: CodeStyleSettings,
        xmlFormattingPolicy: XmlFormattingPolicy,
        indent: Indent?,
        alignment: Alignment?,
        wrap: Wrap?
    ): TemplateLanguageBlock {
        return AspTemplateLanguageBlock(
            builder = this,
            node = node,
            wrap = wrap,
            alignment = alignment,
            settings = settings,
            xmlFormattingPolicy = xmlFormattingPolicy,
            // Embedded formatters (e.g. JavaScript in <script>) may not supply an indent.
            indent = indent ?: Indent.getNoneIndent()
        )
    }

    override fun mergeWithTemplateBlocks(
        blocks: List<Block>,
        settings: CodeStyleSettings,
        xmlFormattingPolicy: XmlFormattingPolicy,
        indent: Indent?
    ): List<Block> {
        val patchedBlocks = blocks.flatMap(::unwrapTextBlockContainingAsp)
        return super.mergeWithTemplateBlocks(patchedBlocks, settings, xmlFormattingPolicy, indent)
    }

    override fun buildTemplateLanguageBlocksInside(
        file: PsiFile,
        range: TextRange,
        settings: CodeStyleSettings,
        xmlFormattingPolicy: XmlFormattingPolicy,
        indent: Indent?
    ): List<Block> {
        val markupFile = file.viewProvider.getPsi(com.intellij.lang.html.HTMLLanguage.INSTANCE) ?: return emptyList()
        val outerElements = outerElements(markupFile)
        if (outerElements.isEmpty()) return emptyList()

        var low = 0
        var high = outerElements.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (outerElements[middle].textRange.endOffset <= range.startOffset) {
                low = middle + 1
            } else {
                high = middle
            }
        }

        return outerElements.asSequence()
            .drop(low)
            .takeWhile { element -> element.textRange.startOffset < range.endOffset }
            .filter { element -> range.intersects(element.textRange) }
            .map { element ->
                createTemplateLanguageBlock(
                    element.node,
                    settings,
                    xmlFormattingPolicy,
                    indent ?: Indent.getNoneIndent(),
                    null,
                    null
                )
            }
            .toList()
    }

    private fun outerElements(markupFile: PsiFile): List<AspOuterPsiElement> {
        return CachedValuesManager.getCachedValue(markupFile) {
            val startedAt = System.nanoTime()
            val elements = SyntaxTraverser.psiTraverser(markupFile)
                .filter(AspOuterPsiElement::class.java)
                .toList()
                .sortedBy { element -> element.textRange.startOffset }
            AspFormatPerformanceTrace.record("html-template-block-index", markupFile, startedAt)
            CachedValueProvider.Result.create(elements, markupFile)
        }
    }

    private fun unwrapTextBlockContainingAsp(block: Block): List<Block> {
        val xmlBlock = block as? TemplateXmlBlock ?: return listOf(block)
        if (xmlBlock.node.elementType != XmlElementType.XML_TEXT || xmlBlock.isLeaf) return listOf(block)

        return if (xmlBlock.isTextContainingTemplateElements) xmlBlock.subBlocks else listOf(block)
    }
}

private class AspTemplateLanguageBlock(
    builder: AbstractXmlTemplateFormattingModelBuilder,
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    settings: CodeStyleSettings,
    xmlFormattingPolicy: XmlFormattingPolicy,
    indent: Indent
) : TemplateLanguageBlock(builder, node, wrap, alignment, settings, xmlFormattingPolicy, indent) {
    override fun getChildIndent(node: ASTNode): Indent = Indent.getNoneIndent()

    override fun getSpacing(child: TemplateLanguageBlock): Spacing? = null

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = null
}
