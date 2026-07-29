package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclarationUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import java.util.concurrent.TimeUnit

class VbFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
    override fun canFindUsages(element: PsiElement): Boolean {
        val id = element as? VbId ?: return false
        return VbDeclarationUtil.declaration(id) != null
    }

    override fun createFindUsagesHandler(
        element: PsiElement,
        forHighlightUsages: Boolean
    ): FindUsagesHandler = VbFindUsagesHandler(
        element,
        ActionManagerEx.getInstanceEx().lastPreformedActionId == IdeActions.ACTION_GOTO_DECLARATION
    )
}

private class VbFindUsagesHandler(
    element: PsiElement,
    private val invokedFromGotoDeclaration: Boolean
) : FindUsagesHandler(element) {
    override fun processElementUsages(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions
    ): Boolean {
        val startedAt = System.nanoTime()
        var usageFound = false
        val fastTrack = options.fastTrack
        val userScope = options.searchScope
        val effectiveScope = ReadAction.compute<SearchScope, RuntimeException> {
            VbUsageSearchScope.forElement(element, userScope)
        }
        options.fastTrack = null
        options.searchScope = effectiveScope
        val completed = try {
            super.processElementUsages(
                element,
                Processor { usage ->
                    usageFound = true
                    processor.process(VbHostUsageInfo.from(usage))
                },
                options
            )
        } finally {
            // The platform's fast-track collector constructs UsageInfo directly from injected
            // references after this handler returns, bypassing the host-range conversion above.
            options.fastTrack = fastTrack
            options.searchScope = userScope
        }

        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
        val delayMillis = VbNoUsagesHintStabilizer.remainingDelayMillis(
            elapsedMillis,
            invokedFromGotoDeclaration,
            usageFound,
            completed
        )
        if (delayMillis > 0) {
            ProgressManager.checkCanceled()
            Thread.sleep(delayMillis)
            ProgressManager.checkCanceled()
        }
        return completed
    }
}

/**
 * Show Usages information hints hide on the next key event. A very fast empty search launched by
 * Command/Ctrl+Click can therefore show its hint before the navigation modifier is released and
 * immediately close it again. Keep only that empty navigation search alive past the input gesture.
 */
internal object VbNoUsagesHintStabilizer {
    private const val MINIMUM_SEARCH_DURATION_MILLIS = 400L

    fun remainingDelayMillis(
        elapsedMillis: Long,
        invokedFromGotoDeclaration: Boolean,
        usageFound: Boolean,
        completed: Boolean
    ): Long {
        if (!invokedFromGotoDeclaration || usageFound || !completed) return 0
        return (MINIMUM_SEARCH_DURATION_MILLIS - elapsedMillis).coerceAtLeast(0)
    }
}
