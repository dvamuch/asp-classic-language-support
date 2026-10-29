# ASP Classic Language Support development plan

This document records the current implementation status and the intended order
of future work. It is the working technical roadmap for contributors.

## Current status

The plugin is stable enough for day-to-day navigation and editing in real ASP
Classic projects. Stable version `1.2.0`, which adds optional MCP tools for AI
agents, is published on JetBrains Marketplace and GitHub.

Implemented capabilities:

- `.asp`, `.inc`, and `.vbs` file recognition;
- ASP segmentation into HTML template data and `<% ... %>` / `<%= ... %>`
  VBScript blocks;
- ASP and VBScript syntax highlighting;
- one native, file-wide VBScript PSI for an ASP file while HTML remains a
  separate template-data PSI;
- a VBScript parser and generated PSI model;
- scope-aware Go to Definition within a file and through include chains;
- Find Usages for local and included symbols;
- indexed include directives, navigation, and reverse include lookup;
- a missing-include inspection for relative and virtual paths, highlighted on
  the path range only;
- a conservative undeclared-identifier inspection when `Option Explicit` is
  present. Built-ins, include declarations, user classes, and dynamic object
  members do not create false warnings;
- safe HTML PSI handling of ASP fragments embedded in attribute values. The
  shared `XmlAttributeValue` manipulator changes only the safe physical range,
  so platform file/URL quick fixes—including filename-case correction and
  `http://` → `https://`—cannot delete a following ASP fragment. Changes that
  intersect `<% ... %>` are rejected;
- completion for VBScript/ASP built-ins, visible scope, transitive includes,
  and common documented COM objects;
- navigation and member completion for user-class instances created with
  `New`, including classes from the include graph, `Me`,
  `(New ClassName).Member`, direct `New` returns from functions/properties, and
  qualified or nested `With` expressions. Completion inside `With` also
  understands ASP built-ins and known COM objects;
- standard VBScript line commenting with `'` in `.vbs` and inside ASP
  scriptlets;
- semantic folding of the main VBScript blocks in `.vbs` and `.asp`, including
  control structures that cross HTML between scriptlets;
- Structure View for functions, procedures, classes, and properties in `.vbs`,
  `.asp`, and `.inc`, with a single tree across multiple scriptlets;
- matching-highlight support for paired VBScript keywords, including pairs
  split by HTML between ASP blocks;
- a `.vbs` formatter for the main block structures and unambiguous spacing. A
  lexer fallback inserts operator spacing even when neighboring ASP fragments
  contain parser errors;
- mixed ASP/HTML Reformat Code support: the platform HTML formatter handles
  template data while the plugin formats VBScript scriptlet contents;
- shared VBScript control-flow context across several ASP scriptlets;
- configurable continuation indentation via `CONTINUATION_INDENT_SIZE`, with a
  closing parenthesis returned to the originating expression level;
- alignment of an ASP expression starting on a new line inside a multiline
  HTML attribute relative to the attribute line plus HTML continuation indent;
- an optional read-only corpus formatter safety audit for every TTS `.asp`,
  `.inc`, and `.vbs` file. It compares non-whitespace text, the VBScript token
  stream, and parser-error counts. Long runs execute as independent batches,
  immediately print the active file, and persist intermediate reports;
- immutable lexical snapshots for ASP post-format replacements. Every edit
  validates the original range and VBScript token stream, and the complete
  pass rolls back transactionally if non-whitespace content changes;
- a whole-operation Reformat Code guard. Pre-format captures the original
  document; after the HTML/template and VBScript stages, the guard validates
  non-whitespace content, the ASP/VBScript token stream, and parser-error count.
  Any violation restores the entire document in the same write command and
  preserves correct Undo behavior;
- no multi-host injection for ASP. The ASP lexer switches between HTML and
  VBScript, and the parser keeps `If`, `Function`, `Class`, and other constructs
  intact even when they cross HTML;
- resolve, completion, include navigation, Find Usages, Structure View,
  folding, keyword matching, and formatting based on native host offsets,
  without synthetic analysis PSI or a bidirectional offset map;
- a full-daemon-highlighting performance regression. The current
  `customers/orderinfo.asp`, with 1,317 scriptlets and 224 `If` blocks crossing
  HTML, builds an error-free PSI in tens of milliseconds and completes full
  highlighting in roughly three seconds;
