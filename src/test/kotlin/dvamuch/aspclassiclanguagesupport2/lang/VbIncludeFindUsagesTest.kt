package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.find.findUsages.FindUsagesManager
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbFindUsagesHandlerFactory
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId

class VbIncludeFindUsagesTest : BasePlatformTestCase() {
    fun testFindsDirectAndNestedIncludeUsagesWithoutUnrelatedSameNames() {
        val declarationFile = myFixture.addFileToProject(
            "site/shared/constants.inc",
            "<% Dim SharedValue %>"
        )
        val directConsumer = myFixture.addFileToProject(
            "site/pages/direct.asp",
            "<!--#include file=\"../shared/constants.inc\" --><% Response.Write SharedValue %>"
        )
        myFixture.addFileToProject(
            "site/includes/bootstrap.inc",
            "<!--#include file=\"../shared/constants.inc\" -->"
        )
        val nestedConsumer = myFixture.addFileToProject(
            "site/pages/nested.asp",
            "<!--#include file=\"../includes/bootstrap.inc\" --><% Response.Write SHAREDVALUE %>"
        )
        repeat(50) { index ->
            myFixture.addFileToProject(
                "site/pages/unrelated-$index.asp",
                "<% Dim SharedValue : Response.Write SharedValue %>"
            )
        }
        val declaration = idAt(declarationFile, "Dim SharedValue", "SharedValue")

        val usages = findUsages(declaration)

        assertEquals(2, usages.size)
        assertEquals(
            setOf(directConsumer.virtualFile, nestedConsumer.virtualFile),
            usages.mapTo(linkedSetOf()) { it.virtualFile }
        )

        myFixture.configureFromExistingVirtualFile(declarationFile.virtualFile)
        myFixture.editor.caretModel.moveToOffset(declarationFile.text.indexOf("SharedValue"))
        myFixture.performEditorAction(IdeActions.ACTION_FIND_USAGES)
        FindUsagesManager.waitForAsyncTaskCompletion(project)
    }

    fun testFindsUsagesAcrossManyAspConsumers() {
        val declarationFile = myFixture.addFileToProject(
            "site/core/adovbs.inc",
            "<% Const adInteger = 3 %>"
        )
        val consumers = List(80) { index ->
            myFixture.addFileToProject(
                "site/pages/page-$index.asp",
                "<!--#include file=\"../core/adovbs.inc\" --><% value = adInteger %>"
            )
        }
        val declaration = idAt(declarationFile, "Const adInteger", "adInteger")

        val usages = findUsages(declaration)

        assertEquals(80, usages.size)
        assertEquals(
            consumers.mapTo(linkedSetOf()) { it.virtualFile },
            usages.mapTo(linkedSetOf()) { it.virtualFile }
        )
    }

    fun testFindsUsageThroughVirtualInclude() {
        val declarationFile = myFixture.addFileToProject(
            "shared/constants.inc",
            "<% Const SharedValue = 1 %>"
        )
        val consumer = myFixture.addFileToProject(
            "pages/page.asp",
            "<!--#include virtual=\"/shared/constants.inc\" --><% Response.Write SharedValue %>"
        )
        val declaration = idAt(declarationFile, "Const SharedValue", "SharedValue")

        val usages = findUsages(declaration)

        assertEquals(1, usages.size)
        assertEquals(consumer.virtualFile, usages.single().virtualFile)
    }

    fun testSameNamedFunctionFromAnotherIncludeIsNotReported() {
        val declarationFile = myFixture.addFileToProject(
            "site/inc/sendmailwithlog.asp",
            """
            <%
            Function SMTPSendMail()
                With cdoConfig.Fields
                    .Item("smtpserver") = Application("SMTP")
                    .Update
                End With
                With message
                    .BodyPart.CharSet = "utf-8"
                    .Send()
                End With
                SMTPSendMail = "OK"
            End Function
            %>
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "site/inc/sendmail.asp",
            "<% Function SMTPSendMail() : End Function %>"
        )
        val expectedConsumer = myFixture.addFileToProject(
            "site/pages/with-log.asp",
            "<!--#include file=\"../inc/sendmailwithlog.asp\" --><% result = SMTPSendMail() %>"
        )
        myFixture.addFileToProject(
            "site/pages/without-log.asp",
            "<!--#include file=\"../inc/sendmail.asp\" --><% result = SMTPSendMail() %>"
        )
        val declaration = idAt(declarationFile, "Function SMTPSendMail", "SMTPSendMail")

        val usages = findUsages(declaration)

        assertEquals(2, usages.size)
        assertEquals(
            setOf(declarationFile.virtualFile, expectedConsumer.virtualFile),
            usages.mapTo(linkedSetOf()) { it.virtualFile }
        )
    }

    fun testSameNamedIncludeTargetFromAnotherDirectoryIsNotReported() {
        val declarationFile = myFixture.addFileToProject(
            "site/library-a/shared.inc",
            "<% Const SharedValue = 1 %>"
        )
        myFixture.addFileToProject(
            "site/library-b/shared.inc",
            "<% Const SharedValue = 2 %>"
        )
        val expectedConsumer = myFixture.addFileToProject(
            "site/pages/a.asp",
            "<!--#include file=\"../library-a/shared.inc\" --><% value = SharedValue %>"
        )
        myFixture.addFileToProject(
            "site/pages/b.asp",
            "<!--#include file=\"../library-b/shared.inc\" --><% value = SharedValue %>"
        )
        val declaration = idAt(declarationFile, "Const SharedValue", "SharedValue")

        val usages = findUsages(declaration)

        assertEquals(1, usages.size)
        assertEquals(expectedConsumer.virtualFile, usages.single().virtualFile)
    }

    fun testFirstIncludedDeclarationWinsWhenConsumerIncludesTwoImplementations() {
        val declarationFile = myFixture.addFileToProject(
            "site/inc/sendmailwithlog.asp",
            "<% Function SMTPSendMail() : End Function %>"
        )
        myFixture.addFileToProject(
            "site/inc/sendmailwithlog1.asp",
            "<% Function SMTPSendMail() : End Function %>"
        )
        val expectedConsumer = myFixture.addFileToProject(
            "site/pages/target-first.asp",
            """
            <!--#include file="../inc/sendmailwithlog.asp" -->
            <!--#include file="../inc/sendmailwithlog1.asp" -->
            <% result = SMTPSendMail() %>
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "site/pages/copy-first.asp",
            """
            <!--#include file="../inc/sendmailwithlog1.asp" -->
            <!--#include file="../inc/sendmailwithlog.asp" -->
            <% result = SMTPSendMail() %>
            """.trimIndent()
        )
        val declaration = idAt(declarationFile, "Function SMTPSendMail", "SMTPSendMail")

        val usages = findUsages(declaration)

        assertEquals(1, usages.size)
        assertEquals(expectedConsumer.virtualFile, usages.single().virtualFile)
    }

