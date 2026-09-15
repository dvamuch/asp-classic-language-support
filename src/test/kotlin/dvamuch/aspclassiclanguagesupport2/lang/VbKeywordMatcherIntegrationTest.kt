package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.codeInsight.highlighting.HeavyBraceHighlighter
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType

class VbKeywordMatcherIntegrationTest : BasePlatformTestCase() {
    fun testMatchesNestedIfAndNearestEndIf() {
        val file = myFixture.configureByText(
            VbScriptFileType,
            """
            If outerCondition Then
                If innerCondition Then
                    value = 1
                End If
            End If
            """.trimIndent()
        )

        assertPairAt(file, file.text.indexOf("If outerCondition"), "If", "End If")
        assertPairAt(file, file.text.indexOf("If innerCondition"), "If", "End If")

        val innerEndOffset = file.text.indexOf("End If")
        val innerPair = HeavyBraceHighlighter.match(file, innerEndOffset)
        assertNotNull("Inner End If should have a pair", innerPair)
        assertEquals(file.text.indexOf("If innerCondition"), innerPair!!.first.startOffset)
    }

    fun testMatchesDeclarationAndControlFlowBlocks() {
        val file = myFixture.configureByText(
            VbScriptFileType,
            """
            Class Worker
                Property Get Name()
                    Name = "worker"
                End Property

                Sub Run(items)
                    Select Case items.Count
                        Case 0
                            Exit Sub
                    End Select
                    For Each item In items
                        item.Process
                    Next
                    Do While pending
                        pending = LoadNext()
                    Loop
                    While active
                        active = CheckState()
                    Wend
                    With current
                        .Save
                    End With
                End Sub

                Function Calculate(value)
                    Calculate = value
                End Function
            End Class
            """.trimIndent()
        )

        assertPairAt(file, file.text.indexOf("Class Worker"), "Class", "End Class")
        assertPairAt(file, file.text.indexOf("Property Get"), "Property", "End Property")
        assertPairAt(file, file.text.indexOf("Sub Run"), "Sub", "End Sub")
        assertPairAt(file, file.text.indexOf("Select Case"), "Select", "End Select")
        assertPairAt(file, file.text.indexOf("For Each"), "For", "Next")
        assertPairAt(file, file.text.indexOf("Do While"), "Do", "Loop")
        assertPairAt(file, file.text.indexOf("While active"), "While", "Wend")
        assertPairAt(file, file.text.indexOf("With current"), "With", "End With")
        assertPairAt(file, file.text.indexOf("Function Calculate"), "Function", "End Function")
    }

    fun testDoesNotTreatSingleLineIfAsBlockPair() {
        val file = myFixture.configureByText(
            VbScriptFileType,
            "If condition Then value = 1 Else value = 2"
        )

        assertNull(HeavyBraceHighlighter.match(file, file.text.indexOf("If")))
    }

    fun testMatchesIfAcrossAspScriptletsAndHtml() {
        val file = myFixture.configureByText(
            AspFileType,
            """
            <%
            If showDetails Then
            %>
            <section>
                <p>Details</p>
            </section>
            <%
            End If
            %>
            """.trimIndent()
        )

        assertPairAt(file, file.text.indexOf("If showDetails"), "If", "End If")
        assertPairAt(file, file.text.indexOf("End If"), "If", "End If")
    }

    fun testMatchesKeywordsAtNativeAspOffsets() {
        val file = myFixture.configureByText(
            AspFileType,
            """
            <%
            Dim firstValue
            Dim secondValue
            Dim MM_columnsStr
            %>
            <main>HTML makes host and injected offsets differ</main>
            <%
            If showDetails Then
                RenderDetails
            End If
            %>
            """.trimIndent()
        )
        val dimHostOffset = file.text.indexOf("Dim MM_columnsStr")
        val ifHostOffset = file.text.indexOf("If showDetails")

        assertNull(
            "A host offset on Dim must not accidentally match a later If",
            HeavyBraceHighlighter.match(file, dimHostOffset)
        )

        val pair = HeavyBraceHighlighter.match(file, ifHostOffset)
        assertNotNull("Host offset on If should resolve inside native ASP PSI", pair)
        assertEquals("If", pair!!.first.substring(file.text))
        assertEquals("End If", pair.second.substring(file.text))
    }

    private fun assertPairAt(file: PsiFile, offset: Int, expectedOpening: String, expectedClosing: String) {
        assertTrue("Test marker should exist", offset >= 0)
        val pair = HeavyBraceHighlighter.match(file, offset)
        assertNotNull("Expected a keyword pair at offset $offset", pair)
        assertEquals(expectedOpening, pair!!.first.substring(file.text))
        assertEquals(expectedClosing, pair.second.substring(file.text))
    }
}
