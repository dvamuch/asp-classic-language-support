package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLexerAdapter
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes

/** Produces one VBScript token stream across all scriptlets in an ASP document. */
class AspLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null
    private var mode = MODE_HTML
    private var modeAfterToken = MODE_HTML
    private var scriptEnd = 0
    private var delegate: Lexer? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        mode = if (initialState == MODE_SCRIPT) MODE_SCRIPT else MODE_HTML
        modeAfterToken = mode
        delegate = null
        locateToken(startOffset)
    }

    override fun getState(): Int = mode
    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        mode = modeAfterToken
        locateToken(tokenEnd)
    }

    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = bufferEnd

    private fun locateToken(offset: Int) {
        if (offset >= bufferEnd) {
            tokenType = null
            tokenStart = bufferEnd
            tokenEnd = bufferEnd
            return
        }
        if (mode == MODE_HTML) locateHtmlToken(offset) else locateScriptToken(offset)
    }

    private fun locateHtmlToken(offset: Int) {
        tokenStart = offset
        when {
            startsWith(offset, "<%--") -> {
                tokenEnd = findEnd(offset + 4, "--%>")
                tokenType = VbTypes.ASP_CONTROL
                modeAfterToken = MODE_HTML
            }
            startsWith(offset, "<%@") -> {
                tokenEnd = findEnd(offset + 3, "%>")
                tokenType = VbTypes.ASP_CONTROL
                modeAfterToken = MODE_HTML
            }
            startsWith(offset, "<%=") -> {
                tokenEnd = offset + 3
                tokenType = VbTypes.ASP_EXPR_OPEN
                modeAfterToken = MODE_SCRIPT
                prepareDelegate(tokenEnd)
            }
            startsWith(offset, "<%") -> {
                tokenEnd = offset + 2
                tokenType = VbTypes.ASP_OPEN
                modeAfterToken = MODE_SCRIPT
                prepareDelegate(tokenEnd)
            }
            else -> {
                val nextAsp = indexOf("<%", offset + 1)
                tokenEnd = if (nextAsp < 0) bufferEnd else nextAsp
                tokenType = AspTokenTypes.TEMPLATE_DATA
                modeAfterToken = MODE_HTML
            }
        }
    }

    private fun locateScriptToken(offset: Int) {
        if (startsWith(offset, "%>")) {
            emitClose(offset)
            return
        }

        var activeDelegate = delegate
        if (activeDelegate == null || offset > scriptEnd) {
            prepareDelegate(offset)
            activeDelegate = delegate
        }

        if (activeDelegate?.tokenType != null) {
            tokenStart = activeDelegate.tokenStart
            tokenEnd = activeDelegate.tokenEnd
            tokenType = activeDelegate.tokenType
            modeAfterToken = MODE_SCRIPT
            activeDelegate.advance()
            return
        }

        if (scriptEnd < bufferEnd && startsWith(scriptEnd, "%>")) {
            emitClose(scriptEnd)
        } else {
            tokenType = null
            tokenStart = bufferEnd
            tokenEnd = bufferEnd
        }
    }

    private fun emitClose(offset: Int) {
        tokenStart = offset
        tokenEnd = (offset + 2).coerceAtMost(bufferEnd)
        tokenType = VbTypes.ASP_CLOSE
        modeAfterToken = MODE_HTML
        delegate = null
    }

    private fun prepareDelegate(offset: Int) {
        val close = indexOf("%>", offset)
        scriptEnd = if (close < 0) bufferEnd else close
        delegate = VbScriptLexerAdapter().also {
            it.start(buffer, offset.coerceAtMost(scriptEnd), scriptEnd, 0)
        }
    }

    private fun findEnd(from: Int, delimiter: String): Int {
        val index = indexOf(delimiter, from)
        return if (index < 0) bufferEnd else (index + delimiter.length).coerceAtMost(bufferEnd)
    }

    private fun indexOf(value: String, from: Int): Int {
        var index = from.coerceAtLeast(0)
        val last = bufferEnd - value.length
        while (index <= last) {
            if (startsWith(index, value)) return index
            index++
        }
        return -1
    }

    private fun startsWith(offset: Int, value: String): Boolean {
        if (offset < 0 || offset + value.length > bufferEnd) return false
        for (index in value.indices) {
            if (buffer[offset + index] != value[index]) return false
        }
        return true
    }

    private companion object {
        const val MODE_HTML = 0
        const val MODE_SCRIPT = 1
    }
}
