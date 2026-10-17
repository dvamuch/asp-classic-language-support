# 1.1.0 release candidate

## Release status

Version 1.1.0 is prepared and validated locally. It has not been committed,
tagged, published as a GitHub Release, or uploaded to JetBrains Marketplace.
Publishing remains an explicit follow-up action.

This is intentionally a focused release. It adds selection-aware formatting
and the multiline control-flow correction needed for correct selected-block
indentation; unrelated code-analysis backlog remains deferred to 1.2 and later.

## User-visible changes

- Reformat Code now formats selected physical lines in `.asp`, `.inc`, and
  `.vbs` files instead of normalizing the whole document.
- The formatter reads file-wide VBScript control-flow context so a selection
  inside an outer `If`, `For`, `With`, `Select`, procedure, or class receives
  the same indentation as a full-file reformat.
- Text outside the selected line range is preserved exactly. The operation
  guard verifies the unchanged prefix and suffix in addition to the existing
  token, parser-error, cancellation, and rollback checks.
- Selection formatting preserves Undo behavior, CRLF and mixed line endings,
  delimiter boundaries, and second-pass idempotence.
- Multiline block-opening statements joined with `_`, including multiline
  `If ... Then`, now update control-flow depth before their body is indented.

Selection remains deliberately line-based: a selection beginning or ending in
the middle of a line formats that complete physical line. ASP delimiter
placement is restructured only when the complete scriptlet is selected.

## Artifact

- ZIP: `build/distributions/asp-classic-language-support-1.1.0.zip`
- Size: 558534 bytes.
- SHA-256: `d2b2e2fb118a8a16ef632bf0f5a7b9d786189fe5edb80bf7d544e86233450c17`.
- Minimum IDE build: `252.27397` (PhpStorm 2025.2.4).
- Bytecode target: Java 21.

## Validation

- All 224 ordinary automated tests pass on both:
  - PhpStorm `PS-252.27397.112` (2025.2.4), the compatibility baseline;
  - PhpStorm `PS-262.10968.76` (2026.2.3), the current development target.
- Real editor-action tests pass on TTS Git HEAD `189fed0`, including
  `Bugs/index.asp`, `Bugs/BugAdd.asp`, `Bugs/BugInfo.asp`, and
  `customers/orderinfo.asp`.
- A selected multiline SQL-construction block in `customers/orderinfo.asp`
  formats in approximately 1.3–1.6 seconds on the current machine, preserves
  the exact surrounding document, and is unchanged on the second pass.
- The complete read-only formatter safety audit passes for all 2,203 supported
  `.asp`, `.inc`, and `.vbs` files in the current TTS working tree. All nine
  batches preserve non-whitespace text and ASP/VBScript tokens and introduce no
  parser errors. The TTS checkout is not modified.
- The focused 108-test functional smoke suite passes on each of:
  - IntelliJ IDEA Ultimate `IU-262.10315.125` (2026.2.2);
  - WebStorm `WS-262.10315.144` (2026.2.2);
  - PyCharm `PY-262.10968.92` (2026.2.3).
- Plugin Verifier 1.410 reports the plugin compatible with:
  - PhpStorm `PS-252.27397.112`;
  - PhpStorm `PS-262.10968.76`;
  - IntelliJ IDEA Ultimate `IU-262.10315.125`;
  - WebStorm `WS-262.10315.144`;
  - PyCharm `PY-262.10968.92`.
- Plugin project configuration, archive structure, and ZIP assembly checks
  pass.

Plugin Verifier reports the existing dynamic-plugin restriction for
`heavyBracesHighlighter`. Installing, enabling, disabling, or upgrading the
plugin may therefore require an IDE restart. This is expected and is not a
1.1.0 regression.

## Release boundary

The release does not add unused-declaration diagnostics, deeper user-class or
COM type inference, Quick Documentation, a project-wide symbol index,
`global.asa` support, or structural formatting of complex ASP expressions
inside quoted HTML attributes. Those items remain in
[`POST_1_0_ROADMAP.md`](POST_1_0_ROADMAP.md).

## Remaining publication steps

After explicit approval to release:

1. review and commit the complete 1.1.0 change set;
2. push the commit;
3. create and push tag `v1.1.0`;
4. publish the matching GitHub Release, which triggers the Marketplace
   publishing workflow;
5. confirm the GitHub Actions result and Marketplace approval.
