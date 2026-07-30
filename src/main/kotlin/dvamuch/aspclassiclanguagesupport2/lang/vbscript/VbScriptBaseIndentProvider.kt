package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.injected.editor.DocumentWindow
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

internal fun interface VbScriptBaseIndentProvider {
    fun indentFor(node: ASTNode): Int

    companion object {
        val NONE = VbScriptBaseIndentProvider { 0 }

        fun forFile(file: PsiFile, tabSize: Int): VbScriptBaseIndentProvider {
            val document = PsiDocumentManager.getInstance(file.project).getDocument(file) as? DocumentWindow
                ?: return NONE
            val hostDocument = document.delegate
            val hostRanges = document.hostRanges
            return VbScriptBaseIndentProvider { node ->
                val hostOffset = document.injectedToHost(node.startOffset)
                val shred = hostRanges.firstOrNull { range ->
                    hostOffset >= range.startOffset && hostOffset <= range.endOffset
                } ?: return@VbScriptBaseIndentProvider 0
                val line = hostDocument.getLineNumber(shred.startOffset)
                val lineStart = hostDocument.getLineStartOffset(line)
                leadingIndentWidth(hostDocument.charsSequence, lineStart, shred.startOffset, tabSize)
            }
        }

        private fun leadingIndentWidth(
            text: CharSequence,
            startOffset: Int,
            endOffset: Int,
            tabSize: Int
        ): Int {
            var width = 0
            var offset = startOffset
            while (offset < endOffset) {
                when (text[offset]) {
                    ' ' -> width++
                    '\t' -> width += tabSize - width % tabSize
                    else -> return width
                }
                offset++
            }
            return width
        }
    }
}
