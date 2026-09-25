# 1.0.0-rc3 release candidate

## Changes since rc2

- Fixed Find Usages for user-class members declared in included `.asp` and
  `.inc` files. The regression is covered with two classes exposing the same
  member name, so unrelated member calls are not reported.
- Replaced all remaining deprecated `ReadAction.compute(ThrowableComputable)`
  calls with the supported blocking read-action API.

## Artifact

- ZIP: `build/distributions/asp-classic-language-support-1.0.0-rc3.zip`
- Size: 535702 bytes.
- SHA-256: `79edf1f51bcc8f63b5856078907d66e6f0991c70ea83490f61b200de24137499`.
- Minimum IDE build: `262.10315`.

## Validation

- The complete ordinary Gradle test suite passes on PhpStorm 2026.2.2.
- A focused 103-test functional smoke suite passes on IntelliJ IDEA Ultimate
  2026.2.2 and WebStorm 2026.2.2. It covers formatting, completion, resolve,
  inspections and include-aware Find Usages.
- Plugin Verifier reports compatibility with PhpStorm 2026.2 and 2026.3,
  IntelliJ IDEA Ultimate 2026.2.2 and WebStorm 2026.2.2.
- Plugin Verifier reports no deprecated API usages.
- Plugin structure and project configuration checks pass.

The feature set remains frozen. After manual acceptance of this RC, the same
feature set can be promoted to `1.0.0`.
