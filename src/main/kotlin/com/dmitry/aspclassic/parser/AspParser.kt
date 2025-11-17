package com.dmitry.aspclassic.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

/**
 * ASP parser - creates AST nodes for all tokens
 */
class AspParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        println("PARSER: Starting parse for root: $root")

        val marker = builder.mark()

        while (!builder.eof()) {
            val tokenType = builder.tokenType
            val tokenText = builder.tokenText?.take(20) ?: ""
            println("PARSER: Processing token: $tokenType '$tokenText...'")

            // Create leaf node for every token
            when (tokenType) {
                AspTokenType.HTML_CONTENT -> {
                    val htmlMarker = builder.mark()
                    builder.advanceLexer()
                    htmlMarker.done(AspTokenType.HTML_CONTENT)
                    println("PARSER: Created HTML_CONTENT node")
                }
                AspTokenType.ASP_OPEN_TAG -> {
                    val openMarker = builder.mark()
                    builder.advanceLexer()
                    openMarker.done(AspTokenType.ASP_OPEN_TAG)
                    println("PARSER: Created ASP_OPEN_TAG node")
                }
                AspTokenType.ASP_CLOSE_TAG -> {
                    val closeMarker = builder.mark()
                    builder.advanceLexer()
                    closeMarker.done(AspTokenType.ASP_CLOSE_TAG)
                    println("PARSER: Created ASP_CLOSE_TAG node")
                }
                AspTokenType.ASP_SCRIPT_CONTENT -> {
                    val scriptMarker = builder.mark()
                    builder.advanceLexer()
                    scriptMarker.done(AspTokenType.ASP_SCRIPT_CONTENT)
                    println("PARSER: Created ASP_SCRIPT_CONTENT node")
                }
                else -> {
                    println("PARSER: Unknown token, skipping: $tokenType")
                    builder.advanceLexer()
                }
            }
        }

        marker.done(root)
        println("PARSER: Finished parsing - created AST")
        return builder.treeBuilt
    }
}