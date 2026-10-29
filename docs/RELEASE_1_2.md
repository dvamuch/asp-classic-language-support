# 1.2.0 release notes

## Release status

Version 1.2.0 is the current stable release. It adds an optional JetBrains MCP
Server integration. Connected AI agents can use the plugin's existing
formatter, inspections, symbol model,
include resolver, and Find Usages implementation instead of approximating ASP
Classic semantics from source text alone.

The normal editor experience is unchanged when the bundled MCP Server plugin is
not installed or is disabled.

## User-visible changes

- Adds `asp_format_file` for guarded whole-file formatting of `.asp`, `.inc`,
  and `.vbs` files, with optional formatted content in the response.
- Adds `asp_inspect_file` for parser errors, missing includes, and conservative
  undeclared-identifier diagnostics under `Option Explicit`.
- Adds `asp_get_symbols` and `asp_get_symbol_at` for include-aware declaration
  listing and symbol resolution.
- Adds `asp_find_usages` for semantic usages, including ASP files that consume
  a declaration through an include.
- Adds `asp_resolve_includes` for transitive include graphs, unresolved targets,
  and cycles.
- Restricts all requested paths to the current project's content roots. Source
  positions and returned locations use one-based lines and columns.
- Keeps the MCP integration optional, so the language plugin continues to load
  without the JetBrains MCP Server plugin.

To expose these tools, enable **MCP Server** under
`Settings | Tools | MCP Server` and connect the AI client to that server.
Installing the language plugin alone does not enable the MCP server.

The complete tool contract is documented in
[`MCP_TOOLS.md`](MCP_TOOLS.md).

## Artifact

- ZIP: `build/distributions/asp-classic-language-support-1.2.0.zip`.
- Size: 678504 bytes.
- SHA-256: `58ab613cd8a1c608278ecdba13c97122b60ee950eba0bf34b4f1991e198ec165`.
- Minimum IDE build: `252.27397` (PhpStorm 2025.2.4).
- Bytecode target: Java 21.

## Validation

- All 241 ordinary automated tests cover the existing language features and the
  six MCP operations.
- MCP integration tests verify optional extension registration,
  reflection-generated input/output schemas, guarded formatting, diagnostics,
  declaration listing, cross-include usages, symbol resolution, missing
  includes, transitive include graphs, and cycles.
- The release checks retain the PhpStorm 2025.2.4 compatibility baseline and
  PhpStorm 2026.2.3 development target.
- Plugin Verifier covers PhpStorm 2025.2.4 and 2026.2.3, IntelliJ IDEA Ultimate,
  WebStorm, and PyCharm from the repository's release matrix.
- Plugin project configuration, archive structure, and ZIP assembly are checked
  before publication.
- The snapshot was manually installed in PhpStorm 2026.2.3. After enabling the
  bundled MCP Server, Junie discovered the ASP Classic MCP tools.

## Release boundary

MCP formatting operates on complete files; selection-aware formatting remains
an interactive editor action. Inspection output intentionally contains this
plugin's diagnostics plus parser errors rather than aggregating every IDE
inspection. The semantic results retain the existing conservative behavior for
dynamic COM members and do not add a project-wide symbol index.

Unused-declaration diagnostics, deeper user-class and COM type inference,
Quick Documentation, a project-wide symbol index, dedicated `global.asa`
support, and structural formatting of complex ASP expressions inside quoted
HTML attributes remain planned for later releases.

## Publication process

Publishing the `v1.2.0` GitHub Release triggers the Marketplace workflow. The
workflow repeats the release checks and uploads version 1.2.0 to the default
Marketplace channel for JetBrains approval.