- cached symbol tables and fast candidate prefiltering for resolve and Find
  Usages;
- a current parser audit of 2,200 TTS files with no hangs or crashes. Of those,
  2,158 have no diagnostics; the remaining 42 are classified as 40 real source
  defects and two non-ASP service files;
- smoke, regression, and integration tests for the lexer, parser, resolve,
  include support, Find Usages, and formatter;
- an optional JetBrains MCP Server toolset for connected AI agents. It exposes
  safe whole-file formatting, plugin-owned inspections, visible symbols,
  include-aware Find Usages, symbol-at-position resolution, and the transitive
  include graph without creating a second parser or semantic model.

Version `0.0.8` was built against PhpStorm 2026.1.2. Stable `1.1.0` is compiled
against the minimum PhpStorm 2025.2.4 platform (PS-252.27397.112) with Java 21
bytecode; PhpStorm 2026.2.3 (PS-262.10968.76) remains the main development
target.

All 241 ordinary automated tests and plugin ZIP assembly pass on PhpStorm
2025.2.4 and 2026.2.3. On 262, validation also covers 21 `Bugs/` files, eight
`core`/DAL files, editor scenarios for `Bugs/index.asp`, `Bugs/BugAdd.asp`,
`Bugs/BugInfo.asp`, and `customers/orderinfo.asp`, plus all 2,203 supported
files in the current TTS checkout. The full formatter safety audit completed
without code/token changes or new parser errors. It was repeated after the
selection-aware formatting, idempotence, spacing, continuation, attribute, and
delimiter-placement changes on TTS HEAD `189fed0`, including its local
uncommitted edits. All nine batches reported `Violations: 0`.

## Current boundaries

1. Completion covers common documented COM objects and user-class members,
   including direct user-class returns. It does not yet infer types through
   arbitrary local intermediate variables or branches, and contextual ranking
   is not complete.
2. Full-file formatting for `.vbs`, `.asp`, and `.inc` is a stable MVP. The
   complete TTS audit, second-pass idempotence, and a real first pass on
   `customers/orderinfo.asp` are confirmed. Cancellation, stale snapshots,
   document mutation, safe partial-range invocation, mixed EOLs, and unfinished
   scriptlets are tested. The development branch now implements line-based
   selection-aware formatting for `.asp`, `.inc`, and `.vbs`: it reads
   file-wide control-flow context but only writes selected whole lines. This is
   the focused feature of stable `1.1.0`.
3. Resolve does not yet use a stub index for arbitrary project symbols and
   still needs broader complex-shadowing and class-member coverage.
4. Missing includes and undeclared identifiers under `Option Explicit` are
   inspected; unused declarations are not.
5. Stable `1.1.0` is published on GitHub Releases and JetBrains Marketplace.
   Automated release gates, isolated-sandbox startup, and manual validation
   are complete. The `1.0.x` line is reserved for regressions and compatibility
   fixes.

## Deliberately out of scope

- A built-in VBScript/ASP Classic debugger.
- Integration with Windows Script Debugger or PDM as a release requirement.
- Advertising debugger support in the public 1.x roadmap.

## Version 1.0 release contract

Version `1.0` means a stable tool for everyday ASP Classic maintenance, not the
completion of every possible feature. Later releases are scoped separately in
[`docs/POST_1_0_ROADMAP.md`](docs/POST_1_0_ROADMAP.md).

Required criteria, all complete:

- [x] Native file-wide ASP/VBScript PSI without per-scriptlet injection.
- [x] Navigation, Find Usages, the include graph, completion, and essential
  editor features work on a real project.
- [x] The formatter has whole-operation snapshot/fingerprint/rollback
  protection and passed the full TTS audit without code or token changes.
- [x] Missing includes and undeclared names under `Option Explicit` are
  diagnosed conservatively.
- [x] Full Reformat Code performance on `customers/orderinfo.asp` was reduced
  from 7.86/6.29 seconds for first/second passes on a 476 KiB file with 1,319
  ASP blocks to 3.23/1.84 seconds by removing repeated full HTML PSI traversals
  and batching whitespace writes. Targets on the current machine are at most
  five and three seconds; the optional TTS regression uses six- and four-second
  limits to allow for harness overhead.
