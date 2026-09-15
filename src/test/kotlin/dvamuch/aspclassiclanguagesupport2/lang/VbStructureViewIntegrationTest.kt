package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbStructureViewFactory

class VbStructureViewIntegrationTest : BasePlatformTestCase() {
    fun testBuildsTopLevelAndNestedClassStructureForVbScript() {
        val file = myFixture.configureByText(
            VbScriptFileType,
            """
            Function LoadValue(id)
                Dim localValue
                LoadValue = id
            End Function

            Sub Save()
            End Sub

            Class Worker
                Private state

                Public Sub Run(task)
                    Dim localTask
                End Sub

                Private Function Calculate(value)
                    Calculate = value
                End Function

                Public Property Get Name()
                    Name = "worker"
                End Property

                Public Property Let Name(value)
                End Property
            End Class
            """.trimIndent()
        )

        val model = structureModel(VbStructureViewFactory().getStructureViewBuilder(file))
        try {
            val topLevel = structureChildren(model.root)
            assertEquals(listOf("LoadValue(id)", "Save()", "Worker"), presentations(topLevel))

            val classMembers = structureChildren(topLevel.single { it.presentation.presentableText == "Worker" })
            assertEquals(
                listOf("Run(task)", "Calculate(value)", "Get Name()", "Let Name(value)"),
                presentations(classMembers)
            )
            assertFalse("Local variables must not appear in Structure View", presentations(classMembers).contains("localTask"))
            assertTrue(classMembers.all(StructureViewTreeElement::canNavigateToSource))
        } finally {
            model.dispose()
        }
    }

    fun testBuildsSingleStructureAcrossAspScriptlets() {
        val file = myFixture.configureByText(
            AspFileType,
            """
            <%
            Function First(value)
                First = value
            End Function
            %>
            <main>content</main>
            <%
            Class Mailer
                Public Sub Send(message)
                End Sub
            End Class
            %>
            <%
            Sub Last()
            End Sub
            %>
            """.trimIndent()
        )

        val model = structureModel(AspStructureViewFactory().getStructureViewBuilder(file))
        try {
            val topLevel = structureChildren(model.root)
            assertEquals(listOf("First(value)", "Mailer", "Last()"), presentations(topLevel))
            assertEquals(
                listOf("Send(message)"),
                presentations(structureChildren(topLevel.single { it.presentation.presentableText == "Mailer" }))
            )

            val first = topLevel.first().value as PsiElement
            val topLevelFile = InjectedLanguageManager.getInstance(project).getTopLevelFile(first)
            assertEquals(file.virtualFile, topLevelFile.virtualFile)
            assertTrue(topLevel.first().canNavigateToSource())
        } finally {
            model.dispose()
        }
    }

    private fun structureModel(builder: com.intellij.ide.structureView.StructureViewBuilder): StructureViewModel {
        return (builder as TreeBasedStructureViewBuilder).createStructureViewModel(myFixture.editor)
    }

    private fun presentations(elements: List<StructureViewTreeElement>): List<String> {
        return elements.mapNotNull { element -> element.presentation.presentableText }
    }

    private fun structureChildren(element: StructureViewTreeElement): List<StructureViewTreeElement> {
        return element.children.map { child -> child as StructureViewTreeElement }
    }
}
