package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType

class VbScriptFormatterIntegrationTest : BasePlatformTestCase() {
    fun testFormatsNestedBlocksAndBinaryOperators() {
        assertReformatted(
            """
            Function LoadValue(id)
            If id>0 Then
            For i=1 To id
            value=value+i
            Next
            Else
            value=0
            End If
            LoadValue=value
            End Function
            """.trimIndent(),
            """
            Function LoadValue(id)
                If id > 0 Then
                    For i = 1 To id
                        value = value + i
                    Next
                Else
                    value = 0
                End If
                LoadValue = value
            End Function
            """.trimIndent()
        )
    }

    fun testFormatsClassSelectAndCaseBodies() {
        assertReformatted(
            """
            Class Worker
            Sub Run(items)
            Select Case items.Count
            Case 0
            result="empty"
            Case Else
            result=items.Count
            End Select
            End Sub
            End Class
            """.trimIndent(),
            """
            Class Worker
                Sub Run(items)
                    Select Case items.Count
                        Case 0
                            result = "empty"
                        Case Else
                            result = items.Count
                    End Select
                End Sub
            End Class
            """.trimIndent()
        )
    }

    fun testNormalizesCommasParenthesesAndDotsWithoutChangingLiteralsOrComments() {
        assertReformatted(
            """
            Sub SaveValues()
            Dim values( 10 ),name
            ' keep  multiple   spaces in this comment
            name="a  b"
            Call Save( name,values( 1 ) )
            End Sub
            """.trimIndent(),
            """
            Sub SaveValues()
                Dim values(10), name
                ' keep  multiple   spaces in this comment
                name = "a  b"
                Call Save(name, values(1))
            End Sub
            """.trimIndent()
        )
    }

    fun testFormatsRemainingSupportedBlockKinds() {
        assertReformatted(
            """
            Class Worker
            Property Get Name()
            If ready Then
            Name="ready"
            ElseIf pending Then
            Name="pending"
            Else
            Name="idle"
            End If
            End Property
            Sub Run(items)
            For Each item In items
            With item
            .State="running"
            End With
            Next
            Do While pending
            pending=LoadNext()
            Loop
            While active
            active=CheckState()
            Wend
            End Sub
            End Class
            """.trimIndent(),
            """
            Class Worker
                Property Get Name()
                    If ready Then
                        Name = "ready"
                    ElseIf pending Then
                        Name = "pending"
                    Else
                        Name = "idle"
                    End If
                End Property
                Sub Run(items)
                    For Each item In items
                        With item
                            .State = "running"
                        End With
                    Next
                    Do While pending
                        pending = LoadNext()
                    Loop
                    While active
                        active = CheckState()
                    Wend
                End Sub
            End Class
            """.trimIndent()
        )
    }

    fun testPreservesLineContinuation() {
        val file = myFixture.configureByText(
            VbScriptFileType,
            """
            Function BuildValue()
            value=first & _
              second
            BuildValue=value
            End Function
            """.trimIndent()
        )

        reformat(file)

        assertTrue("Line continuation marker must survive formatting", file.text.contains("& _\n"))
        assertTrue(
            "The continued expression must remain on the next line: ${file.text}",
            Regex("& _\\r?\\n[ \\t]+second").containsMatchIn(file.text)
        )
        assertFalse("Formatter must not collapse the continuation", file.text.contains("& second"))
    }

    fun testDoesNotFailOnIncompleteBlock() {
        val file = myFixture.configureByText(
            VbScriptFileType,
            "If condition Then\nvalue=\"unchanged  literal\""
        )

        reformat(file)

        assertTrue(file.text.contains("\"unchanged  literal\""))
    }

    fun testNormalizesFirstStatementAndExistingTwoSpaceIndents() {
        assertReformatted(
            """
            If condition Then
              first=value+1
              second=value+2
            End If
            """.trimIndent(),
            """
            If condition Then
                first = value + 1
                second = value + 2
            End If
            """.trimIndent()
        )
    }

    fun testNormalizesMixedLegacyIndentAfterBlankLineAndComment() {
        assertReformatted(
            """
            If (CStr(Request("MM_insert")) = "form1") Then

              MM_editConnection=MM_connection_STRING
                MM_editTable="dbo.Bugs"

                ' create arrays
              For MM_i=LBound(MM_fields) To UBound(MM_fields) Step 2
                value=value+1
              Next
            End If
            """.trimIndent(),
            """
            If (CStr(Request("MM_insert")) = "form1") Then

                MM_editConnection = MM_connection_STRING
                MM_editTable = "dbo.Bugs"

                ' create arrays
                For MM_i = LBound(MM_fields) To UBound(MM_fields) Step 2
                    value = value + 1
                Next
            End If
            """.trimIndent()
        )
    }

    private fun assertReformatted(before: String, after: String) {
        val file = myFixture.configureByText(VbScriptFileType, before)
        reformat(file)
        assertEquals(after, file.text)
    }

    private fun reformat(file: com.intellij.psi.PsiFile) {
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(file)
        }
    }
}
