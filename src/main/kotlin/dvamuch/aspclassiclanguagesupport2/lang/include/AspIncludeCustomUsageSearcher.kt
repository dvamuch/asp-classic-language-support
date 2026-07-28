package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.find.findUsages.CustomUsageSearcher
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usages.Usage
import com.intellij.util.Processor
import dvamuch.aspclassiclanguagesupport2.lang.AspFileType

class AspIncludeCustomUsageSearcher : CustomUsageSearcher() {
    override fun processElementUsages(
        element: PsiElement,
        processor: Processor<in Usage>,
        options: FindUsagesOptions
    ) {
        val file = element as? PsiFile ?: return
        if (file.fileType != AspFileType) return

        ReferencesSearch.search(file, options.searchScope, false).forEach(
            Processor<PsiReference> { reference ->
                val includeReference = reference as? AspIncludeReference
                    ?: return@Processor true
                processor.process(AspIncludeUsage(includeReference))
            }
        )
    }
}
