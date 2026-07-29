package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.find.findUsages.FindUsagesManager
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.psi.PsiFile
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbFindUsagesHandlerFactory
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId

class VbSingleFileFindUsagesTest : BasePlatformTestCase() {
    fun testFindUsagesIsAvailableAndFindsReferencesInCurrentAspFile() {
        val file = myFixture.addFileToProject(
            "site/Bugs/BugAdd.asp",
            """
            <%
            Dim MM_editCmd
            Set MM_editCmd = Server.CreateObject("ADODB.Command")
            MM_editCmd.CommandText = MM_editQuery
            Set rs = MM_editCmd.Execute
            %>
            """.trimIndent()
        )
        val declaration = idAt(file, "Dim MM_editCmd", "MM_editCmd")
        val factory = VbFindUsagesHandlerFactory()

        assertTrue(factory.canFindUsages(declaration))
        assertEquals(3, ReferencesSearch.search(declaration, LocalSearchScope(declaration.containingFile)).findAll().size)

        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        myFixture.editor.caretModel.moveToOffset(file.text.indexOf("Dim MM_editCmd") + 4)
        myFixture.performEditorAction(IdeActions.ACTION_FIND_USAGES)
        FindUsagesManager.waitForAsyncTaskCompletion(project)
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
