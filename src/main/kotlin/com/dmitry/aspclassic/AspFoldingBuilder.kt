package com.dmitry.aspclassic

import com.dmitry.aspclassic.psi.AspTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Code folding builder for ASP Classic
 */
class AspFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        PsiTreeUtil.findChildrenOfAnyType(root, PsiElement::class.java).forEach { element ->
            when (element.node.elementType) {
                AspTypes.ASP_BLOCK -> {
                    val textRange = element.textRange
                    if (textRange.length > 10) { // Only fold if block is substantial
                        descriptors.add(FoldingDescriptor(element.node, textRange))
                    }
                }
                AspTypes.FUNCTION_DECLARATION, AspTypes.SUB_DECLARATION -> {
                    val textRange = element.textRange
                    if (textRange.length > 20) {
                        descriptors.add(FoldingDescriptor(element.node, textRange))
                    }
                }
                AspTypes.IF_STATEMENT, AspTypes.FOR_STATEMENT, AspTypes.WHILE_STATEMENT,
                AspTypes.DO_STATEMENT, AspTypes.SELECT_STATEMENT, AspTypes.WITH_STATEMENT -> {
                    val textRange = element.textRange
                    if (textRange.length > 15) {
                        descriptors.add(FoldingDescriptor(element.node, textRange))
                    }
                }
            }
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        return when (node.elementType) {
            AspTypes.ASP_BLOCK -> "<%...%>"
            AspTypes.FUNCTION_DECLARATION -> "Function..."
            AspTypes.SUB_DECLARATION -> "Sub..."
            AspTypes.IF_STATEMENT -> "If..."
            AspTypes.FOR_STATEMENT -> "For..."
            AspTypes.WHILE_STATEMENT -> "While..."
            AspTypes.DO_STATEMENT -> "Do..."
            AspTypes.SELECT_STATEMENT -> "Select..."
            AspTypes.WITH_STATEMENT -> "With..."
            else -> "..."
        }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}

