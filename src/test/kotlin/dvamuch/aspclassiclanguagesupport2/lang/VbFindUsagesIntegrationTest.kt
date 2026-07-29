package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeGraph
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId

class VbFindUsagesIntegrationTest : BasePlatformTestCase() {
    fun testFindsLocalVariableUsagesInSameFile() {
        val file = myFixture.addFileToProject(
            "site/page.asp",
            """
            <%
            Dim total
            total = 10
            Response.Write total
            %>
            """.trimIndent()
        )
        val declaration = idAt(file, "Dim total", "total")

        val references = findReferences(declaration)

        assertEquals(2, references.size)
        assertEquals(setOf(file.virtualFile), topLevelFiles(references))
    }

    fun testFindUsagesRespectsLocalShadowing() {
        val file = myFixture.addFileToProject(
            "site/page.asp",
            """
            <%
            Dim Value
            Response.Write Value

            Sub Render()
                Dim value
                Response.Write value
            End Sub
            %>
            """.trimIndent()
        )
        val globalDeclaration = idAt(file, "Dim Value", "Value")
        val localDeclaration = idAt(file, "Dim value", "value")

        assertEquals(1, findReferences(globalDeclaration).size)
        assertEquals(1, findReferences(localDeclaration).size)
    }

    fun testFindsUsagesThroughDirectAndNestedIncludesOnly() {
        val declarationFile = myFixture.addFileToProject(
            "site/shared/constants.inc",
            """
            <%
            Dim SharedValue
            %>
            """.trimIndent()
        )
        val directConsumer = myFixture.addFileToProject(
            "site/pages/direct.asp",
            """
            <!--#include file="../shared/constants.inc" -->
            <%
            Response.Write SharedValue
            %>
            """.trimIndent()
        )
        val bootstrap = myFixture.addFileToProject(
            "site/includes/bootstrap.inc",
            """
            <!--#include file="../shared/constants.inc" -->
            """.trimIndent()
        )
        val nestedConsumer = myFixture.addFileToProject(
            "site/pages/nested.asp",
            """
            <!--#include file="../includes/bootstrap.inc" -->
            <%
            Response.Write SharedValue
            %>
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "site/pages/unrelated.asp",
            """
            <%
            Response.Write SharedValue
            %>
            """.trimIndent()
        )
        val declaration = idAt(declarationFile, "Dim SharedValue", "SharedValue")

        assertEquals(
            setOf(directConsumer.virtualFile, bootstrap.virtualFile, nestedConsumer.virtualFile),
            AspIncludeGraph.transitiveConsumers(declarationFile).mapTo(linkedSetOf()) { it.virtualFile }
        )
        val references = findReferences(declaration)

        assertEquals(2, references.size)
        assertEquals(
            setOf(directConsumer.virtualFile, nestedConsumer.virtualFile),
            topLevelFiles(references)
        )
    }

    fun testFindUsagesHandlesIncludeCycleWithoutDuplicates() {
        val declarationFile = myFixture.addFileToProject(
            "site/includes/a.inc",
            """
            <!--#include file="b.inc" -->
            <%
            Function CycleFunction()
                CycleFunction = "ok"
            End Function
            %>
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "site/includes/b.inc",
            """
            <!--#include file="a.inc" -->
            """.trimIndent()
        )
        val consumer = myFixture.addFileToProject(
            "site/page.asp",
            """
            <!--#include file="includes/a.inc" -->
            <%
            Response.Write CycleFunction()
            %>
            """.trimIndent()
        )
        val declaration = idAt(declarationFile, "Function CycleFunction", "CycleFunction")

        val references = findReferences(declaration)

        assertEquals(2, references.size)
        assertEquals(setOf(declarationFile.virtualFile, consumer.virtualFile), topLevelFiles(references))
    }

