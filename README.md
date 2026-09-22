# ASP Classic Language Support for IntelliJ IDEA

An IntelliJ Platform plugin that adds support for **ASP Classic** files with **VBScript** engine (`.asp`, `.inc`).

Modern IDEs often lack proper support for legacy technologies like ASP Classic. 
This plugin aims to make the maintenance of legacy codebases more comfortable by providing essential IDE features within PHPStorm, IntelliJ IDEA, and other JetBrains tools.

## Current Status

The plugin is a working MVP for day-to-day navigation and editing of ASP Classic
projects. The current source version is `1.0.0-rc3` and targets PhpStorm 2026.2.2.

### What works now:
- **File Recognition**: Supports `.asp`, `.inc` and `.vbs` files.
- **ASP Parsing**: Correctly identifies `<% ... %>` and `<%= ... %>` blocks.
- **Syntax Highlighting**: 
    - Basic VBScript syntax highlighting.
    - Special highlighting for variables (identifiers) to improve readability.
- **Native ASP PSI**: Parses all scriptlets in an ASP file as one file-wide
  VBScript context without synthetic multi-host injection.
- **Basic HTML Support**: HTML parts of the ASP files are handled by the IDE's built-in HTML support.
- **Keyword Case Style**: `Editor | Code Style | VBScript` uses `Title Case` by
  default and can instead preserve existing keyword case or enforce `lower case`.
- **ASP Delimiter Style**: the same settings page controls spaces inside
  `<% ... %>` / `<%= ... %>` and whether paired delimiters keep consistent
  inline or multiline placement; both options are enabled by default.
- **VBScript Navigation**: "Jump to Definition" for scope-aware local symbols and top-level symbols from direct or nested includes.
- **Include Navigation**: Resolves `file` and `virtual` paths in `<!-- #include ... -->` directives.
- **Missing Include Inspection**: Highlights an unresolved relative or virtual
  include path without affecting ordinary HTML comments.
- **Undeclared Identifier Inspection**: With `Option Explicit`, highlights
  unresolved root names while treating dynamic object/COM members conservatively
  and resolving symbols from ASP includes.
- **Find Usages**: Finds local and include-aware symbol usages and reverse include dependencies.
- **Completion and Parameter Info**: Completes visible symbols, ASP built-ins and
  documented members of common ADO, Scripting Runtime, MSXML, WinHTTP and
  RegExp objects.
- **User Classes and `With`**: Resolves and completes public members of objects
  created with `New`, including classes from ASP includes, direct user-class
  return chains, `(New ClassName).Member`, `Me`, and qualified/nested `With`;
  `With` completion also supports ASP built-ins and known COM types.
- **Editor Features**: Line commenting, semantic folding, Structure View and
  matching of paired VBScript block keywords.
- **Formatting**: Formats `.vbs`, `.asp` and `.inc`, including mixed HTML and
  VBScript control flow spanning multiple scriptlets, spaces VBScript operators
  even in incomplete legacy fragments, preserves inline delimiter pairs, and
  normalizes line continuations.
- **Safe Compound Attributes**: HTML file/URL quick-fixes preserve embedded ASP
  expressions inside attribute values.
- **Reformat Safety Guard**: Captures the document before platform formatting
  and restores it if the complete operation changes code tokens or introduces
  new ASP parser errors.

### Visual Demonstration
![ASP Classic Support Demo](./media/demo.png)
*Support for syntax highlighting and scriptlet blocks.*

## Roadmap

- [x] **Native ASP Architecture**: Keep one file-wide VBScript PSI while preserving the HTML template-data PSI.
- [x] **Performance Baseline**: Parse the complete TTS corpus without hangs and avoid unconditional project-wide resolve searches.
- [x] **Include Navigation**: Resolve `file` and `virtual` paths in `<!-- #include ... -->`.
- [x] **Include-aware Symbol Navigation**: Resolve top-level symbols through direct, nested, `file`, and `virtual` includes.
- [x] **Formatter MVP**: Format VBScript and mixed ASP/HTML while preserving scriptlet content.
- [x] **Editor Essentials**: Commenting, folding, Structure View and block matching.
- [x] **Code Completion MVP**: Complete VBScript/ASP symbols and documented members of common COM objects.
- [x] **Formatter Hardening**: Preserve code across full/partial formatting,
  cancellation, stale commands and document changes; keep large-file
  formatting idempotent and regression-tested.
- [ ] **Advanced Symbol Model**: Extend user-class inference beyond direct `New` returns and add default members plus indexed project symbols.
- [ ] **Inspections**: Missing includes and conservative `Option Explicit`
  unresolved identifiers are covered; audit real projects, then add
  unused-declaration diagnostics without affecting editor responsiveness.
- [ ] **Global.asa**: Add its server-side script blocks and application/session lifecycle model.
- [x] **Publication Metadata**: Marketplace name, description, change notes,
  license and plugin icons are ready for the 1.0 release candidate.

## Contributing

Contributions are very welcome! Since this is a niche tool for legacy technology, every bit of help counts.

If you want to help:
1. **Report bugs**: Open an issue if something isn't working as expected.
2. **Submit PRs**: If you know Kotlin and the IntelliJ SDK, feel free to pick up a task from the Roadmap.
3. **Suggestions**: Have an idea for a feature? Let's discuss it in the Issues.

