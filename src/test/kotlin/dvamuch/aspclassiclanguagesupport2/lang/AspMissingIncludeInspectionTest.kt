package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.include.AspMissingIncludeInspection

class AspMissingIncludeInspectionTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(AspMissingIncludeInspection::class.java)
    }

    fun testHighlightsMissingRelativeIncludePath() {
        myFixture.configureByText(
            "missing.asp",
            """<!-- #include file="<warning descr="Included file not found: missing.inc">missing.inc</warning>" -->"""
        )

        myFixture.checkHighlighting()
    }

    fun testDoesNotHighlightResolvedRelativeOrVirtualIncludes() {
        myFixture.addFileToProject("site/includes/common.inc", "<% Dim sharedValue %>")
        myFixture.addFileToProject("shared/navigation.inc", "<% Sub RenderNavigation() %>")
        val page = myFixture.addFileToProject(
            "site/pages/default.asp",
            """
            <!-- #include file="../includes/common.inc" -->
            <!-- #include virtual="/shared/navigation.inc" -->
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(page.virtualFile)

        myFixture.checkHighlighting()
    }

    fun testIgnoresOrdinaryCommentsAndIncludeSyntaxInPlainHtml() {
        myFixture.configureByText(
            "ordinary.asp",
            "<!-- ordinary comment -->\n<!-- #include file=\"   \" -->"
        )
        myFixture.checkHighlighting()

        myFixture.configureByText("ordinary.html", "<!-- #include file=\"missing.inc\" -->")
        myFixture.checkHighlighting()
    }
}
