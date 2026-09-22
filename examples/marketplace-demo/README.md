# ASP Classic Marketplace Demo

This small project contains synthetic, public-safe ASP Classic examples for the
JetBrains Marketplace screenshots. It has no application secrets, customer
data, internal URLs, or dependencies on the TTS project.

Open this directory as a standalone project in PhpStorm after installing the
ASP Classic Language Support plugin.

## Recommended IDE setup

- Use a standard PhpStorm light or dark theme.
- Set the editor font to approximately 16–18 px.
- Keep HTML **Align text** disabled.
- Keep the default VBScript settings: **Title Case**, spaces inside ASP
  delimiters, and matching delimiter placement.
- Use the same IDE window size for every image, preferably 1440×900 or
  1600×1000.
- Crop to the IDE window and exclude the macOS desktop, Dock, notifications,
  absolute paths, and unrelated tool windows.

## Screenshot 1 — mixed ASP and HTML

Open `default.asp`. Show the Project tool window narrowly enough to include the
ASP/INC file icons, while keeping the orders table and its surrounding
VBScript control flow visible.

Suggested caption:

> ASP Classic and VBScript support in mixed HTML templates

## Screenshot 2 — completion

Open `completion-demo.asp`.

1. On the `Response.Write` line, remove `Write`, leave the caret after
   `Response.`, and invoke completion.
2. Alternatively, leave the caret after `orderService.` to show completion for
   a user class declared in an included file.

Suggested caption:

> Context-aware completion for ASP built-ins and user classes

Undo the temporary edit after taking the screenshot.

## Screenshot 3 — include-aware navigation

Open `includes/order-service.inc`, place the caret on `GetRecentOrders`, and run
**Find Usages**. Keep the result in `default.asp` visible in the Find tool
window. The include directive in `default.asp` can be used for a second
navigation-focused image if desired.

Suggested caption:

> Include-aware navigation and Find Usages across ASP and INC files

## Screenshot 4 — formatting

Select `formatting-before.asp` and `formatting-after.asp` in the Project tool
window and choose **Compare Files**. The IDE diff should show only whitespace
and keyword-case changes.

Suggested caption:

> Stable formatting for VBScript control flow spanning HTML templates

## Optional screenshot — inspections

Open `inspection-demo.asp`. It intentionally contains one undeclared identifier
under `Option Explicit` and one missing include. Show both editor highlights or
one quick-documentation popup. Do not use this file in the general overview
screenshot because its diagnostics are intentional.

Suggested caption:

> Conservative Option Explicit and missing-include diagnostics