- [x] Cancellation points and exception/cancellation cases around the platform
  HTML stage and concurrent document changes are covered. Long formatter loops
  check cancellation; a command-scoped snapshot cannot roll back a later
  command; a pre/post fingerprint detects intervening code changes and restores
  the original document.
- [x] The unresolved-name inspection was audited on real TTS files containing
  `Option Explicit`. All five eligible files and their include dependencies
  were checked. Thirty-five findings correspond to four genuinely undeclared
  names in `inc/500-102.asp`; the other files have no false positives. The
  audit is reproducible through `AspTtsInspectionAuditTest`.
- [x] The current parser audit checked 2,200 files: zero crashes, 2,158 without
  diagnostics, 40 with confirmed source errors, and two containing service
  data rather than ASP. Two genuine parser gaps were found and fixed. See
  [`docs/TTS_PARSER_REVIEW_BACKLOG.md`](docs/TTS_PARSER_REVIEW_BACKLOG.md).
- [x] Plugin metadata and the repository were cleaned of template/debug
  components; description, change notes, license, icons, and limitations are
  ready and published.
- [x] The complete suite passes on PhpStorm 2025.2.4 and 2026.2.3. Plugin
  Verifier reports compatibility, zero compatibility errors, and zero
  deprecated API usages on PhpStorm 2025.2.4/2026.2/2026.3, IntelliJ IDEA
  Ultimate 2026.2.2, WebStorm 2026.2.2, and PyCharm 2026.2.3. A focused
  103-test functional suite passes on IDEA Ultimate, WebStorm, and PyCharm.
  Deprecated `ReadAction.compute` calls were removed in `1.0.0-rc3`.
- [x] PhpStorm 2026.2.3 starts with an isolated sandbox and loads the plugin
  without plugin errors.
- [x] `1.0.0-rc1` was built and feature development was frozen during release
  candidate validation.
- [x] `1.0.0-rc3` was used daily on TTS without release-blocking regressions
  before stable `1.0.0`. Marketplace installations were observed across
  PhpStorm, IDEA, WebStorm, PyCharm, Rider, and other products. PyCharm
  2026.2.3 also received an isolated manual demo-project test.

## Version 1.1 release contract

Version `1.1.0` is intentionally limited to selection-aware formatting plus
the control-flow correction required for multiline block-opening statements.
No unrelated code-analysis feature is included in this release.

Required criteria, all complete in the published release:

- [x] Selected `.asp`, `.inc`, and `.vbs` physical lines receive normal
  spacing, keyword case, continuation, delimiter, and indentation rules while
  the exact prefix and suffix outside the expanded line range remain unchanged.
- [x] Whole-file control-flow context is used without formatting preceding or
  following lines.
- [x] Selection tests cover a single scriptlet, multiple scriptlets,
  HTML/VBScript boundaries, CRLF and mixed EOLs, Undo, idempotence, and
  multiline `If ... Then` statements joined with `_`.
- [x] A real selected SQL-construction block in `customers/orderinfo.asp`
  formats in under five seconds and is unchanged on the second pass.
- [x] All 235 release tests pass on PhpStorm 2025.2.4 and 2026.2.3.
- [x] All 2,203 supported files in the current TTS checkout pass the formatter
  safety audit on PhpStorm 2026.2.3.
- [x] Plugin Verifier reports compatibility on PhpStorm 2025.2.4 and 2026.2.3,
  IntelliJ IDEA Ultimate 2026.2.2, WebStorm 2026.2.2, and PyCharm 2026.2.3.
- [x] The 108-test functional smoke suite passes on IDEA Ultimate, WebStorm,
  and PyCharm.
- [x] The plugin ZIP, project configuration, and plugin structure checks pass.

The release is committed, tagged as `v1.1.0`, published on GitHub, and uploaded
to JetBrains Marketplace.

Unused-declaration inspection, deeper type inference and default members, a
project-wide stub index, additional COM catalogs, Quick Documentation,
`global.asa`, and restructuring ASP inside complex HTML attributes are not
included in 1.1.0 and remain planned for later releases.

## Implementation priorities

### First 1.2 feature: MCP tools for AI agents — implemented

- register six tools through the optional JetBrains MCP Server extension;
- reuse the normal formatter, inspections, PSI resolve, include graph, and
  Find Usages implementation rather than adding a parallel analyzer;
