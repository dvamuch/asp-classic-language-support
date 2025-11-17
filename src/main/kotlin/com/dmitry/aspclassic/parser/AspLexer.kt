package com.dmitry.aspclassic.parser

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * ASP lexer - splits source code into tokens
 */
class AspLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var tokenType: IElementType? = null
    private var state = LexerState.OUTSIDE_ASP

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.state = LexerState.fromInt(initialState)
        advance()
    }

    override fun getState(): Int = state.ordinal

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEnd

    override fun advance() {
        tokenStart = tokenEnd

        if (tokenStart >= bufferEnd) {
            tokenType = null
            println("LEXER: EOF at position $tokenStart")
            return
        }

        // Логируем текущее состояние
        println("LEXER: advance() - start=$tokenStart, state=$state, char='${if (tokenStart < buffer.length) buffer[tokenStart] else '?'}'")

        when (state) {
            LexerState.OUTSIDE_ASP -> parseOutsideAsp()
            LexerState.INSIDE_ASP -> parseInsideAsp()
            LexerState.AFTER_OPEN -> parseAfterOpen()
            LexerState.AFTER_CLOSE -> parseAfterClose()
        }

        println("LEXER: produced token: type=$tokenType, start=$tokenStart, end=$tokenEnd")
    }

    private fun parseOutsideAsp() {
        val openTagIndex = findOpenTag(tokenStart)

        if (openTagIndex != -1) {
            if (openTagIndex > tokenStart) {
                // HTML content before ASP tag
                tokenEnd = openTagIndex
                tokenType = AspTokenType.HTML_CONTENT
            } else {
                // ASP open tag
                tokenEnd = tokenStart + 2
                tokenType = AspTokenType.ASP_OPEN_TAG
                state = LexerState.AFTER_OPEN
            }
        } else {
            // Only HTML content till end
            tokenEnd = bufferEnd
            tokenType = AspTokenType.HTML_CONTENT
        }
    }

    private fun parseAfterOpen() {
        // We just processed "<%" - now look for "%>"
        val closeTagIndex = findCloseTag(tokenStart)

        if (closeTagIndex != -1) {
            if (closeTagIndex > tokenStart) {
                // ASP script content before close tag
                tokenEnd = closeTagIndex
                tokenType = AspTokenType.ASP_SCRIPT_CONTENT
            } else {
                // Close tag starts immediately - emit empty ASP content
                tokenEnd = tokenStart
                tokenType = AspTokenType.ASP_SCRIPT_CONTENT
            }
            state = LexerState.INSIDE_ASP
        } else {
            // No close tag - everything is ASP script
            tokenEnd = bufferEnd
            tokenType = AspTokenType.ASP_SCRIPT_CONTENT
            state = LexerState.INSIDE_ASP
        }
    }

    private fun parseInsideAsp() {
        // We're inside ASP content - look for close tag
        val closeTagIndex = findCloseTag(tokenStart)

        if (closeTagIndex != -1) {
            if (closeTagIndex > tokenStart) {
                // ASP content before close tag
                tokenEnd = closeTagIndex
                tokenType = AspTokenType.ASP_SCRIPT_CONTENT
            } else {
                // Close tag starts immediately
                tokenEnd = tokenStart + 2
                tokenType = AspTokenType.ASP_CLOSE_TAG
                state = LexerState.AFTER_CLOSE
            }
        } else {
            // No close tag - rest is ASP content
            tokenEnd = bufferEnd
            tokenType = AspTokenType.ASP_SCRIPT_CONTENT
        }
    }

    private fun parseAfterClose() {
        // We just processed "%>" - now back to HTML content
        state = LexerState.OUTSIDE_ASP
        parseOutsideAsp()
    }

    private fun findOpenTag(startFrom: Int): Int {
        for (i in startFrom until bufferEnd - 1) {
            if (buffer[i] == '<' && buffer[i + 1] == '%') {
                return i
            }
        }
        return -1
    }

    private fun findCloseTag(startFrom: Int): Int {
        for (i in startFrom until bufferEnd - 1) {
            if (buffer[i] == '%' && buffer[i + 1] == '>') {
                return i
            }
        }
        return -1
    }

    private enum class LexerState {
        OUTSIDE_ASP,    // Parsing HTML content
        INSIDE_ASP,     // Inside ASP script content
        AFTER_OPEN,     // Just after "<%" tag
        AFTER_CLOSE;    // Just after "%>" tag

        companion object {
            fun fromInt(value: Int): LexerState {
                return values().getOrElse(value.coerceIn(0, values().size - 1)) { OUTSIDE_ASP }
            }
        }
    }
}