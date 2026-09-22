# 1.0.0-rc1 release candidate

## Artifact

- ZIP: `build/distributions/asp-classic-language-support-1.0.0-rc1.zip`
- Size: 532004 bytes.
- SHA-256: `a40c56045e46565d1e847f583f7c6f355c35599f26d439440094306646e2c3d1`
- Minimum IDE build: `262.10315`.

## Completed gates

- 206 ordinary automated tests pass on PhpStorm 2026.2.2.
- Formatter safety audit: 2200/2200 TTS files, 0 violations.
- Parser audit: 2200 files, 0 crashes; all 42 remaining diagnostics classified
  as 40 source defects and 2 non-ASP service files.
- `Option Explicit` audit: all 5 TTS files checked with include dependencies;
  no false positives found.
- `customers/orderinfo.asp` editor reformat is idempotent and stays below the
  6 s / 4 s regression limits.
- Plugin Verifier: compatible with PhpStorm 2026.2 and 2026.3, 0 compatibility
  errors. Three deprecated `ReadAction.compute` usages are accepted for 1.0.
- Isolated PhpStorm 2026.2.2 sandbox starts and logs the plugin as loaded.
- ZIP contains one plugin JAR, both plugin icons, no bundled Kotlin runtime and
  no debug action.

## Manual RC acceptance

1. In a clean PhpStorm 2026.2.2 profile, choose **Settings | Plugins | Install
   Plugin from Disk** and select the RC ZIP.
2. Restart the IDE and confirm that `.asp`, `.inc` and `.vbs` files use the
   plugin file types.
3. Open TTS `customers/orderinfo.asp`, keep HTML **Align text** disabled and run
   **Reformat Code** twice. The second pass must make no changes and the IDE must
   remain responsive.
4. Spot-check include navigation, user-class/COM completion, missing-include
   highlighting and `Option Explicit` diagnostics.
5. Use the RC for normal TTS work. Only release-blocking regressions are fixed
   during this period; new features remain frozen.

If these checks pass without a blocker, promote the same feature set to
`1.0.0`. Deferred work is listed in `POST_1_0_ROADMAP.md`; accepted limitations
are listed in `KNOWN_LIMITATIONS.md`.
