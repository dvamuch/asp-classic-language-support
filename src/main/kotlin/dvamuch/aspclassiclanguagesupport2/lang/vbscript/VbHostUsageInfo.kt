package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.injected.editor.DocumentWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.usageView.UsageInfo

/** Convert short-lived injected PSI usages to stable ranges in the containing ASP file. */
internal object VbHostUsageInfo {
    fun from(usage: UsageInfo): UsageInfo {
        return if (ApplicationManager.getApplication().isReadAccessAllowed) {
            fromInReadAction(usage)
        } else {
            ReadAction.compute<UsageInfo, RuntimeException> { fromInReadAction(usage) }
        }
    }

    private fun fromInReadAction(usage: UsageInfo): UsageInfo {
        val element = usage.element ?: return usage
        val injectedFile = element.containingFile ?: return usage
        val document = PsiDocumentManager.getInstance(element.project).getDocument(injectedFile)
            as? DocumentWindow ?: return usage
        val hostFile = InjectedLanguageManager.getInstance(element.project).getTopLevelFile(element)
        val rangeInElement = usage.rangeInElement ?: TextRange(0, element.textLength)
        val injectedRange = rangeInElement.shiftRight(element.textRange.startOffset)
        val hostStart = document.injectedToHost(injectedRange.startOffset)
        val hostEnd = document.injectedToHost(injectedRange.endOffset)
        if (hostStart < 0 || hostEnd < hostStart || hostEnd > hostFile.textLength) return usage

        return UsageInfo(hostFile, hostStart, hostEnd, usage.isNonCodeUsage).also {
            it.setDynamicUsage(usage.isDynamicUsage)
        }
    }
}
