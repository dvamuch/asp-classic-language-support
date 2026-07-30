package dvamuch.aspclassiclanguagesupport2.lang.vbscript

internal class VbScriptControlFlowTracker {
    private val blocks = mutableListOf<BlockKind>()

    val depth: Int
        get() = blocks.size

    fun consume(rawLine: String): LineResult {
        val line = stripComment(rawLine).trim()
        if (line.isEmpty()) return LineResult(depth, false)
        val lower = line.lowercase()

        closingKind(lower)?.let { kind ->
            popThrough(kind)
            return LineResult(depth, true)
        }

        if (lower.startsWith("case ") || lower == "case else") {
            if (blocks.lastOrNull() == BlockKind.CASE) blocks.removeLast()
            val lineDepth = depth
            blocks.add(BlockKind.CASE)
            return LineResult(lineDepth, true)
        }

        if (isIfBranch(lower)) {
            val lineDepth = if (blocks.lastOrNull() == BlockKind.IF) (depth - 1).coerceAtLeast(0) else depth
            return LineResult(lineDepth, true)
        }

        openingKind(line, lower)?.let { kind ->
            val lineDepth = depth
            blocks.add(kind)
            return LineResult(lineDepth, true)
        }

        return LineResult(depth, false)
    }

    private fun popThrough(kind: BlockKind) {
        if (kind == BlockKind.SELECT && blocks.lastOrNull() == BlockKind.CASE) blocks.removeLast()
        val matchingIndex = blocks.indexOfLast { block -> block == kind }
        if (matchingIndex < 0) return
        while (blocks.lastIndex >= matchingIndex) blocks.removeLast()
    }

    private fun openingKind(line: String, lower: String): BlockKind? {
        if (Regex("^if\\b.*\\bthen\\s*$", RegexOption.IGNORE_CASE).matches(line) &&
            !lower.startsWith("elseif") && !lower.startsWith("else if")
        ) return BlockKind.IF
        return when {
            lower.startsWith("select case ") -> BlockKind.SELECT
            lower.startsWith("for ") -> BlockKind.FOR
            lower == "do" || lower.startsWith("do ") -> BlockKind.DO
            lower.startsWith("while ") -> BlockKind.WHILE
            lower.startsWith("with ") -> BlockKind.WITH
            lower.startsWith("class ") -> BlockKind.CLASS
            lower.startsWith("sub ") -> BlockKind.SUB
            lower.startsWith("function ") -> BlockKind.FUNCTION
            Regex("^property\\s+(get|let|set)\\b.*", RegexOption.IGNORE_CASE).matches(line) -> BlockKind.PROPERTY
            else -> null
        }
    }

    private fun closingKind(lower: String): BlockKind? = when {
        lower == "end if" -> BlockKind.IF
        lower == "end select" -> BlockKind.SELECT
        lower == "next" || lower.startsWith("next ") -> BlockKind.FOR
        lower == "loop" || lower.startsWith("loop ") -> BlockKind.DO
        lower == "wend" -> BlockKind.WHILE
        lower == "end with" -> BlockKind.WITH
        lower == "end class" -> BlockKind.CLASS
        lower == "end sub" -> BlockKind.SUB
        lower == "end function" -> BlockKind.FUNCTION
        lower.startsWith("end property") -> BlockKind.PROPERTY
        else -> null
    }

    private fun isIfBranch(lower: String): Boolean {
        return lower == "else" || lower.startsWith("elseif ") || lower.startsWith("else if ")
    }

    private fun stripComment(line: String): String {
        var inString = false
        var index = 0
        while (index < line.length) {
            when (line[index]) {
                '"' -> {
                    if (inString && index + 1 < line.length && line[index + 1] == '"') {
                        index++
                    } else {
                        inString = !inString
                    }
                }
                '\'' -> if (!inString) return line.substring(0, index)
            }
            index++
        }
        return line
    }

    data class LineResult(val indentLevel: Int, val isBoundary: Boolean)

    private enum class BlockKind {
        IF,
        SELECT,
        CASE,
        FOR,
        DO,
        WHILE,
        WITH,
        CLASS,
        SUB,
        FUNCTION,
        PROPERTY
    }
}
