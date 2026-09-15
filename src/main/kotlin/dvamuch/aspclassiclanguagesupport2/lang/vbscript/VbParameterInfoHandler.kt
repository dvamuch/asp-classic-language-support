package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoHandler
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.psi.PsiElement

internal class VbParameterInfoHandler : ParameterInfoHandler<PsiElement, VbResolvedCall> {
    override fun findElementForParameterInfo(context: CreateParameterInfoContext): PsiElement? {
        val owner = VbCallSignatureSupport.findOwner(context.file, context.offset) ?: return null
        val call = VbCallSignatureSupport.resolve(owner) ?: return null
        context.itemsToShow = arrayOf(call)
        return owner
    }

    override fun showParameterInfo(element: PsiElement, context: CreateParameterInfoContext) {
        context.showHint(element, VbCallSignatureSupport.hintOffset(element), this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): PsiElement? {
        val owner = VbCallSignatureSupport.findOwner(context.file, context.offset) ?: return null
        return owner.takeIf { candidate -> VbCallSignatureSupport.resolve(candidate) != null }
    }

    override fun updateParameterInfo(element: PsiElement, context: UpdateParameterInfoContext) {
        if (element !== context.parameterOwner) context.parameterOwner = element
        context.setCurrentParameter(VbCallSignatureSupport.currentParameterIndex(element, context.offset))
    }

    override fun updateUI(call: VbResolvedCall, context: ParameterInfoUIContext) {
        val presentation = parameterPresentation(call.member, context.currentParameterIndex)
        context.setupUIComponentPresentation(
            presentation.text,
            presentation.highlightStart,
            presentation.highlightEnd,
            false,
            false,
            false,
            context.defaultParameterColor
        )
    }

    private fun parameterPresentation(member: VbObjectMember, currentParameter: Int): ParameterPresentation {
        val parameters = member.parameters.orEmpty()
        val renderedParameters = parameters.map(VbObjectParameter::presentation)
        val prefix = "${member.name}("
        val text = renderedParameters.joinToString(prefix = prefix, postfix = ")")
        if (currentParameter !in renderedParameters.indices) return ParameterPresentation(text, -1, -1)

        val highlightStart = prefix.length + renderedParameters.take(currentParameter).sumOf { it.length + 2 }
        return ParameterPresentation(
            text,
            highlightStart,
            highlightStart + renderedParameters[currentParameter].length
        )
    }
}

private data class ParameterPresentation(
    val text: String,
    val highlightStart: Int,
    val highlightEnd: Int
)
