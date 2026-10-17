# Formatter safety investigation, September 3, 2026

The original version under investigation was v0.0.4, commit `6eb1838`. The
source version at the start of this report was 1.0.0-rc1. TTS sources are read
only; formatting runs against in-memory fixture copies.

## Confirmed safety-mechanism defects

### Stale corpus-test oracle

`AspTtsFormattingSafetyTest.lexAspScriptlets` searched for the
`AspTokenTypes.OUTER` token. After the native PSI migration, `AspLexer` emits
ASP_OPEN, ASP_EXPR_OPEN, ASP_CLOSE, and VBScript tokens. OUTER exists in the
HTML template-data PSI but not in this lexer stream. The ASP VB token
fingerprint was therefore empty.

`parseErrorCount` also continued to search for injected PSI, which no longer
exists. The non-whitespace comparison still worked, but the two additional
checks had become meaningless.

Both paths were corrected. A self-test now proves that the fingerprint sees
code, detects a changed space inside a string literal, and observes the
`Dim =` parse error.

### Snapshot taken too late in the safety postprocessor

`AspPostFormatProcessor.formatVbScriptFragments` captured
`originalDocumentText` only when entering post-format processing. The main
HTML/template formatter could already have changed the document by then. A
comparison and rollback against that snapshot could not detect an earlier
loss. `processText` skipped the pass entirely for a partial range.

This was a confirmed gap in the protection, **not proof** that the HTML pass
caused the user's most recent corruption.

The gap is closed in the current code by a whole-operation guard. The snapshot
is captured by `AspPreFormatProcessor` before the platform formatter runs.
After all stages, `AspPostFormatProcessor` compares non-whitespace content, the
ASP/VBScript token fingerprint, and the parser-error count. A violation of any
invariant restores the original document within the same write command.

The post-1.0 development branch also closes the functional partial-range gap.
Requested ranges expand to complete physical lines. VBScript control flow is
still analyzed across the whole file, but normalized text is merged back only
for selected lines. For a partial operation, the guard additionally verifies
that the exact document prefix and suffix outside the expanded range remain
unchanged. Delimiter restructuring is allowed only when the complete scriptlet
is selected.

For the 1.1.0 candidate, selected-range behavior is covered by functional
`.asp`, `.inc`, and `.vbs` tests for single and multiple scriptlets,
HTML/VBScript boundaries, CRLF and mixed line endings, Undo, idempotence, exact
prefix/suffix preservation, and multiline block-opening statements joined by
`_`. A real selection in `customers/orderinfo.asp` formats in about 1.3–1.6
seconds on PhpStorm 2026.2.3 and remains unchanged on the second pass.

Fingerprints also ignore EOL characters. Matching non-whitespace characters
alone does not guarantee identical VBScript or HTML/JavaScript semantics.

### Test migration to the active IDE version during v0.0.5

The project was moved to the locally installed PhpStorm 2026.1.2,
build PS-261.24374.185:

- Kotlin 2.3.20;
- IntelliJ Platform Gradle Plugin 2.18.1;
- GrammarKit 2023.3.0.3;
- `sinceBuild = 261.24374`;
- the closest published test framework, 261.24374.154, runs with the exact
  libraries from the local 261.24374.185 IDE.

Vue is disabled only in the test sandbox because its LSP plugin assumes the
production classloader layout, which is unavailable in the platform test
harness. Bundled HTML, JavaScript, and CSS remain enabled and participate in
formatter tests.

### Defects found after the migration to 261

- The template formatter could pass a `null` indent for ASP inside JavaScript;
  the builder now falls back safely to `Indent.getNoneIndent()`.
- Internal marker comments ended with `_` and were incorrectly recognized as
  VBScript line continuations. Continuation detection now uses the lexer.
- Horizontal whitespace before `%>` accumulated on every pass.
- Single-line ASP control-flow blocks inside an HTML tag or attribute were
  incorrectly expanded to multiline form. Semantic expansion is now limited
  to scriptlets that stand alone on a line.
- ASP-expression spacing ran after HTML wrapping, so the first and second
  passes could produce different line widths. Spacing moved to pre-format.

## Validated scenarios

- TTS corpus copy (`test-asp-project/TTS`, HEAD `087d025`): all 47 files under
  `Bugs/` and the first 100 `customers/` files in test sort order. No lost
  non-whitespace characters or changed VB tokens were found.
- `Bugs/index.asp`: the real editor action, the `rsViewers` section both inside
  and outside the viewport, and formatting after Undo all pass.
- `Bugs/index.asp`, `Bugs/BugAdd.asp`, and `customers/orderinfo.asp`: leaf
  formatting blocks cover all non-whitespace text, and two consecutive editor
  reformats do not remove characters. `orderinfo.asp` is substantially slower
  than the smaller files.
