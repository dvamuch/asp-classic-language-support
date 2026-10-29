package dvamuch.aspclassiclanguagesupport2.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.McpToolsProvider
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking

class AspClassicMcpOperationsIntegrationTest : BasePlatformTestCase() {
    fun testRegistersSixToolsThroughOptionalMcpDescriptor() {
        assertTrue(McpToolset.EP.extensionList.any { it is AspClassicMcpToolset })
        val expectedToolNames = setOf(
            "asp_format_file",
            "asp_inspect_file",
            "asp_get_symbols",
            "asp_find_usages",
            "asp_resolve_includes",
            "asp_get_symbol_at"
        )
        val annotatedToolNames = AspClassicMcpToolset::class.java.declaredMethods
            .mapNotNull { method -> method.getAnnotation(McpTool::class.java)?.name }
            .toSet()
        assertEquals(expectedToolNames, annotatedToolNames)

        val reflectedTools: List<com.intellij.mcpserver.McpTool> = McpToolsProvider.EP.extensionList
            .flatMap { provider -> provider.getTools() }
        val descriptors = reflectedTools.associate { tool -> tool.descriptor.name to tool.descriptor }
        assertTrue(descriptors.keys.toString(), descriptors.keys.containsAll(expectedToolNames))
        val outputProperties = mapOf(
            "asp_format_file" to "changed",
            "asp_inspect_file" to "problems",
            "asp_get_symbols" to "symbols",
            "asp_find_usages" to "usages",
            "asp_resolve_includes" to "includes",
            "asp_get_symbol_at" to "symbol"
        )
        outputProperties.forEach { (toolName, outputProperty) ->
            val descriptor = requireNotNull(descriptors[toolName])
            val outputSchema = requireNotNull(descriptor.outputSchema)
            assertTrue(toolName, descriptor.inputSchema.propertiesSchema.containsKey("path"))
            assertTrue(toolName, outputSchema.propertiesSchema.containsKey(outputProperty))
        }
    }

    fun testFormatsFileAndReturnsContentOnlyWhenRequested() = runBlocking {
        myFixture.addFileToProject(
            "format-me.vbs",
            """
            if value=1 then
            response.write value
            end if
            """.trimIndent()
        )

        val first = AspClassicMcpOperations.formatFile(project, "format-me.vbs", returnContent = false)
        assertTrue(first.changed)
        assertNull(first.content)

        val second = AspClassicMcpOperations.formatFile(project, "format-me.vbs", returnContent = true)
        assertFalse(second.changed)
        assertEquals(
            """
            If value = 1 Then
                response.write value
            End If
            """.trimIndent(),
            second.content
        )
        assertEquals(second.content, VfsUtilCore.loadText(myFixture.findFileInTempDir("format-me.vbs")))
    }

    fun testReportsPluginOwnedInspectionProblems() = runBlocking {
        myFixture.addFileToProject(
            "inspect.asp",
            """
            <!-- #include file="missing.inc" -->
            <%
            Option Explicit
            MissingValue = 1
            %>
            """.trimIndent()
        )

        val problems = AspClassicMcpOperations.inspectFile(project, "inspect.asp")

        assertTrue(problems.toString(), problems.any {
            it.inspectionId == "AspMissingInclude" && it.message.contains("missing.inc")
        })
        assertTrue(problems.toString(), problems.any {
            it.inspectionId == "VbUnresolvedIdentifier" && it.message.contains("MissingValue")
        })
    }

    fun testListsLocalAndIncludedSymbols() = runBlocking {
        myFixture.addFileToProject(
            "shared.inc",
            """
            <%
            Function SharedHelper(value)
                SharedHelper = value
            End Function
            %>
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "symbols.asp",
            """
            <!-- #include file="shared.inc" -->
            <%
            Sub LocalAction()
            End Sub
            %>
            """.trimIndent()
        )

        val symbols = AspClassicMcpOperations.getSymbols(project, "symbols.asp")

        assertTrue(symbols.toString(), symbols.any {
            it.name == "LocalAction" && it.kind == "sub" && !it.fromInclude
        })
        assertTrue(symbols.toString(), symbols.any {
            it.name == "SharedHelper" && it.kind == "function" && it.fromInclude && it.path == "shared.inc"
        })
    }

    fun testFindsUsagesAcrossIncludeConsumersAndResolvesSymbolAtCall() = runBlocking {
        val declarationText = """
            <%
            Function SharedHelper(value)
                SharedHelper = value
            End Function
            %>
        """.trimIndent()
        val consumerText = """
            <!-- #include file="shared.inc" -->
            <%
            result = SharedHelper(1)
            %>
        """.trimIndent()
        myFixture.addFileToProject("shared.inc", declarationText)
        myFixture.addFileToProject("consumer.asp", consumerText)

        val declarationPosition = positionOf(declarationText, "SharedHelper")
        val usages = AspClassicMcpOperations.findUsages(
            project,
            "shared.inc",
            declarationPosition.first,
            declarationPosition.second
        )
        assertTrue(usages.toString(), usages.any { it.path == "shared.inc" && it.declaration })
        assertTrue(usages.toString(), usages.any {
            it.path == "consumer.asp" && !it.declaration && it.snippet?.contains("SharedHelper(1)") == true
        })

        val usagePosition = positionOf(consumerText, "SharedHelper")
        val symbol = AspClassicMcpOperations.getSymbolAt(
            project,
            "consumer.asp",
            usagePosition.first,
            usagePosition.second
        )
        assertNotNull(symbol)
        assertEquals("SharedHelper", symbol?.name)
        assertEquals("function", symbol?.kind)
        assertEquals("shared.inc", symbol?.declarationPath)
    }

    fun testResolvesTransitiveIncludesAndReportsCyclesAndMissingTargets() = runBlocking {
        myFixture.addFileToProject(
            "main.asp",
            "<!-- #include file=\"nested/child.inc\" -->"
        )
        myFixture.addFileToProject(
            "nested/child.inc",
            """
            <!-- #include file="../main.asp" -->
            <!-- #include file="missing.inc" -->
            """.trimIndent()
        )

        val includes = AspClassicMcpOperations.resolveIncludes(project, "main.asp")

        assertTrue(includes.toString(), includes.any {
            it.sourcePath == "main.asp" && it.resolvedPath == "nested/child.inc" && it.depth == 1 && it.resolved
        })
        assertTrue(includes.toString(), includes.any {
            it.sourcePath == "nested/child.inc" && it.resolvedPath == "main.asp" && it.depth == 2 && it.cyclic
        })
        assertTrue(includes.toString(), includes.any {
            it.declaredPath == "missing.inc" && !it.resolved && it.depth == 2
        })
    }

    private fun positionOf(text: String, needle: String): Pair<Int, Int> {
        val offset = text.indexOf(needle)
        require(offset >= 0)
        val prefix = text.substring(0, offset)
        val line = prefix.count { it == '\n' } + 1
        val lineStart = prefix.lastIndexOf('\n').let { if (it < 0) 0 else it + 1 }
        return line to (offset - lineStart + 1)
    }
}
