# ASP Classic Language Support for IntelliJ IDEA

An IntelliJ Platform plugin that adds support for **ASP Classic** files with **VBScript** engine (`.asp`, `.inc`).

Modern IDEs often lack proper support for legacy technologies like ASP Classic. 
This plugin aims to make the maintenance of legacy codebases more comfortable by providing essential IDE features within PHPStorm, IntelliJ IDEA, and other JetBrains tools.

## Current Status (MVP)

Currently, the plugin provides basic support for ASP files using the VBScript engine.

### What works now:
- **File Recognition**: Supports `.asp` and `.inc` files.
- **Language Injection**: Correctly identifies `<% ... %>` and `<%= ... %>` blocks.
- **Syntax Highlighting**: 
    - Basic VBScript syntax highlighting.
    - Special highlighting for variables (identifiers) to improve readability.
- **Multi-Host Injection**: Properly handles multiple scriptlet blocks as a single VBScript context.
- **Basic HTML Support**: HTML parts of the ASP files are handled by the IDE's built-in HTML support.
- **Keyword Case Style**: `Editor | Code Style | VBScript` can preserve keyword case or enforce `lower case` / `Title Case` in formatting and completion.
- **VBScript Navigation**: "Jump to Definition" for scope-aware local symbols and top-level symbols from direct or nested includes.
- **Include Navigation**: Resolves `file` and `virtual` paths in `<!-- #include ... -->` directives.

### Visual Demonstration
![ASP Classic Support Demo](./media/demo.png)
*Support for syntax highlighting and scriptlet blocks.*

## Roadmap (Planned Features)
- [ ] **Performance**: Keep editing and language injection responsive in large legacy files.
- [x] **Include Navigation**: Resolve `file` and `virtual` paths in `<!-- #include ... -->`.
- [x] **Include-aware Symbol Navigation**: Resolve top-level symbols through direct, nested, `file`, and `virtual` includes.
- [ ] **Formatters**: Basic code formatting for VBScript blocks.
- [ ] **Editor Essentials**: Commenting, folding, structure view, and block matching.
- [ ] **Code Completion**: Basic IntelliSense for VBScript and built-in ASP objects (`Request`, `Response`, `Session`, etc.).
- [ ] **Go To Definition (Advanced)**: Add indexed project symbols, class-member resolution, and richer `With` support.

## Contributing

Contributions are very welcome! Since this is a niche tool for legacy technology, every bit of help counts.

If you want to help:
1. **Report bugs**: Open an issue if something isn't working as expected.
2. **Submit PRs**: If you know Kotlin and the IntelliJ SDK, feel free to pick up a task from the Roadmap.
3. **Suggestions**: Have an idea for a feature? Let's discuss it in the Issues.

### Development Note

The development and test target is **PhpStorm 2026.1.2 (PS-261.24374.185)**.
New builds require platform 261.24374 or later; compatibility with 2025.2 is
no longer claimed. The Gradle wrapper uses Java 21+; the test IDE runs with
its JetBrains Runtime. Kotlin is 2.3.20 and is not bundled in the plugin ZIP.

Run the test suite, build a plugin ZIP, or launch an isolated development IDE:

```bash
./gradlew test
./gradlew buildPlugin
./gradlew runIde
```

To reuse an existing installation instead of downloading PhpStorm, pass
`-PlocalIdePath=/absolute/path/to/PhpStorm.app/Contents` to any of these commands.
The installation is read as an SDK; tests use a separate sandbox, not your
working IDE profile. `AspPlatformCompatibilityTest` asserts the exact test build.

The optional TTS formatter audit only reads source files and reformats in-memory
copies. Run bounded batches with immediate per-file progress:

```bash
./gradlew test --tests '*AspTtsFormattingSafetyTest' \
  -PttsProjectDir=/absolute/path/to/TTS -PttsPathFilter=Bugs/ \
  -PttsBatchSize=100 -PttsBatchIndex=0
```

For real editor actions, selections, Undo, and important large files, run
`*AspEditorReformatSafetyTest` and `*AspFormattingModelSafetyTest` with
`-PttsProjectDir`. Their external-file cases need that property; a normal
`test` run does not constitute a TTS corpus audit.

## License
Distributed under the Apache License 2.0. See `LICENSE` for more information.

---
*Created by [Artem Selifonov (Dvamuch)](https://github.com/Dvamuch)*
