package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbBareCallArgs
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbCallArgs
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbCallStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPostfixRefExpr
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPostfixSuffix

internal data class VbResolvedCall(
    val owner: PsiElement,
    val objectType: VbObjectType,
    val member: VbObjectMember
)

internal data class VbCallArgumentContext(
    val call: VbResolvedCall,
    val parameterIndex: Int
) {
    val parameter: VbObjectParameter?
        get() = call.member.parameters?.getOrNull(parameterIndex)
}

internal object VbCallSignatureSupport {
    fun findOwner(file: PsiFile, offset: Int): PsiElement? {
        val boundedOffset = offset.coerceIn(0, file.textLength)
        val candidates = buildList {
            if (boundedOffset < file.textLength) add(file.findElementAt(boundedOffset))
            if (boundedOffset > 0) add(file.findElementAt(boundedOffset - 1))
        }
        return candidates.asSequence()
            .filterNotNull()
            .mapNotNull { element -> nearestOwner(element, boundedOffset) }
            .firstOrNull()
    }

    fun contextAt(position: PsiElement, offset: Int): VbCallArgumentContext? {
        val owner = nearestOwner(position, offset)
            ?: position.containingFile?.let { file -> findOwner(file, offset) }
            ?: return null
        val call = resolve(owner, position) ?: return null
        return VbCallArgumentContext(call, currentParameterIndex(owner, offset))
    }

    fun resolve(owner: PsiElement, place: PsiElement = owner): VbResolvedCall? {
        val path = memberPath(owner) ?: return null
        if (path.size < 2) return null

        val methodName = path.last()
        val objectType = VbObjectTypeResolver.resolve(path.dropLast(1), place) ?: return null
        val member = objectType.member(methodName)
            ?.takeIf { candidate -> candidate.parameters != null }
            ?: return null
        return VbResolvedCall(owner, objectType, member)
    }

    fun currentParameterIndex(owner: PsiElement, offset: Int): Int {
        val fileText = owner.containingFile?.text ?: return 0
        val ownerStart = owner.textRange.startOffset
        val scanStart = if (owner is VbCallArgs) ownerStart + 1 else ownerStart
        val scanEnd = offset.coerceIn(scanStart.coerceAtMost(fileText.length), owner.textRange.endOffset.coerceAtMost(fileText.length))

        var parameterIndex = 0
        var nestedParentheses = 0
        var inString = false
        var current = scanStart
        while (current < scanEnd) {
            when (fileText[current]) {
                '"' -> {
                    if (inString && current + 1 < scanEnd && fileText[current + 1] == '"') {
                        current++
                    } else {
                        inString = !inString
                    }
                }

                '\'' -> if (!inString) return parameterIndex
                '(' -> if (!inString) nestedParentheses++
                ')' -> if (!inString && nestedParentheses > 0) nestedParentheses--
                ',' -> if (!inString && nestedParentheses == 0) parameterIndex++
            }
            current++
        }
        return parameterIndex
    }

    fun hintOffset(owner: PsiElement): Int {
        return owner.textRange.startOffset + if (owner is VbCallArgs) 1 else 0
    }

    private fun nearestOwner(element: PsiElement, offset: Int): PsiElement? {
        var current: PsiElement? = element
        while (current != null) {
            if ((current is VbCallArgs || current is VbBareCallArgs) && containsOffset(current, offset)) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun containsOffset(element: PsiElement, offset: Int): Boolean {
        val range = element.textRange
        return offset >= range.startOffset && offset <= range.endOffset
    }

    private fun memberPath(owner: PsiElement): List<String>? {
        if (owner is VbCallArgs) {
            val suffix = owner.parent as? VbPostfixSuffix
            if (suffix != null) return postfixMemberPath(suffix)
        }

        val callStatement = owner.parent as? VbCallStmt ?: return null
        return callStatement.qualifiedIdentifier?.idList
            ?.mapNotNull { id -> (id as? VbNamedElement)?.name }
            ?.takeIf { path -> path.isNotEmpty() }
    }

    private fun postfixMemberPath(callSuffix: VbPostfixSuffix): List<String>? {
        val reference = callSuffix.parent as? VbPostfixRefExpr ?: return null
        val suffixIndex = reference.postfixSuffixList.indexOfFirst { suffix -> suffix === callSuffix }
        if (suffixIndex < 0) return null

        val rootName = (reference.id as? VbNamedElement)?.name ?: return null
        return buildList {
            add(rootName)
            reference.postfixSuffixList.take(suffixIndex).forEach { suffix ->
                (suffix.id as? VbNamedElement)?.name?.let(::add)
            }
        }
    }
}
