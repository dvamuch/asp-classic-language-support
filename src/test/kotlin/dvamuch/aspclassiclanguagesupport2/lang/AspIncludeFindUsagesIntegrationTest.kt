package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.util.Processor
import dvamuch.aspclassiclanguagesupport2.lang.include.AspFileFindUsagesHandlerFactory
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeCustomUsageSearcher
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeReference
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeUsage

class AspIncludeFindUsagesIntegrationTest : BasePlatformTestCase() {
    fun testProducesTypedUsageForIncludeReference() {
        val target = myFixture.addFileToProject("site/includes/common.inc", "<% Dim sharedValue %>")
        myFixture.addFileToProject(
            "site/pages/default.asp",
            """<!-- #include file="../includes/common.inc" -->"""
        )
        val usages = collectCustomUsages(target)

        assertEquals(1, usages.size)
        val usage = usages.single()
        assertInstanceOf(usage, AspIncludeUsage::class.java)
        assertEquals("ASP include", (usage as AspIncludeUsage).usageType.toString())
    }

    fun testDoesNotProduceCustomUsageForPlainHtmlTarget() {
        val target = myFixture.addFileToProject("site/common.html", "<p>Common</p>")
        val usages = collectCustomUsages(target)

        assertEmpty(usages)
        assertFalse(AspFileFindUsagesHandlerFactory().canFindUsages(target))
    }

    fun testFindUsagesFactoryHandlesAspAndIncFiles() {
        val aspFile = myFixture.addFileToProject("site/default.asp", "<p>ASP</p>")
        val incFile = myFixture.addFileToProject("site/common.inc", "<% Dim value %>")
        val factory = AspFileFindUsagesHandlerFactory()

        assertTrue(factory.canFindUsages(aspFile))
        assertTrue(factory.canFindUsages(incFile))
    }

    fun testHandlerFiltersStandardCommentUsageForIncludeReference() {
        val target = myFixture.addFileToProject("site/includes/common.inc", "<% Dim sharedValue %>")
        myFixture.addFileToProject(
            "site/pages/default.asp",
            """<!-- #include file="../includes/common.inc" -->"""
        )
        val standardUsages = mutableListOf<UsageInfo>()
        val options = findUsagesOptions()
        val handler = AspFileFindUsagesHandlerFactory().createFindUsagesHandler(target, false)

        handler.processElementUsages(
            target,
            Processor { usageInfo ->
                standardUsages.add(usageInfo)
                true
            },
            options
        )

        assertFalse(standardUsages.any { it.reference is AspIncludeReference })
    }

    private fun collectCustomUsages(target: PsiFile): List<Usage> {
        val usages = mutableListOf<Usage>()
        val options = findUsagesOptions()
        AspIncludeCustomUsageSearcher().processElementUsages(
            target,
            Processor { usage ->
                usages.add(usage)
                true
            },
            options
        )
        return usages
    }

    private fun findUsagesOptions(): FindUsagesOptions = FindUsagesOptions(project).apply {
        searchScope = GlobalSearchScope.projectScope(project)
    }
}