- accept project-relative paths and reject paths outside project content roots;
- keep line and column coordinates one-based and JSON results stable;
- verify both extension registration and reflection-generated MCP schemas;
- cover formatting, diagnostics, declarations, cross-include usages,
  symbol-at-position lookup, missing includes, and include cycles in integration
  tests.

The public contract and setup instructions are in
[`docs/MCP_TOOLS.md`](docs/MCP_TOOLS.md).

### 0. Formatter code safety — critical and continuously enforced

A second code-loss report was investigated against v0.0.4 on September 3,
2026. The formatter and test environment were moved to the user's PhpStorm
version. A precise case was later obtained from `Bugs/BugInfo.asp`: an ASP
expression containing quotes inside a long HTML attribute. Token loss is not
reproducible with the current formatter, either in isolation or through a real
full-file Reformat Code action, and a permanent regression test covers the
case. Full findings are in
[`docs/FORMATTER_SAFETY_INVESTIGATION.md`](docs/FORMATTER_SAFETY_INVESTIGATION.md).

Confirmed work:

- The former corpus safety test looked for `ASP_OUTER` in the lexer and for
  injected PSI after the migration to native PSI, leaving ASP token and
  parser-error checks empty. The oracle was corrected and self-tested.
- The original snapshot and rollback began only after the HTML/template stage.
  A separate operation guard now snapshots in pre-format and validates after
  all stages.
- The development platform moved from PhpStorm 2026.1.2 through 2026.2.2 to
  2026.2.3 (262.10968.76). For stable release, the build/test baseline was lowered to
  PhpStorm 2025.2.4 (252.27397.112) and JVM target 21 so one ZIP loads in both
  older Java 21 and newer Java 25 IDEs. Gradle provisions the required
  toolchain. The project uses Kotlin 2.3.20, IntelliJ Platform Gradle Plugin
  2.19.0, and GrammarKit 2023.3.0.3.
- On 261, fixes covered a `null` indent for ASP inside JavaScript, false line
  continuation on internal markers, accumulating spaces before `%>`, and
  unwanted expansion of inline ASP inside HTML attributes.
- Full-file `Bugs/BugInfo.asp` exposed non-idempotent blank-line indentation in
  nested ASP branches. Feedback from current blank-line indentation was
  removed. Two Reformat Code actions now match, and tests also compare the IDE
  action with direct `CodeStyleManager.reformat` under identical settings.
- The platform `XmlAttributeValueManipulator` could delete an ASP suffix from
  compound attributes by rebuilding the value as plain HTML and interpreting
  VBScript quotes as HTML delimiters. ASP scriptlets are now exposed as one
  outer range, and an ASP-aware manipulator preserves untouched physical
  document regions. A regression invokes the real **Change prefix to
  https://** action and verifies that the ASP expression is unchanged.
- Second-pass whitespace drift in `customers/orderinfo.asp` was fixed.
  Pre-format removes only the semantic indentation added previously, the HTML
  formatter calculates base indentation, and post-format adds current
  VBScript depth once.
- On the same file, `HTML_ALIGN_TEXT=true` was confirmed as the source of
  extreme ordinary HTML alignment: 76 regions changed and indentation reached
  285 spaces. With `false`, the maximum drops to 137 and eight visibly shifted
  lines disappear. TTS should disable `Editor | Code Style | HTML | Other |
  Align text`; this is not an ASP/VBScript formatter setting.
- Spacing around comparisons and arithmetic, `_` multiline-call indentation,
  and an ASP expression on a separate line inside a quoted attribute were
  corrected. Two editor passes match with either `HTML_ALIGN_TEXT` value.
- Short inline scriptlets in the original `customers/orderinfo.asp` no longer
  expand unconditionally. Paired delimiters preserve inline layout or move to
  aligned multiline lines together; following HTML starts on a new line.
- Operation-guard snapshots are bound to a file, thread, and IDE command. A
  cancelled operation cannot roll back a subsequent command, and a pre-format
  exception discards its snapshot immediately.
- User-installed build `0.0.10` formatted `customers/orderinfo.asp` correctly
  in one pass, closing the original convergence problem in a real IDE.