- Eleven synthetic mixed cases: ASP inside HTML, attributes, JavaScript, CSS,
  textareas, and comments; cross-template control flow; an unclosed scriptlet;
  invalid ASP; and a directive. No text loss or coverage gaps were found.
- Twenty-eight selections with different boundaries around ASP, HTML, and
  JavaScript, including one inside a string literal: text is preserved.
- Four edit → commit → highlighting → Undo → editor reformat cycles on a large
  synthetic file: document text and both PSI views remain consistent.
- A final targeted run used the then-current TTS checkout (HEAD `017d5bf`,
  2,275 eligible files): all 47 `Bugs/` files plus the listed editor/model
  scenarios on current copies of `index.asp`, `BugAdd.asp`, and
  `orderinfo.asp` passed. This was not a full corpus audit. Command:

  ```sh
  ./gradlew test --tests '*AspTtsFormattingSafetyTest' \
    --tests '*AspFormattingModelSafetyTest' --tests '*AspEditorReformatSafetyTest' \
    -PttsProjectDir=/Users/dvamuch/work-projects/TTS \
    -PttsPathFilter=Bugs/ -PttsBatchSize=100 --console=plain
  ```

- After migrating to PhpStorm 2026.1.2, another run used TTS HEAD `521be0b`
  with 2,244 eligible files. All 22 `Bugs/` files, `adovbs.inc`, five DAL
  files, and `core/helpers.inc` preserved non-whitespace content and the
  VBScript token stream and introduced no new parser errors. `Bugs/index.asp`
  and `Bugs/BugAdd.asp` became idempotent after the first pass.

At this stage of the investigation, checks on PhpStorm 2026.1.2 **did not
reproduce the original user-visible code loss**. The unknown root cause could
not yet be considered closed without the exact next file and user state. The
separate defects discovered on 261 were nevertheless covered by regression
tests.

### Current test environment

After the development machine was upgraded from macOS 13, the test matrix
moved to PhpStorm 2026.2.2 (PS-262.10315.130) and JVM target 25. Gradle runs on
the JBR bundled with PhpStorm and automatically provisions the declared
toolchain when it is not installed locally. `AspPlatformCompatibilityTest`
continues to enforce the exact platform build; historical results above refer
to 261. Builds use IntelliJ Platform Gradle Plugin 2.19.0. All 174 ordinary
tests and `buildPlugin` pass on 262. After adding the operation guard, 21
`Bugs/` files, eight `core`/DAL files, and editor/model scenarios for
`Bugs/index.asp`, `Bugs/BugAdd.asp`, `Bugs/BugInfo.asp`, and
`customers/orderinfo.asp` passed again.

On September 16, 2026, a full formatter safety audit covered all 2,200
supported files in the current TTS checkout (Git HEAD `cb5be77`, including
local uncommitted changes). It ran as nine independent batches on PhpStorm
2026.2.2/JVM 25. Every batch reported `Violations: 0`: non-whitespace text and
the ASP/VBScript token stream were unchanged, and no new parser errors appeared.
Reports were written to
`build/reports/tts-format-safety-batch-0000.txt` through `0008.txt`; the tests
did not modify the TTS checkout.

On September 20, 2026, the known second-pass whitespace drift in
`customers/orderinfo.asp` was fixed. The post-format VBScript indentation added
during one pass had been fed back into the HTML formatter on malformed legacy
HTML. Pre-format now removes exactly the previously added suffix, after which
post-format recalculates semantic indentation. Full copies of
`Bugs/index.asp`, `Bugs/BugAdd.asp`, and `customers/orderinfo.asp` are
idempotent after the first Reformat Code action, and the former test exclusion
was removed.

The operation guard stores snapshots per file and binds them to the active IDE
command. If formatting is cancelled before the postprocessor runs, a stale
snapshot cannot overwrite text from the next command. A snapshot is also
discarded when the plugin's own pre-format stage throws.

The complete formatter safety audit was then repeated on TTS HEAD `75312cb`,
including locally modified `Bugs/BugInfo.asp` and
`customers/orderinfo.asp`. All 2,200 files again passed nine batches with
`Violations: 0`, without writing to the TTS checkout.

On September 21, 2026, full editor-reformat runs on
`customers/orderinfo.asp` compared `HTML_ALIGN_TEXT=false` and `true`. Both
modes are idempotent and preserve code, but `true` changes 76 ordinary HTML
text regions and reproduces extreme indentation of up to 285 spaces. This is
built-in HTML formatter behavior; TTS should disable **Align text**. Independent
ASP formatter fixes added lexer-based spacing for incomplete legacy fragments,
continuation indentation for `_` lines, closing-parenthesis alignment, and
alignment of an ASP expression on a separate line inside a quoted HTML
attribute. Regressions are checked with both `HTML_ALIGN_TEXT` values and two
passes.

