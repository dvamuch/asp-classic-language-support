package com.dmitry.aspclassic.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

/**
 * Simple ASP parser - creates flat structure of tokens
 */
class AspParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        println("PARSER: Starting parse for root: $root")

        val marker = builder.mark()

        while (!builder.eof()) {
            val tokenType = builder.tokenType
            println("PARSER: Processing token: $tokenType '${builder.tokenText?.take(20)}...'")

            when (tokenType) {
                AspTokenType.HTML_CONTENT -> {
                    // Create HTML content element
                    builder.advanceLexer()
                }
                AspTokenType.ASP_OPEN_TAG,
                AspTokenType.ASP_CLOSE_TAG,
                AspTokenType.ASP_SCRIPT_CONTENT -> {
                    // Create ASP block elements
                    builder.advanceLexer()
                }
                else -> {
                    println("PARSER: Unknown token, skipping: $tokenType")
                    builder.advanceLexer()
                }
            }
        }

        marker.done(root)
        println("PARSER: Finished parsing")
        return builder.treeBuilt
    }
}