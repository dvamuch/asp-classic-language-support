package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

class AspLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var startOffset = 0
    private var endOffset = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        tokenType = null
        locateToken(startOffset)
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        locateToken(tokenEnd)
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun locateToken(offset: Int) {
        if (offset >= endOffset) {
            tokenType = null
            tokenStart = endOffset
            tokenEnd = endOffset
            return
        }

        tokenStart = offset
        if (startsWithAspTag(offset)) {
            val closeIndex = findAspClose(offset + 2)
            tokenEnd = if (closeIndex == -1) endOffset else (closeIndex + 2).coerceAtMost(endOffset)
            tokenType = AspTokenTypes.OUTER
            return
        }

        val nextAsp = findNextAspOpen(offset + 1)
        tokenEnd = if (nextAsp == -1) endOffset else nextAsp
        tokenType = AspTokenTypes.TEMPLATE_DATA
    }

    private fun startsWithAspTag(offset: Int): Boolean {
        if (offset + 1 >= endOffset) return false
        return buffer[offset] == '<' && buffer[offset + 1] == '%'
    }

    private fun findNextAspOpen(offset: Int): Int {
        var i = offset
        while (i + 1 < endOffset) {
            if (buffer[i] == '<' && buffer[i + 1] == '%') return i
            i++
        }
        return -1
    }

    private fun findAspClose(offset: Int): Int {
        var i = offset
        while (i + 1 < endOffset) {
            if (buffer[i] == '%' && buffer[i + 1] == '>') return i
            i++
        }
        return -1
    }
}
