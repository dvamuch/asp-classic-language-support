# MCP tools for AI agents

Version 1.2 exposes ASP Classic language services through the MCP server bundled
with recent JetBrains IDEs. An MCP-capable agent
connected to the IDE can use the same formatter, PSI, include resolver,
inspections, and Find Usages implementation that interactive IDE actions use.

The integration is optional. The language plugin continues to load normally if
the JetBrains MCP Server plugin is unavailable or disabled.

## Available tools

| MCP tool | Operation |
| --- | --- |
| `asp_format_file` | Format an `.asp`, `.inc`, or `.vbs` file. The response always contains `changed`; `returnContent=true` also returns the complete resulting text. |
| `asp_inspect_file` | Return ASP/VBScript parser errors, missing includes, and conservative undeclared-identifier findings under `Option Explicit` in `problems`. |
| `asp_get_symbols` | List VBScript declarations in a file and declarations imported through its ASP include graph in `symbols`. |
| `asp_find_usages` | Find semantic usages of the symbol at a source position, including usages in ASP files that include its declaration file, in `usages`. |
| `asp_resolve_includes` | Return the transitive include graph, unresolved targets, include kind, depth, and cycles in `includes`. |
| `asp_get_symbol_at` | Resolve the symbol at a source position and return it in `symbol`; the field is JSON `null` when no VBScript symbol is present. |

Tool parameters use project-relative paths. Absolute paths are accepted only
when they remain under one of the current project's content roots. Source lines
and columns in both requests and responses are one-based.

Every tool returns a JSON object because the JetBrains reflection-based MCP API
requires object-shaped root results. List operations therefore use the named
array fields shown in the table instead of returning a bare JSON array.

`asp_format_file` changes and saves the IDE document and therefore participates
in the normal IntelliJ write-command and Undo model. It runs the plugin's
complete formatting pipeline, including the token/parser-error safety guard.
The other five tools are read-only.

## Enabling the connection

Enable the bundled **MCP Server** under `Settings | Tools | MCP Server`, then
connect the agent using the configuration shown by the IDE. Installing this
language plugin or the MCP Server plugin does not enable the server
automatically. Exact connection controls can differ between JetBrains versions
and AI clients. Once connected, the six tools appear with the `asp_` prefix so
they remain distinguishable from general IDE tools.

The agent does not receive these capabilities merely because the language
plugin is installed: it must be connected to the IDE's MCP server and permitted
to call the tools by its own client configuration.

## Deliberate boundaries

- Inspection results are limited to diagnostics owned by this plugin plus
  parser errors; this is not a replacement for every inspection installed in
  the IDE.
- Symbol and usage results follow the plugin's current semantic model. Dynamic
  COM members remain conservative, and arbitrary project-wide declarations
  outside the include graph are not indexed yet.
- Formatting currently operates on a complete file through MCP. Selection-aware
  formatting remains available through the interactive editor action.
- The integration uses the JetBrains MCP extension API available at the
  project's minimum supported platform, PhpStorm 2025.2.4 / build 252.27397.

## Development checks

`AspClassicMcpOperationsIntegrationTest` verifies registration and reflection
of all six MCP schemas as well as formatting, inspections, symbol listing,
cross-include usages, symbol resolution, transitive includes, missing targets,
include cycles, and persistence of formatted content. The complete 241-test
suite passes on both PhpStorm 2025.2.4 and 2026.2.3. Plugin Verifier reports the
snapshot compatible with the two PhpStorm targets plus IntelliJ IDEA Ultimate,
WebStorm, and PyCharm in the repository's release matrix. The optional
descriptor is `META-INF/asp-classic-mcp.xml`.
