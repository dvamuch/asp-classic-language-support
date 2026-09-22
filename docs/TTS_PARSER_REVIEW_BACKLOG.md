# TTS parser audit

Актуальная классификация parser diagnostics на рабочей копии TTS. Это не
backlog из 42 ошибок плагина: аудит отделяет ограничения парсера от реальных
дефектов и служебных файлов legacy-проекта.

- Дата повторного аудита: 22 сентября 2026 года.
- Корень: `/Users/dvamuch/work-projects/TTS`.
- TTS HEAD: `75312cb`, включая локальные незакоммиченные изменения.
- Проверено: 2200 `.asp` / `.inc` файлов.
- Без parser diagnostics: 2158 файлов.
- С diagnostics: 42 файла.
- Падений и зависаний парсера: 0.
- Время полного прогона: 110,5 с на текущей машине.

Аудит воспроизводится отдельным read-only тестом; исходники TTS не меняются:

```bash
./gradlew test --tests '*AspTtsParserAuditTest' \
  -PttsProjectDir=/Users/dvamuch/work-projects/TTS
```

Полный отчёт создаётся в `build/reports/tts-parser-audit.txt`.

## Что исправлено во время повторного аудита

Первый прогон актуального native file-wide parser показал 82 файла. Из них 40
были ложными diagnostics на двух валидных ASP-конструкциях:

- закрывающий `%>` выражения `<%= ... %>` на следующей строке;
- `Select Case`, у которого заголовок и `Case` находятся в разных ASP-блоках.

Обе конструкции поддержаны, добавлены минимальные regression-тесты. Повторный
полный прогон уменьшил остаток до 42 файлов.

## Классификация остатка

Подтверждённых валидных конструкций, которые не умеет разбирать плагин: **0**.

### Реальные синтаксические дефекты TTS — 40 файлов

Неверный оператор `=>` вместо `>=`:

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

Перенос выражения после `&` без обязательного `_`:

```text
admin/linksu2o.asp
customers/orderrespinfo.asp
monitoring/GroupList.asp
network/networkroutesr.asp
```

Пустая физическая строка сразу после явного continuation marker `_`:

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

Остальные повреждённые legacy/generated-фрагменты: лишние или отсутствующие
скобки и `End If`, незакомментированный текст внутри VBScript, сломанные
кавычки, недопустимый `<% =...%>` и использование ключевого слова `Step` как
имени переменной:

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

Исправление этих файлов не входит в репозиторий плагина и не является блокером
версии 1.0. Parser diagnostics на них полезны и не должны подавляться
расширением грамматики.

### Служебные или не-ASP данные — 2 файла

```text
smt/lang/lang.it.pack/_vti_cnf/smt.base.asp
smt/lang/lang.pt.asp
```

Первый файл содержит метаданные Microsoft FrontPage, второй — quoted-printable
данные. Расширение `.asp` в данном случае не означает исполняемый ASP Classic.

## Историческое сравнение

Аудит от 30 июля 2026 года проверял 2477 файлов и оставлял 52 неоднозначных
случая. Тот список относился к прежней рабочей копии TTS и старой архитектуре
parser/injection, поэтому больше не является рабочим backlog. Текущий аудит
запускается кодом теста, сохраняет подробные descriptions/context и должен
повторяться перед release candidate при изменениях lexer/parser.
