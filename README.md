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
This project uses Gradle. You can run the IDE with the plugin enabled using:
```bash
./gradlew runIde
```

## License
Distributed under the Apache License 2.0. See `LICENSE` for more information.

---
*Created by [Artem Selifonov (Dvamuch)](https://github.com/Dvamuch)*
