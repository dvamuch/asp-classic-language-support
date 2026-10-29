# Known limitations

- The supported IntelliJ Platform line starts with PhpStorm 2025.2.4, build
  `252.27397`. The complete test suite passes on that minimum baseline and on
  the current PhpStorm 2026.2.3 development target, which is also used for
  real-project validation. IntelliJ IDEA Ultimate and WebStorm 2026.2.2, plus
  PyCharm 2026.2.3, pass Plugin Verifier and the functional smoke suite;
  PyCharm has also been tested manually. Other IntelliJ Platform products have
  not been validated by the project even if Marketplace permits installation
  based on their module compatibility.
- The HTML portion of an ASP file is formatted by the built-in HTML formatter.
  On deeply nested legacy pages, disable `Editor | Code Style | HTML | Other |
  Align text`; otherwise, the platform may produce extremely long indentation.
- ASP code inside complex, multiline quoted HTML attributes is preserved
  safely, but the plugin does not yet restructure those attributes.
- Selection-aware formatting in stable 1.1.0 operates on
  complete physical lines. A selection that starts or ends in the middle of a
  line formats that entire line. ASP delimiter placement is changed only when
  the complete scriptlet is inside the selected line range.
- Analysis of dynamic COM objects is intentionally conservative. Unknown
  member names are not reported as errors to avoid widespread false positives.
- `global.asa` and server-side `<script runat="server">` blocks do not yet have
  a dedicated ASP lifecycle model.
- Enabling or disabling the plugin requires an IDE restart because keyword-pair
  highlighting uses a non-dynamic IntelliJ Platform extension point.
- The optional MCP integration exposes only diagnostics owned by this plugin
  plus parser errors. It does not aggregate every inspection installed in the
  IDE, and MCP formatting currently targets complete files rather than editor
  selections.