- Hardening covers the current unsaved Document as rollback baseline, partial
  ranges crossing scriptlet/HTML boundaries, mixed CRLF/LF, tab indentation,
  and unfinished scriptlets. Internal fragment markers choose a namespace not
  present in user strings or comments.

Ongoing formatter policy:

1. Keep the whole-operation guard and existing hardening cases: cancellation,
   stale snapshots, unsaved documents, pre/post mutations, partial ranges,
   mixed EOLs, tabs, unfinished scriptlets, and strings/comments near fragment
   markers.
2. Expand formatting behavior only for a reproducible user case. Add the smallest
   regression fixture first and preserve every existing safety gate.
3. For a new defect, record the smallest fixture and the first incorrect edit
   (range, stage, and call stack), then correct only the confirmed source while
   retaining built-in HTML formatting and native file-wide PSI.
4. Continue checking non-whitespace text, exact string literals, ASP
   delimiters, significant EOL/continuations, comment boundaries, and absence
   of new parser errors. Test whitespace-sensitive HTML/JavaScript/CSS
   separately. Reformatting must be idempotent, Undo must restore the original,
   and a test does not pass merely because the formatter rolled back or did
   nothing.
5. Before a release, run important files (`Bugs/index.asp`, `Bugs/BugAdd.asp`,
   `Bugs/BugInfo.asp`, `customers/orderinfo.asp`, DAL/includes), followed by
   batched TTS audits with progress and before/after artifacts on the primary
   262 platform.

### 1. Large-file performance — architectural migration complete

The plugin must remain responsive on large real-world `.asp` and `.inc` files.
The current TTS parser audit processes 2,200 files without hangs or crashes;
2,158 are diagnostic-free and all 42 remaining cases are classified. Formerly
pathological files parse without hanging. Resolve and Find Usages no longer run
an unconditional project-wide search.

Ongoing checks:

- measure opening, initial lexing/parsing/template-data processing, edits inside
  VBScript, highlighting/annotator passes, and navigation on large files;
- keep performance and smoke fixtures under `src/test/testData/asp/real-world`
  and external-project tests, with explicit time limits and cases containing
  many scriptlets;
- keep `AspLexer` transitions linear and allocation-conscious, including
  unclosed scriptlets;
- preserve linear template lexer/parser behavior by scriptlet count and test
  incremental reparse near the start and end of a file;
- do not reintroduce language injection for ASP scriptlets;
- keep debug annotators out of normal operation and prevent full-file work on
  every pass;
- reuse cached symbol tables and include graphs and avoid project searches on
  each completion invocation.

Acceptance criteria:

- large ASP/INC files open and edit without visible IDE hangs;
- performance tests catch regressions;
- debug components do not affect ordinary use.

### 2. VBScript and ASP scriptlet formatting — full-file MVP complete

Formatting is a high-value daily feature for legacy maintenance, but mixed
HTML/scriptlet boundaries require conservative handling.

Implemented for `.vbs`:

- a registered `FormattingModelBuilder` and PSI-aware `Block` tree;
- indentation for `If`, `Select Case`, `For`, `For Each`, `Do`, `While`,
  `Sub`, `Function`, `Class`, and `With` blocks and their closing clauses;
- spacing around binary operators, after argument commas, around parentheses,
  and in assignments;
- preservation of string literals, comments, and `_` line continuations;
- tests for nested blocks, every supported block kind, comments, strings,
  incomplete code, and line continuation.

Implemented for `.asp` and `.inc`:

- a template formatter layered on the platform XML/HTML model;
- standard HTML rules for tags, attributes, and text;
- outer/scriptlet elements kept separate from HTML formatter mutation;
- all `<% ... %>` contents formatted in one shared VBScript context and safely
  written back by scriptlet boundaries;
- `<%= expression %>` expression formatting with preserved delimiters;
- base indentation combining the ASP block's HTML position with semantic
  VBScript nesting;
- control-flow context across HTML and several scriptlets;
- preservation of compact control-flow scriptlets such as
  `<% If ... Then %>`, `<% Else %>`, and `<% End If %>`, with the enclosed HTML
  receiving one additional VBScript indentation level;
- paired delimiter placement: when one delimiter is separated from VBScript by
  a line break, the other moves to its own aligned line as well;
- a final lexical control-flow pass that stabilizes indentation even near
  parser errors and normalizes mixed two-/four-space legacy indentation;
