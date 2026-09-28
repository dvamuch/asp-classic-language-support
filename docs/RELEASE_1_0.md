# 1.0.0 stable release

## Release status

Version 1.0.0 promotes the language feature set accepted in `1.0.0-rc3` after
day-to-day use on the TTS project and initial Marketplace use across several
JetBrains products. No formatter, parser, resolve or completion feature was
expanded after that release candidate.

The stable release adds the public feedback paths needed after publication:

- a one-time, non-modal invitation after at least seven days and eight ASP,
  INC or VBS file openings;
- a permanent **Help | ASP Classic: Send Feedback** action;
- GitHub bug-report and feature-request forms;
- direct links to Marketplace reviews and GitHub Issues in the plugin
  description and README.

The invitation stores only its local display state. The plugin does not collect
or transmit telemetry; a network request occurs only when the user explicitly
opens a review or issue link.

## Artifact

- ZIP: `build/distributions/asp-classic-language-support-1.0.0.zip`
- Size: 550221 bytes.
- SHA-256: `419b7872633e41ec09bf16ef10071f73a60f44fbe69c36a5c47619d27fd53611`.
- Minimum IDE build: `252.27397` (PhpStorm 2025.2.4).

## Validation

- All 212 ordinary automated tests pass on the PhpStorm 2025.2.4 compatibility
  baseline and the PhpStorm 2026.2.2 current development target.
- The focused 103-test functional smoke suite passes on IntelliJ IDEA Ultimate
  2026.2.2, WebStorm 2026.2.2 and PyCharm 2026.2.3. It covers formatting,
  completion, resolve, inspections and include-aware Find Usages.
- PyCharm 2026.2.3 was also validated manually with the public demo project.
- Plugin Verifier 1.410 reports the plugin compatible with:
  - PhpStorm `PS-252.27397.112`;
  - PhpStorm `PS-262.10968.76`;
  - PhpStorm 2026.3 EAP `PS-263.5153.42`;
  - IntelliJ IDEA Ultimate `IU-262.10315.125`;
  - WebStorm `WS-262.10315.144`;
  - PyCharm `PY-262.10968.92`.
- Plugin Verifier reports zero compatibility problems and zero deprecated API
  usages on that matrix.
- Plugin structure and project configuration checks pass.

The only verifier notice is the existing restart requirement caused by the
platform's non-dynamic `heavyBracesHighlighter` extension point.

## Release boundary

Known limitations are documented in
[`KNOWN_LIMITATIONS.md`](KNOWN_LIMITATIONS.md). Features intentionally deferred
to later releases are tracked in
[`POST_1_0_ROADMAP.md`](POST_1_0_ROADMAP.md). Regression fixes belong in the
`1.0.x` line; broader language features begin with `1.1.0`.