    fun testFindsUsageAfterUnrelatedConsumerParseError() {
        val declarationFile = myFixture.addFileToProject(
            "site/inc/constants.inc",
            "<% Const SharedValue = 1 %>"
        )
        val consumer = myFixture.addFileToProject(
            "site/page.asp",
            """
            <!--#include file="inc/constants.inc" -->
            <%
            If True Then
            End If
            End If
            Response.Write SharedValue
            %>
            """.trimIndent()
        )
        val declaration = idAt(declarationFile, "Const SharedValue", "SharedValue")

        val usages = findUsages(declaration)

        assertEquals(1, usages.size)
        assertEquals(consumer.virtualFile, usages.single().virtualFile)
    }

    fun testLexicalSearchIgnoresStringsCommentsAndMemberNames() {
        val declarationFile = myFixture.addFileToProject(
            "site/inc/library.inc",
            "<% Function SharedFunction() : End Function %>"
        )
        val consumer = myFixture.addFileToProject(
            "site/page.asp",
            """
            <!--#include file="inc/library.inc" -->
            <%
            text = "SharedFunction"
            ' SharedFunction
            value = object.SharedFunction
            result = SharedFunction()
            %>
            """.trimIndent()
        )
        val declaration = idAt(declarationFile, "Function SharedFunction", "SharedFunction")

        val usages = findUsages(declaration)

        assertEquals(1, usages.size)
        assertEquals(consumer.virtualFile, usages.single().virtualFile)
    }

    fun testIncludeCycleHasNoDuplicates() {
        val declarationFile = myFixture.addFileToProject(
            "site/includes/a.inc",
            """
            <!--#include file="b.inc" -->
            <% Dim CycleValue %>
            """.trimIndent()
        )
        val cycleConsumer = myFixture.addFileToProject(
            "site/includes/b.inc",
            """
            <!--#include file="a.inc" -->
            <% Response.Write CycleValue %>
            """.trimIndent()
        )
        val pageConsumer = myFixture.addFileToProject(
            "site/page.asp",
            "<!--#include file=\"includes/b.inc\" --><% Response.Write CycleValue %>"
        )
        val declaration = idAt(declarationFile, "Dim CycleValue", "CycleValue")

        val usages = findUsages(declaration)

        assertEquals(2, usages.size)
        assertEquals(
            setOf(cycleConsumer.virtualFile, pageConsumer.virtualFile),
            usages.mapTo(linkedSetOf()) { it.virtualFile }
        )
    }

    fun testConsumerDeclarationShadowsIncludedGlobal() {
        val declarationFile = myFixture.addFileToProject(
            "site/includes/globals.inc",
            "<% Dim SharedValue %>"
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

        assertEmpty(findUsages(declaration))
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

        assertEquals(setOf(consumer.virtualFile), findUsages(procedure).mapTo(linkedSetOf()) { it.virtualFile })
        assertEquals(setOf(consumer.virtualFile), findUsages(klass).mapTo(linkedSetOf()) { it.virtualFile })
    }

    private fun findUsages(declaration: VbId): List<UsageInfo> {
        val handler = VbFindUsagesHandlerFactory().createFindUsagesHandler(declaration, false)
        val options = handler.findUsagesOptions.apply {
            searchScope = GlobalSearchScope.projectScope(project)
        }
        val usages = mutableListOf<UsageInfo>()
        assertTrue(handler.processElementUsages(declaration, Processor {
            usages.add(it)
            true
        }, options))
        return usages
    }

    private fun idAt(file: PsiFile, marker: String, name: String): VbId {
        val markerOffset = file.text.indexOf(marker)
        assertTrue("Marker '$marker' should exist", markerOffset >= 0)
        val idOffset = markerOffset + marker.indexOf(name)
        val aspPsi = file.viewProvider.getPsi(AspLanguage) ?: file
        val element = aspPsi.findElementAt(idOffset)
        assertNotNull("Marker '$marker' should be inside native ASP PSI", element)
        return PsiTreeUtil.getParentOfType(element, VbId::class.java, false)
            ?: fail("Marker '$marker' should point to VbId") as VbId
    }
}