- multiline scriptlets adjacent to tags moved to separate lines and assigned
  a local calculated HTML indent without recursively reformatting the file;
- integration tests for `.asp`, `.inc`, HTML attributes, multiple scriptlets,
  and second-pass idempotence;
- regressions for a large first scriptlet followed by hundreds of inline hosts
  and for the real editor action with the affected `Bugs/index.asp` region both
  inside and outside the viewport;
- shared `.vbs`/`.asp`/`.inc` keyword-case settings under
  `Editor | Code Style | VBScript`: preserve existing, lower case, or canonical
  Title Case (`If`, `ElseIf`, `End If`, `Option Explicit`). Title Case is the
  default and also applies to keyword completion without changing strings,
  comments, HTML, or member names after a dot;
- enabled-by-default settings for spaces inside ASP delimiters and coordinated
  inline/multiline placement of `<%` and `%>`. When placement matching is
  disabled, the plugin leaves mixed source placement to the platform formatter.

The 1.0 hardening suite covers incomplete/invalid scriptlets, strings and
comments near fragment markers, mixed CRLF/LF, tabs, and safe partial-format
invocations around scriptlet/HTML boundaries. The post-1.0 selection suite adds
functional result checks for a range inside one scriptlet, a range spanning
multiple scriptlets and HTML, exact preservation outside the selected lines,
mixed line separators, and Undo.

The `control-flow scriptlets around HTML` style verifies that VBScript can open
an HTML region through a separate `%>` line, the HTML receives its normal HTML
indent, continuation keywords return in a separate scriptlet, multiline
scriptlet code begins at the opening delimiter's level, and the configured
inline/multiline choice remains stable.

Target example:

```asp
<%
If items.Count > 0 Then
    %>
    <ul>
        <%
        For Each item In items
            %>
            <li><%= item.Name %></li>
            <%
        Next
        %>
    </ul>
    <%
Else
    %>
    <p>No items</p>
    <%
End If
%>
```

Additional WYSIWYG fixtures are added only for reproducible real cases. ASP
inside quoted HTML attributes must not be structurally rewritten without a
separate agreed design.

First post-1.0 formatter feature — implemented for the `1.1` line:

- [x] Implement range-aware formatting for `.asp`, `.inc`, and `.vbs` so selected
  lines receive the same spacing, case, and indentation rules as a full file
  while lines outside the selection remain unchanged;
- [x] Read control-flow context before the selection without formatting
  preceding code;
- [x] Cover a selection inside one scriptlet, across several scriptlets, and
  across HTML/VBScript boundaries with functional, safety, idempotence, and
  Undo tests.

Implementation boundary: the requested range expands to complete physical
lines. The formatter analyzes all VBScript fragments to preserve file-wide
control-flow indentation, then merges normalized content only for selected
lines. The whole-operation guard rejects any partial operation that changes the
exact prefix or suffix outside the expanded range. A multiline delimiter pair
is structurally repositioned only when its complete scriptlet is selected.
Logical VBScript statements joined with `_` are accumulated before control-flow
tracking, so a multiline `If ... Then` opens its block before subsequent body
and `End If` lines are indented.

### 3. Include support — MVP complete

Implemented:

- `<!-- #include file="relative/path.inc" -->` and
  `<!-- #include virtual="/absolute/from/site/root.inc" -->` recognition;
- references and navigation on include paths;
- resolution relative to the current file and project root;
- a reverse-dependency index and transitive include graph;
- cross-file Go to Definition, Find Usages, and completion;
- integration tests for relative, virtual, nested, and cyclic includes.

Remaining:

- additional tests for non-standard web-root/content-root configurations.

Acceptance criteria:

- include paths are understood by the IDE;
- navigation to an included file works;
- a dedicated inspection reports missing includes.

### 4. Essential editor features — complete for 1.0

Implemented:

- VBScript line commenting with `'` in `.vbs` and native ASP VBScript PSI;
- semantic folding for `Sub`, `Function`, `Property`, `Class`, `If`, `Select`,
  `For`, `For Each`, `Do`, `While`, and `With`, including structures crossing
  HTML between scriptlets. Regions are expanded by default;
- Structure View for top-level declarations and class members, parameterized
  signatures, distinct `Property Get/Let/Set`, and one tree across all ASP
  scriptlets using physical host offsets;
