# TTS parser audit

This is the current classification of parser diagnostics in the working TTS
checkout. It is not a backlog of 42 plugin bugs: the audit separates parser
limitations from real source defects and non-ASP service files in the legacy
project.

- Audit repeated on September 22, 2026.
- Root: `/Users/dvamuch/work-projects/TTS`.
- TTS HEAD: `75312cb`, including local uncommitted changes.
- Files checked: 2,200 `.asp` and `.inc` files.
- Files without parser diagnostics: 2,158.
- Files with diagnostics: 42.
- Parser crashes or hangs: 0.
- Full run time: 110.5 seconds on the current machine.

The audit is reproducible through a dedicated read-only test. It does not
modify TTS sources:

```bash
./gradlew test --tests '*AspTtsParserAuditTest' \
  -PttsProjectDir=/Users/dvamuch/work-projects/TTS
```

The complete report is written to `build/reports/tts-parser-audit.txt`.

## Fixes made during the repeated audit

The first run of the current native file-wide parser reported 82 files. Forty
were false diagnostics caused by two valid ASP constructs:

- the closing `%>` of an `<%= ... %>` expression appearing on the next line;
- a `Select Case` whose header and `Case` clauses are in different ASP blocks.

Both constructs are now supported and covered by minimal regression tests. A
second complete run reduced the remainder to 42 files.

## Classification of the remaining files

Confirmed valid constructs unsupported by the plugin: **0**.

### Real TTS syntax defects — 40 files

Invalid `=>` operator instead of `>=`:

```text
PhoneNumbers/functions.asp
customers/customerBizservices.asp
customers/customerinfo.asp
customers/customerservices.asp
customers/customerservices_old.asp
inc/Phone_sl.asp
inc/functions.asp
inc/functions_sl.asp
```

Expression continued after `&` without the required `_` marker:

```text
admin/linksu2o.asp
customers/orderrespinfo.asp
monitoring/GroupList.asp
network/networkroutesr.asp
```

An empty physical line immediately after an explicit `_` continuation marker:

```text
services/sendmail_pservice_memo.asp
troubles/ContactAdd2Trouble.asp
troubles/ContactAdd2Trouble_old.asp
troubles/troubleadd_b.asp
troubles/troubleadd_old.asp
troubles/troubleadd_sl.asp
troubles/troublechangestatus.asp
troubles/troublechangestatus1.asp
troubles/troublechangestatus_b.asp
troubles/troublechangestatus_sl.asp
troubles/troublereport.asp
troubles/troublereport_b.asp
troubles/troublereport_sl.asp
troubles/troublereportxls_new.asp
```

Other damaged legacy/generated fragments: extra or missing parentheses and
`End If` clauses, uncommented prose inside VBScript, broken quotes, invalid
`<% =...%>` syntax, and use of the `Step` keyword as a variable name:

```text
500-100.asp
Cables/index.asp
customers/ordermsg2billing.asp
customers/ordersRepor-type12-xls.asp
devices/configdelete.asp
files/update.asp
inc/500-101.asp
network/aspointadd_.asp
services/serviceitemsListchangestatus1.asp
spam/filters.asp
troubles/TroubleChanges_steps1-3.asp
users/sms_new1.asp
users/user2groupadd.asp
users/userorders_sl3.asp
```

Fixing these files is outside the plugin repository and was not a blocker for
version 1.0. Their parser diagnostics are useful and must not be suppressed by
loosening the grammar.

### Service or non-ASP data — 2 files

```text
smt/lang/lang.it.pack/_vti_cnf/smt.base.asp
smt/lang/lang.pt.asp
```

The first file contains Microsoft FrontPage metadata; the second contains
quoted-printable data. An `.asp` extension does not make either file executable
ASP Classic source.

## Historical comparison

The July 30, 2026 audit checked 2,477 files and left 52 ambiguous cases. That
list belonged to an older TTS checkout and the former parser/injection
architecture, so it is no longer an active backlog. The current audit is run by
the test code, records detailed descriptions and context, and should be
repeated before a release candidate whenever the lexer or parser changes.
