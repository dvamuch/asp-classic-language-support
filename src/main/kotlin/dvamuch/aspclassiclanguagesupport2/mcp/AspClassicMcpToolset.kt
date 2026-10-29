package dvamuch.aspclassiclanguagesupport2.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.project
import kotlinx.coroutines.currentCoroutineContext

/** ASP Classic language operations exposed through JetBrains' bundled MCP server. */
class AspClassicMcpToolset : McpToolset {
    @McpTool(name = "asp_format_file")
    @McpDescription("Format an ASP, INC, or VBS file with the ASP Classic formatter and its safety guard.")
    suspend fun formatFile(
        @McpDescription("Project-relative path to an ASP, INC, or VBS file.") path: String,
        @McpDescription("Include the complete formatted file text in the response.") returnContent: Boolean = false
    ): AspFormatResult =
        AspClassicMcpOperations.formatFile(currentCoroutineContext().project, path, returnContent)

    @McpTool(name = "asp_inspect_file")
    @McpDescription("Return ASP Classic parser errors, unresolved includes, and Option Explicit diagnostics for a file.")
    suspend fun inspectFile(
        @McpDescription("Project-relative path to an ASP, INC, or VBS file.") path: String
    ): AspInspectionResult = AspInspectionResult(
        AspClassicMcpOperations.inspectFile(currentCoroutineContext().project, path)
    )

    @McpTool(name = "asp_get_symbols")
    @McpDescription("List VBScript declarations visible in an ASP, INC, or VBS file, including declarations from ASP includes.")
    suspend fun getSymbols(
        @McpDescription("Project-relative path to an ASP, INC, or VBS file.") path: String
    ): AspSymbolsResult = AspSymbolsResult(
        AspClassicMcpOperations.getSymbols(currentCoroutineContext().project, path)
    )

    @McpTool(name = "asp_find_usages")
    @McpDescription("Find semantic usages of the VBScript symbol at a 1-based line and column, including ASP include consumers.")
    suspend fun findUsages(
        @McpDescription("Project-relative path to an ASP, INC, or VBS file.") path: String,
        @McpDescription("One-based source line containing the symbol.") line: Int,
        @McpDescription("One-based source column containing the symbol.") column: Int
    ): AspUsagesResult = AspUsagesResult(
        AspClassicMcpOperations.findUsages(currentCoroutineContext().project, path, line, column)
    )

    @McpTool(name = "asp_resolve_includes")
    @McpDescription("Resolve the transitive ASP include graph and report unresolved paths and cycles.")
    suspend fun resolveIncludes(
        @McpDescription("Project-relative path to an ASP or INC file.") path: String
    ): AspIncludesResult = AspIncludesResult(
        AspClassicMcpOperations.resolveIncludes(currentCoroutineContext().project, path)
    )

    @McpTool(name = "asp_get_symbol_at")
    @McpDescription("Resolve the VBScript symbol at a 1-based line and column to its declaration.")
    suspend fun getSymbolAt(
        @McpDescription("Project-relative path to an ASP, INC, or VBS file.") path: String,
        @McpDescription("One-based source line containing the symbol.") line: Int,
        @McpDescription("One-based source column containing the symbol.") column: Int
    ): AspSymbolAtResult = AspSymbolAtResult(
        AspClassicMcpOperations.getSymbolAt(currentCoroutineContext().project, path, line, column)
    )
}