- nearest-pair keyword matching for `If`/`End If`, `Sub`/`End Sub`,
  `Function`/`End Function`, `Property`/`End Property`, `Class`/`End Class`,
  `Select`/`End Select`, `With`/`End With`, `For`/`Next`, `Do`/`Loop`, and
  `While`/`Wend`, including pairs crossing HTML.

Acceptance criteria:

- `.asp`, `.inc`, and `.vbs` editing is useful even without advanced resolve;
- features do not add visible overhead on large files.

### 5. Completion — basic and documented-COM stages complete

Implemented:

- VBScript keyword completion;
- ASP built-ins: `Request`, `Response`, `Session`, `Application`, and `Server`;
- common built-in members such as `Response.Write`, `Request.QueryString`,
  `Request.Form`, `Server.CreateObject`, and `Server.MapPath`;
- visible parameters, variables, constants, procedures, functions, properties,
  and classes in the current scope;
- top-level symbols from the transitive include graph;
- filtering of invisible locals and not-yet-declared implicit variables;
- suppression inside strings/comments and after the dot of an unknown object;
- case-insensitive lookup with `.vbs` and native ASP integration tests;
- best-effort type inference from `Server.CreateObject("ProgID")`,
  `CreateObject("ProgID")`, a ProgID stored in a local string variable or
  constant, `New RegExp`, and known returns such as `cmd.Execute` or
  `fso.OpenTextFile(...)`;
- a cached, scope-aware index of simple assignments that uses the most recent
  assignment before the completion location;
- sequential type inference through documented member chains such as
  `cmd.Parameters.Append`, `rs.Fields.Item(0).Value`, and
  `document.documentElement.selectSingleNode`;
- structured signatures for common ADO, Scripting Runtime, MSXML, WinHTTP, and
  RegExp methods, including names, types, and optional parameters;
- standard Parameter Info (`Cmd/Ctrl+P`) for documented COM methods, with
  correct current-argument tracking through nested calls and commas in strings;
- contextual ranking of visible `DataTypeEnum` and `ParameterDirectionEnum`
  constants for `Command.CreateParameter`, including constants from
  `adovbs.inc`;
- a deliberately limited catalog based on official documentation: ADO
  `Connection`, `Command`, `Recordset`, `Stream`, `Parameter`, `Field`, and
  common collections; Scripting Runtime `Dictionary`, `FileSystemObject`,
  `File`, `Folder`, `Drive`, and `TextStream`; MSXML `DOMDocument`, DOM
  nodes/lists, and `ServerXMLHTTP`; `WinHttp.WinHttpRequest.5.1`; and
  `VBScript.RegExp` plus `Execute` results;
- no guessed members for unknown or custom ProgIDs;
- initial user-class modeling: `Set value = New ClassName`, public member
  completion/navigation, hidden private/class-level `Dim` members, classes
  from the current file or include graph, simple `With value`,
  `(New ClassName).Member`, `Me`, direct function/property `New` returns,
  `factory.CreateWorker().Member`, qualified `With factory.Current()`, nested
  `With .Current()`, and no implicit completion declarations from bare
  assignments under `Option Explicit`.

Remaining:

- default members in abbreviated chains such as
  `rs.Fields("Name").Value` without `.Item`;
- type inference for objects assigned in an include and used by the including
  ASP file;
- user-class return inference through local intermediate variables,
  conditional branches, and include assignments;
- contextual ranking for other documented enums and insert handlers for
  parameterized constructs;
- Quick Documentation for documented COM members;
- completion for project symbols outside the include graph after a stub index
  exists.

Lower-priority catalog backlog:

- `CDO.Message` and `CDO.Configuration`;
- ADSI types returned by `GetObject("WinNT://...")` and `LDAP://...`;
- `WScript.Shell`;
- legacy `MSWC.*` IIS components;
- Office Automation such as `Excel.Application`, only with an explicit warning
  because server-side Office automation is discouraged;
- `<OBJECT RUNAT="Server" PROGID="...">` as another type source.

Project-specific ProgIDs such as `SMTPRus.SMTPRus.1` and
`LYFUpload.UploadFile` must not be built into the catalog. A future user-defined
type/member mechanism is acceptable; the plugin must not guess their APIs.

