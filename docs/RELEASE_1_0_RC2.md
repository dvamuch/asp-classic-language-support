# 1.0.0-rc2 release candidate

## Changes since rc1

- Fixed the indentation of standalone `%>` and `<%` boundaries around HTML
  nested in VBScript control-flow blocks. The regression was reproduced from
  `Bugs/BugInfo.asp` and now has both focused and full-file coverage.
- Replaced the plugin artwork with the established ASP file icon: a
  `#0066B8` tile with a white `ASP` label. Light and dark IDE themes use the
  same recognizable mark.

## Artifact

- ZIP: `build/distributions/asp-classic-language-support-1.0.0-rc2.zip`
- Size: 535652 bytes.
- SHA-256: `d32c53e36f6f680e11fdf369eddb99882463075ccf507082e61f6aea48d68d0c`
- Minimum IDE build: `262.10315`.

## Validation

- The complete ordinary Gradle test suite passes on PhpStorm 2026.2.2.
- All formatter integration tests pass, including one-pass and idempotence
  checks for control-flow scriptlets around HTML.
- The real TTS `Bugs/BugInfo.asp` passes the destructive-change safety gate.
- The real `BugInfo.asp` editor regression verifies the corrected delimiter
  indentation and an unchanged second formatting pass.

The feature set remains frozen. After manual acceptance of this RC, the same
feature set can be promoted to `1.0.0`.
