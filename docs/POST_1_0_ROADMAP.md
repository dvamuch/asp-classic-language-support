# ASP Classic Language Support after 1.0

Stable version `1.2.0` has been published. This document defines priorities for
the `1.x` line without mixing new features with `1.0.x` patch releases, which
are reserved for compatibility fixes and regressions.

## 1.1.0 — selection-aware formatting and mixed-language commenting

The first feature release after stable `1.0.0` adds functional range-aware
formatting for `.asp`, `.inc`, and `.vbs`. Marketplace version `1.0.0` only
protects partial-format invocations from code corruption.

Implemented behavior:

- [x] Format selected lines in `.asp`, `.inc`, and `.vbs` without normalizing the
  entire document;
- [x] Use the VBScript context before the selection to calculate correct
  indentation without modifying code outside the selected lines;
- [x] Handle a range inside one scriptlet, ranges spanning several scriptlets,
  and selections crossing HTML/VBScript boundaries;
- [x] Apply spacing, keyword case, continuation indentation, and ASP delimiter
  settings only where the selected boundaries can be changed safely;
- [x] Retain whole-operation snapshot/fingerprint/rollback protection,
  cancellation, Undo, mixed EOL support, and idempotence;
- [x] Add functional result tests in addition to the existing non-whitespace and
  token-preservation checks. Tests must explicitly prove that lines outside the
  selection remain unchanged.

The implementation expands the requested range to complete physical lines,
analyzes the whole file for control-flow depth, and merges only selected lines
back into the document. The operation guard also verifies exact prefix and
suffix preservation around a partial range. Delimiter restructuring remains
conservative: it runs only when the complete scriptlet is selected.

The release also routes the standard line-comment action by physical ASP
content: HTML uses `<!-- ... -->`, executable VBScript uses `'`, and mixed
selections apply the appropriate syntax line by line. ASP delimiters remain in
place, including when `<%` shares the first code line or `%>` shares the last
one. Inline HTML containing `<%= ... %>` is treated as HTML; commenting it does
not suppress server-side expression evaluation. Include directives and nested
HTML comments are reversibly escaped using the same convention as the native
JetBrains HTML commenter, then restored exactly when comments are toggled off.

## 1.2 — AI-agent integration and code analysis improvements

The first 1.2 feature is an optional JetBrains MCP Server integration. It makes
the plugin's existing semantic services available to connected AI agents
without duplicating the parser or building a separate command-line service.

- [x] Format a complete `.asp`, `.inc`, or `.vbs` file through MCP using the
  normal formatter and safety guard.
- [x] Return parser errors and the plugin-owned missing-include and
  `Option Explicit` diagnostics.
- [x] List local/include declarations, resolve a symbol at a source position,
  and find include-aware usages.
- [x] Resolve the transitive include graph with missing-target and cycle
  information.
- [x] Keep the MCP dependency optional and restrict tool paths to the current
  project's content roots.

The tool contract and setup notes are documented in
[`MCP_TOOLS.md`](MCP_TOOLS.md).

Later 1.x analysis work:

- An unused `Dim` and `Const` inspection after a dedicated false-positive
  audit on dynamic ASP/include code.
- Default members in abbreviated COM chains, such as
  `rs.Fields("Name").Value` without an explicit `.Item`.
- Type inference for objects assigned in an include file and used by the
  including ASP file.
- User-class inference through intermediate variables, conditional branches,
  and more complex return chains.
- Generalized nested `With` handling for documented COM/member-return chains.
- Quick Documentation for documented COM members, including signatures,
  return types, and short descriptions.
- A project-wide stub/index for `Sub`, `Function`, `Const`, `Class`, and
  `Property` declarations outside the include graph.

## Later 1.x — broader ASP Classic platform support

- Support for `global.asa`, server-side `<script runat="server">`, and the
  `Application_OnStart`, `Application_OnEnd`, `Session_OnStart`, and
  `Session_OnEnd` handlers.
- Additional COM catalogs: CDO, ADSI, `WScript.Shell`, legacy IIS components,
  and carefully documented Office Automation support.
- `<OBJECT RUNAT="Server" PROGID="...">` as a source of type information.
- User-defined type and member declarations for project-specific COM
  components, without guessing their APIs.
- Additional enum ranking and insert handlers for parameterized calls.

## Long-term backlog

- Explicit configuration of non-standard virtual web roots and content roots
  for include resolution.
- Separately designed formatting for ASP embedded in complex quoted HTML
  attributes. Until then, the formatter preserves those boundaries and does
  not restructure them.
- Additional WYSIWYG/legacy HTML fixtures and rare parser constructs.
- Regular smoke tests on more IDE products and versions beyond the validated
  matrix. Version `1.0` does not promise unverified platforms.
- New inspections only when they have linear cost and a demonstrably low
  false-positive rate on real projects.

## Deliberately out of scope

- A built-in VBScript/ASP debugger or integration with Windows Script
  Debugger/PDM.
- Static guessing of APIs for unknown project-specific COM components.
- Automatically "fixing" known-corrupt or archived files merely to reach zero
  parser diagnostics.