### 6. Go to Definition, Find Usages, and symbol resolve — MVP complete

Implemented:

- scope-aware resolve for local variables, parameters, top-level declarations,
  and implicit assignment declarations;
- a cached per-file symbol table;
- include-graph cross-file navigation and usages;
- fast candidate prefiltering for global Find Usages;
- tests for shadowing, local symbols, include chains, and cycles;
- navigation to public methods, properties, and fields on user-class instances,
  including classes from includes;
- class-member resolve inside simple `With value`;
- `Option Explicit` disables implicit declarations from bare assignments and
  loop variables, and a dotted member no longer falls back to a same-named
  global symbol.

Remaining:

- class-member inference beyond direct `Set FunctionName = New ClassName`;
- generalized nested `With` for COM/member-return chains;
- expansion of undeclared-name diagnostics only after real-project validation,
  preserving conservative behavior for dynamic members;
- a project-wide stub/index for top-level `Sub`, `Function`, `Const`, `Class`,
  and `Property` declarations outside the include graph.

Acceptance criteria:

- local resolve is correct within one file;
- cross-file resolve works for include scenarios;
- resolve does not degrade large-file performance.

### 7. Inspections

Implemented:

- missing `file`/`virtual` include inspection using the same resolver as
  navigation and highlighting only the path;
- ordinary HTML comments and include-like syntax in `.html` are ignored;
- tests for present and missing relative/virtual includes;
- undeclared-identifier inspection only under `Option Explicit`, checking root
  expression names while deliberately ignoring members after a dot because COM
  and project APIs are dynamic;
- local/file declarations, parameters, function return names, user classes,
  `Me`, ASP/VBScript built-ins, and direct/transitive includes are recognized;
- undeclared loop variables under `Option Explicit` are not treated as implicit
  declarations, while classic VBScript behavior remains unchanged without
  `Option Explicit`;
- one shared built-in catalog for completion and inspection.

After 1.0:

- add an unused `Dim`/`Const` inspection after a dedicated false-positive
  audit;
- extend the built-in whitelist only for reproducible false positives;
- do not enable expensive inspections without demonstrated linear cost.

Acceptance criteria:

- inspections find real errors;
- inspections do not cause editing latency on large files.

### 8. Publication cleanup — complete

- template and debug components were removed or excluded;
- `plugin.xml` contains current Marketplace metadata and dependencies;
- change notes, Apache 2.0 license, and custom light/dark icons are present;
- `since-build` matches the tested PhpStorm 2025.2.4 baseline (`252.27397`),
  and the ZIP is compatible with verified 262 and 263 platforms;
- README documents actual capabilities, limitations, release automation, and
  the post-1.0 roadmap;
- stable `1.1.0` is published on GitHub and JetBrains Marketplace.

## Deferred backlog

Dedicated `global.asa` support:

- recognize `.asa` and the special role of `global.asa`;
- parse `<script language="VBScript" runat="server">` blocks;
- support `Application_OnStart`, `Application_OnEnd`, `Session_OnStart`, and
  `Session_OnEnd`;
- account for application- and session-level object declarations;
- provide navigation, completion, and Structure View for a file structure that
  is not reducible to ordinary `<% ... %>` scriptlets.

## Recommended next steps

1. [x] Build and revalidate `1.0.0` with the accepted `1.0.0-rc3` language
   feature set.
2. [x] Upload the final ZIP to Marketplace and create the Git tag and GitHub
   Release.
3. [x] Implement range-aware formatting as the first `1.1` feature, including
   file-wide control-flow context and exact preservation outside selected lines.
4. [x] Validate and prepare the focused `1.1.0` release candidate without
   publishing it.
5. [x] After explicit release approval, commit the candidate, tag `v1.1.0`, and
   publish the GitHub Release that triggers the Marketplace workflow.
6. [x] Add optional MCP tools as the first 1.2 feature so connected AI agents
   can use the plugin's existing formatter and semantic services.
7. Fix compatibility issues and regressions in supported patch releases.
8. Continue with the remaining post-1.0 work: unused declarations, deeper type
   inference, default members, Quick Documentation, a stub index, and
   `global.asa`.
9. Repeat parser classification whenever lexer/parser behavior changes. The 40
   defective TTS source files and two service-data files do not block releases.
