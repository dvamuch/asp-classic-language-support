package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import dvamuch.aspclassiclanguagesupport2.lang.AspFileType

class AspFileFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
    override fun canFindUsages(element: PsiElement): Boolean {
        return element is PsiFile && element.fileType == AspFileType
    }

    override fun createFindUsagesHandler(
        element: PsiElement,
        forHighlightUsages: Boolean
    ): FindUsagesHandler = AspFileFindUsagesHandler(element)
}

private class AspFileFindUsagesHandler(element: PsiElement) : FindUsagesHandler(element) {
    override fun processElementUsages(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions
    ): Boolean {
        val withoutAspIncludes = Processor<UsageInfo> { usageInfo ->
            if (usageInfo.reference is AspIncludeReference) true else processor.process(usageInfo)
        }
        return super.processElementUsages(element, withoutAspIncludes, options)
    }
}