    fun testFindsIncludedProcedureAndClassUsages() {
        val declarationFile = myFixture.addFileToProject(
            "site/includes/library.inc",
            """
            <%
            Sub RenderPage()
            End Sub

            Class PageRenderer
            End Class
            %>
            """.trimIndent()
        )
        val consumer = myFixture.addFileToProject(
            "site/page.asp",
            """
            <!--#include file="includes/library.inc" -->
            <%
            RenderPage
            Set renderer = New PageRenderer
            %>
            """.trimIndent()
        )
        val procedure = idAt(declarationFile, "Sub RenderPage", "RenderPage")
        val klass = idAt(declarationFile, "Class PageRenderer", "PageRenderer")

        val procedureReferences = findReferences(procedure)
        val classReferences = findReferences(klass)

        assertEquals(1, procedureReferences.size)
        assertEquals(setOf(consumer.virtualFile), topLevelFiles(procedureReferences))
        assertEquals(1, classReferences.size)
        assertEquals(setOf(consumer.virtualFile), topLevelFiles(classReferences))
    }

    fun testIncludedGlobalIsNotUsedWhenConsumerShadowsIt() {
        val declarationFile = myFixture.addFileToProject(
            "site/includes/globals.inc",
            """
            <%
            Dim SharedValue
            %>
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "site/page.asp",
            """
            <!--#include file="includes/globals.inc" -->
            <%
            Dim SharedValue
            Response.Write SharedValue
            %>
            """.trimIndent()
        )
        val declaration = idAt(declarationFile, "Dim SharedValue", "SharedValue")

        assertEmpty(findReferences(declaration))
    }

    fun testFindUsagesHonorsUserSelectedScope() {
        val declarationFile = myFixture.addFileToProject(
            "site/includes/globals.inc",
            """
            <%
            Dim SharedValue
            %>
            """.trimIndent()
        )
        val selectedConsumer = myFixture.addFileToProject(
            "site/selected.asp",
            """
            <!--#include file="includes/globals.inc" -->
            <% Response.Write SharedValue %>
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "site/excluded.asp",
            """
            <!--#include file="includes/globals.inc" -->
            <% Response.Write SharedValue %>
            """.trimIndent()
        )
        val declaration = idAt(declarationFile, "Dim SharedValue", "SharedValue")

        val references = ReferencesSearch.search(declaration, LocalSearchScope(selectedConsumer)).findAll()

        assertEquals(1, references.size)
        assertEquals(setOf(selectedConsumer.virtualFile), topLevelFiles(references))
    }

    fun testFindUsagesCanRunOnBackgroundThreadWithoutExistingReadAction() {
        val declarationFile = myFixture.addFileToProject(
            "site/includes/globals.inc",
            """
            <%
            Dim SharedValue
            %>
            """.trimIndent()
        )
        val consumer = myFixture.addFileToProject(
            "site/page.asp",
            """
            <!--#include file="includes/globals.inc" -->
            <% Response.Write SharedValue %>
            """.trimIndent()
        )
        val declaration = idAt(declarationFile, "Dim SharedValue", "SharedValue")

        val references = ApplicationManager.getApplication()
            .executeOnPooledThread<Collection<PsiReference>> { findReferences(declaration) }
            .get()

        assertEquals(1, references.size)
        assertEquals(setOf(consumer.virtualFile), topLevelFiles(references))
    }

    private fun findReferences(declaration: VbId): Collection<PsiReference> {
        return ReferencesSearch.search(
            declaration,
            GlobalSearchScope.projectScope(project)
        ).findAll()
    }

    private fun topLevelFiles(references: Collection<PsiReference>) = references.mapTo(linkedSetOf()) {
        InjectedLanguageManager.getInstance(project).getTopLevelFile(it.element).virtualFile
    }

    private fun idAt(file: PsiFile, marker: String, name: String): VbId {
        val markerOffset = file.text.indexOf(marker)
        assertTrue("Marker '$marker' should exist", markerOffset >= 0)
        val idOffset = markerOffset + marker.indexOf(name)
        val injected = InjectedLanguageManager.getInstance(project).findInjectedElementAt(file, idOffset)
        assertNotNull("Marker '$marker' should be inside injected VBScript", injected)
        return PsiTreeUtil.getParentOfType(injected, VbId::class.java, false)
            ?: fail("Marker '$marker' should point to VbId") as VbId
    }
}