### Development Note

The development and test target is **PhpStorm 2026.2.2 (PS-262.10315.130)**.
New builds require platform 262.10315 or later. The previous 2026.1.2 test
baseline was retired after the development machine moved from macOS 13 to the
current macOS and PhpStorm installation. Gradle itself requires a Java 17+
launcher. Compilation and tests use Java 25, matching the target platform;
Gradle downloads that toolchain automatically when it is not installed locally.
Kotlin is 2.3.20 and is not bundled in the plugin ZIP.

Run the test suite, build a plugin ZIP, or launch an isolated development IDE:

```bash
./gradlew test
./gradlew buildPlugin
./gradlew runIde
```

On macOS, when no system JDK is installed, the wrapper automatically uses the
JBR bundled with PhpStorm from `/Applications` (or `$HOME/Applications`) to
start Gradle. The build still selects the declared Java 25 toolchain for
compilation and tests. For a non-standard IDE location, point `JAVA_HOME` at
any Java 17+ runtime explicitly:

```bash
JAVA_HOME=/Applications/PhpStorm.app/Contents/jbr/Contents/Home ./gradlew test
```

To reuse the exact supported installation instead of downloading PhpStorm, pass
`-PlocalIdePath=/absolute/path/to/PhpStorm.app/Contents` to any of these commands.
The installation is read as an SDK; tests use a separate sandbox, not your
working IDE profile. `AspPlatformCompatibilityTest` asserts the exact test build.
At present, a local test SDK must therefore be build `PS-262.10315.130`.

The normal `test` task runs the repository fixtures and synthetic regressions.
Tests that depend on the external TTS checkout are skipped unless
`-PttsProjectDir` is supplied. The optional formatter audit only reads source
files and reformats in-memory copies. Run bounded batches with immediate
per-file progress:

```bash
./gradlew test --tests '*AspTtsFormattingSafetyTest' \
  -PttsProjectDir=/absolute/path/to/TTS -PttsPathFilter=Bugs/ \
  -PttsBatchSize=100 -PttsBatchIndex=0
```

The current PhpStorm 2026.2.2 baseline has completed a full formatter safety
audit of all 2200 supported files in the TTS working tree (Git HEAD `75312cb`,
including its local uncommitted edits). The audit was repeated after the latest
operator, continuation-indent, delimiter-placement, multiline-attribute,
default keyword-case and large-file performance changes: no non-whitespace or
ASP/VBScript token changes and no new parser errors were detected. The audit
does not write to the TTS checkout; it fixes keyword case to `Preserve existing`
so intentional style changes do not mask the destructive-change checks.

The optional parser inventory is also read-only:

```bash
./gradlew test --tests '*AspTtsParserAuditTest' \
  -PttsProjectDir=/absolute/path/to/TTS
```

On the same checkout it reports 2158 clean files, 40 files with confirmed
source-code errors and 2 service files whose contents are not ASP. The detailed
classification is in
[`docs/TTS_PARSER_REVIEW_BACKLOG.md`](docs/TTS_PARSER_REVIEW_BACKLOG.md).

For real editor actions, selections, Undo, and important large files, run
`*AspEditorReformatSafetyTest` and `*AspFormattingModelSafetyTest` with
`-PttsProjectDir`. Their external-file cases need that property; a normal
`test` run does not constitute a TTS corpus audit.

### HTML formatter settings for legacy ASP

The HTML part of an ASP file is formatted by PhpStorm. For deeply nested legacy
pages such as TTS `customers/orderinfo.asp`, disable **Align text** under
`Editor | Code Style | HTML | Other`. With this option enabled, PhpStorm aligns
wrapped text to the preceding inline content and can produce hundreds of spaces;
this is independent of VBScript indentation. ASP expressions that start a new
line inside a quoted HTML attribute are aligned by the plugin to the attribute
line plus the configured HTML continuation indent.

### ASP delimiter settings

`Editor | Code Style | VBScript` contains two independent ASP options:

- **Spaces inside ASP delimiters** produces `<% code %>` and `<%= expression %>`.
- **Match opening and closing delimiter placement** keeps an inline pair inline;
  if either delimiter is separated from the VBScript by a line break, both are
  placed on their own aligned lines. A multiline delimiter line is also kept
  separate from following HTML.

Both are enabled by default. Formatting inside quoted HTML attributes remains
delegated mostly to PhpStorm's HTML formatter and is intentionally not
restructured by the delimiter-placement rule.

The detailed implementation status and prioritised backlog live in
[`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md). Formatter safety findings are in
[`docs/FORMATTER_SAFETY_INVESTIGATION.md`](docs/FORMATTER_SAFETY_INVESTIGATION.md).
Release boundaries are listed in
[`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md), while work intentionally
deferred past 1.0 lives in
[`docs/POST_1_0_ROADMAP.md`](docs/POST_1_0_ROADMAP.md).
The current artifact and manual acceptance checklist are recorded in
[`docs/RELEASE_1_0_RC1.md`](docs/RELEASE_1_0_RC1.md).

## License
Distributed under the Apache License 2.0. See `LICENSE` for more information.

---
*Created by [Dvamuch](https://github.com/Dvamuch)*
