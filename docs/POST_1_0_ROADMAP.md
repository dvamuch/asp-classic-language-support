# ASP Classic Language Support after 1.0

Stable version `1.0.0` has been published. This document defines priorities for
the `1.x` line without mixing new features with `1.0.x` patch releases, which
are reserved for compatibility fixes and regressions.

## 1.1.0 — selection-aware formatting prepared

The first feature after stable `1.0.0` is implemented and validated in the
local 1.1.0 release candidate. Marketplace version `1.0.0` only protects
partial-format invocations from code corruption; 1.1.0 adds functional
range-aware formatting for `.asp`, `.inc`, and `.vbs`.

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

## 1.2 — code analysis improvements

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
