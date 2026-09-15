package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType

class VbScriptRegressionParsingTest : BasePlatformTestCase() {
    fun testParsesEmptyProcedureParameterLists() {
        assertParses(
            """
            Sub Ping()
                Response.Write "pong"
            End Sub

            Function CurrentUser()
                CurrentUser = "guest"
            End Function

            Function EmptyCallback()

            End Function

            Class EmptyClass

            End Class
            """.trimIndent()
        )
    }

    fun testParsesBareCallWithCommaSeparatedArguments() {
        assertParses(
            """
            Response.ContentType = "text/html"
            Response.AddHeader "Content-Type", "text/html;charset=utf-8"
            Response.AddHeader "Cache-Control", "no-cache, no-store, must-revalidate"
            recordset.Open, connection
            """.trimIndent()
        )
    }

    fun testParsesVisibilityDeclarationsAndMembers() {
        assertParses(
            """
            Class JsonEncoder
                Private output, innerCall
                Public Count

                Private Sub Class_Initialize()
                    Count = 0
                End Sub

                Public Default Function Encode(value)
                    Encode = value
                End Function
            End Class
            """.trimIndent()
        )
    }

    fun testParsesMultidimensionalArrayDeclarations() {
        assertParses(
            """
            Dim DepartmentRow(11, 1)
            Dim DynamicRows()
            """.trimIndent()
        )
    }

    fun testParsesKeywordLikeMemberAfterDot() {
        assertParses(
            """
            set cmd = Server.CreateObject("ADODB.Command")
            set rs = cmd.Execute()
            """.trimIndent()
        )
    }

    fun testParsesCallResultPropertyChain() {
        assertParses(
            """
            serviceItemId = serviceNode.selectSingleNode("service_item_id").text
            customerName = serviceNode.selectSingleNode("customer_name").text
            encoded = CStr((New VbsJson).Encode(value))
            """.trimIndent()
        )
    }

    fun testParsesBareCallArgumentStartingWithParenthesizedExpression() {
        assertParses(
            """
            Response.Write (record.Fields.Item("Total").Value) / exchangeRate
            Response.Write (user.Fields.Item("LastName").Value) & " " & firstName
            """.trimIndent()
        )
    }

    fun testParsesIndexedAssignmentTarget() {
        assertParses(
            """
            Dim MM_fields
            Dim MM_i

            MM_fields(MM_i+1) = CStr(Request.Form(MM_fields(MM_i)))
            MM_typeArray = Split(MM_columns(MM_i+1), ",")
            """.trimIndent()
        )
    }

    fun testParsesLowercaseHexDigitsWithoutLosingFollowingConstants() {
        assertParses(
            """
            Const adInteger = 3
            Const adModeShareExclusive = &Hc
            Const adErrPropNotAllSettable = &He9f
            Const adParamInput = &H0001
            """.trimIndent()
        )
    }

    fun testParsesFileContainingOnlyComments() {
        assertParses(
            """
            ' first comment
            ' second comment
            """.trimIndent()
        )
    }

    fun testParsesSingleLineIfWithEndIf() {
        assertParses(
            """
            If IsEmpty(qsComplexity) Then Response.Write("selected") End If
            If qsComplexity = -1 Then Response.Write("selected") End If
            If status = 0 Then Response.Write("SELECTED"):End If
            """.trimIndent()
        )
    }

    fun testParsesLeadingDotMembersInsideWithBlock() {
        assertParses(
            """
            With cdoConfig.Fields
                .Item("smtpserver") = Application("SMTP")
                .Update
            End With

            With myCDONTSMail
                .BodyPart.CharSet = "utf-8"
                .Send()
            End With
            """.trimIndent()
        )
    }

    fun testParsesIfBlockCollapsedByAspHtmlGap() {
        assertParses(
            """
            If rowCount = -1 Then End If
            Response.Write Now()
            """.trimIndent()
        )
    }

    fun testDistinguishesNestedBlockAndSingleLineIfStatements() {
        assertParses(
            """
            If outerCondition Then
                If inlineCondition Then inlineValue = 1
                If nestedCondition Then
                    nestedValue = 2
                ElseIf alternateCondition Then alternateValue = 3
                Else
                    nestedValue = 4
                End If
            Else
                outerValue = 5
            End If
            """.trimIndent()
        )
    }

    fun testParsesBlockIfWithTrailingCommentAfterThen() {
        assertParses(
            """
            If condition Then ' branch explanation
                value = 1
            End If
            """.trimIndent()
        )
    }

    fun testParsesEmptyNestedBlockIfSeparatedByBlankLines() {
        assertParses(
            """
            If outerCondition Then
                If innerCondition Then

                End If
            End If
            """.trimIndent()
        )
    }

    fun testParsesSelectCaseAfterBlankLinesAndComments() {
        assertParses(
            """
            Select Case regionId

                ' Case 0 is intentionally disabled
                ' disabledValue = 0
                Case 3
                    selectedValue = 3
                Case Else
                    selectedValue = -1
            End Select

            Select Case fallback
                Case Else
                    selectedValue = 1
            End Select
            """.trimIndent()
        )
    }

    private fun assertParses(vbScript: String) {
        val file = myFixture.configureByText(VbScriptFileType, vbScript)
        val errors = PsiTreeUtil.collectElementsOfType(file, PsiErrorElement::class.java)
        val sample = errors.take(5).joinToString("\n") { "${it.errorDescription} :: ${it.text}" }
        assertTrue("Unexpected parser errors:\n$sample", errors.isEmpty())
    }
}
