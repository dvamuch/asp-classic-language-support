package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.openapi.util.TextRange

enum class AspIncludeKind {
    FILE,
    VIRTUAL
}

data class AspIncludeDirective(
    val kind: AspIncludeKind,
    val path: String,
    val pathRange: TextRange
)

object AspIncludeDirectiveParser {
    private val includePattern = Regex(
        """<!--\s*#include\s+(file|virtual)\s*=\s*(?:"([^"]*)"|'([^']*)')\s*-->""",
        RegexOption.IGNORE_CASE
    )

    fun parse(commentText: String): AspIncludeDirective? {
        val match = includePattern.matchEntire(commentText) ?: return null
        return directive(match)
    }

    internal fun findAll(fileText: CharSequence): List<AspIncludeDirective> {
        return includePattern.findAll(fileText)
            .mapNotNull(::directive)
            .toList()
    }

    private fun directive(match: MatchResult): AspIncludeDirective? {
        val pathGroup = match.groups[2] ?: match.groups[3]
            ?: error("Include directive path group is missing")
        val path = pathGroup.value.trim()
        if (path.isEmpty()) return null

        val leadingWhitespace = pathGroup.value.indexOfFirst { !it.isWhitespace() }
        val pathStart = pathGroup.range.first + leadingWhitespace.coerceAtLeast(0)
        return AspIncludeDirective(
            kind = AspIncludeKind.valueOf(match.groupValues[1].uppercase()),
            path = path,
            pathRange = TextRange(pathStart, pathStart + path.length)
        )
    }
}