All nine corpus batches were repeated after those fixes on TTS HEAD `75312cb`:
2,200 of 2,200 files passed with `Violations: 0`. The
`AspTtsFormattingSafetyTest` forces **Preserve existing** keyword case because
changing keyword case is an expected style transformation while this gate is
intended to detect character loss, token-stream changes, and increased parser
errors. Separate integration tests cover the default **Title Case** mode.

On September 22, 2026, both the original `customers/orderinfo.asp` from TTS
HEAD and the current working copy were retested. The remaining first-pass
difference came from two interacting mechanisms: the platform HTML formatter
could split the contents of an originally inline scriptlet, while the semantic
post-pass calculated delimiter placement from the depth before `Else` or
`End If`. The post-pass now uses the pre-format snapshot to preserve the chosen
inline/multiline form, and host-block depth is the minimum logical depth of its
lines. If one delimiter is separated from code by a line break, both delimiters
become separate aligned lines and following HTML no longer remains on the `%>`
line. Both policies are available under `Editor | Code Style | VBScript` and
are enabled by default. On both original and already formatted copies of
`orderinfo.asp`, first and second editor-reformat results match with
`HTML_ALIGN_TEXT=false`; code and tokens are preserved. Special restructuring
of multiline ASP inside quoted HTML attributes was deliberately left out of
this iteration.

The nine corpus batches were then repeated in full on the same TTS checkout,
including `customers/orderinfo.asp` and its large archived variants. All 2,200
files reported `Violations: 0`; non-whitespace content, the ASP/VBScript token
stream, and parser-error counts were unchanged.

On October 17, 2026, the 1.1.0 release-candidate audit was repeated on PhpStorm
2026.2.3 against the current TTS working tree at HEAD `189fed0`, including its
local uncommitted edits. The corpus had grown to 2,203 supported files. All
nine batches passed without non-whitespace or ASP/VBScript token changes and
without new parser errors. The audit operated on in-memory copies and did not
modify the TTS checkout.

## Full Reformat Code performance

On the roughly 476 KiB `customers/orderinfo.asp`, the initial real editor
action baseline was 7.86 seconds for the first pass and 6.29 seconds for the
second. The IDE appeared frozen during that time.

Profiling found two architectural sources of redundant work:

- every HTML formatting block traversed the entire template PSI again to find
  ASP outer elements;
- post-format repeatedly recalculated offsets, committed PSI between stages,
  and applied a large number of individual whitespace edits.

The outer-element list is now cached for the PSI version and selected by binary
search only for the required range. Fragment extraction is one sequential pass,
whitespace edits are batched, unchanged fragments are not written, and the
redundant temporary VBScript PSI reformat was removed. Long loops include
cancellation points.

On the same machine and checkout, the result is 3.23 seconds for the first pass
and 1.84 seconds for the second. The optional TTS test uses looser limits of six
and four seconds to allow for test-harness overhead. Additional tracing is
enabled only through the system property exposed as
`-PttsFormatPerformanceTrace=true`.

## Status of the original ASP/HTML boundary-loss hypothesis

An uncovered non-whitespace range or a Document/PSI desynchronization remains
a plausible explanation for the historical platform-formatting defect, but it
is not reproducible on the supported 262 platform. Dedicated tests validate
formatting-block coverage, and the whole-operation guard now detects and rolls
back such a change regardless of its source.

JetBrains explicitly warns that every non-whitespace character must be covered
by leaf formatting blocks or gaps may be deleted:
[Code Formatter — Introduction](https://plugins.jetbrains.com/docs/intellij/code-formatting.html#introduction).

## Test changes

- `AspTtsFormattingSafetyTest`: current lexer/native PSI, oracle self-test, and
  capture of the first non-whitespace change with its stack. A detected loss
  writes `before.txt`, `after.txt`, and `first-change.txt` under
  `build/reports/tts-format-diagnostics/<relative-path>/` outside Git.
- `AspFormattingModelSafetyTest`: formatting-block coverage, synthetic mixed
  cases, editor selections, incremental edits/Undo, command/cancellation
  snapshots, performance limits, and important TTS files.
- `AspFormatterIntegrationTest`, `VbScriptFormatterIntegrationTest`, and
  `FormattingRangeSupportTest`: functional selection results inside one
  scriptlet and across ASP/HTML boundaries, file-wide control-flow context,
  exact preservation of surrounding lines, CRLF boundaries, idempotence, and
  Undo.

The fix plan and acceptance criteria are tracked as priority 0 in
`DEVELOPMENT_PLAN.md`.
