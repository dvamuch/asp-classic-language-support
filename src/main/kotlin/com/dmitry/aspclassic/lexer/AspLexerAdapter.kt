package com.dmitry.aspclassic.lexer

import com.intellij.lexer.FlexAdapter
import com.dmitry.aspclassic.parser.AspLexer

/**
 * Lexer adapter for ASP Classic
 */
class AspLexerAdapter : FlexAdapter(AspLexer(null))

