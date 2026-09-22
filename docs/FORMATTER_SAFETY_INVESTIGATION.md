# Диагностика сохранности форматирования, 2026-09-03

Исходная исследуемая версия: v0.0.4, commit `6eb1838`. Текущая версия исходного
кода: 1.0.0-rc1. Исходники TTS только читаются; форматирование выполняется на
fixture-копиях в памяти.

## Подтверждённые дефекты защитных механизмов

### Устаревший oracle corpus-теста

`AspTtsFormattingSafetyTest.lexAspScriptlets` искал токен `AspTokenTypes.OUTER`.
После native PSI migration `AspLexer` выдаёт ASP_OPEN / ASP_EXPR_OPEN /
ASP_CLOSE и VBScript-токены. OUTER существует в HTML template-data PSI, но не
в этом потоке lexer. Поэтому VB token fingerprint для ASP был пустым.

`parseErrorCount` также продолжал искать injected PSI, которого больше нет.
Сравнение непробельного текста оставалось рабочим, но две дополнительные
проверки стали бессодержательными.

В тесте исправлены оба пути; самотест проверяет, что fingerprint видит код,
замечает изменение пробела внутри строкового литерала и видит ошибку `Dim =`.

### Слишком поздний snapshot в защитном postprocessor

`AspPostFormatProcessor.formatVbScriptFragments` сохраняет `originalDocumentText`
только при входе в post-format. К этому моменту основной HTML/template
formatter уже мог изменить документ. Сравнение/rollback с этим snapshot не
может обнаружить потерю, случившуюся до его создания. При форматировании
неполного диапазона `processText` вообще пропускает этот проход.

Это подтверждённая дыра в защите, **не доказательство**, что именно
HTML-проход вызвал последнее повреждение пользователя.

В текущем исходном коде дыра закрыта сквозным operation guard: snapshot
снимается в `AspPreFormatProcessor` до платформенного formatter, а
`AspPostFormatProcessor` после всех стадий сравнивает непробельный текст,
ASP/VBScript token fingerprint и число parser errors. Нарушение любого
инварианта восстанавливает исходный документ в рамках той же write-команды.

Кроме того, текущие fingerprints игнорируют EOL; одинаковые непробельные
символы сами по себе не гарантируют одинаковую семантику VBScript или HTML/JS.

### Миграция тестов на рабочую версию IDE на этапе v0.0.5

Проект переведён на локальную PhpStorm 2026.1.2, build PS-261.24374.185:

- Kotlin 2.3.20;
- IntelliJ Platform Gradle Plugin 2.18.1;
- GrammarKit 2023.3.0.3;
- `sinceBuild = 261.24374`;
- ближайший опубликованный test-framework 261.24374.154 запускается с точными
  библиотеками локальной IDE 261.24374.185.

Vue отключён только в тестовом sandbox: его LSP-плагин зависит от production-
раскладки classloader, которой нет в платформенном test harness. Штатные HTML,
JavaScript и CSS остаются включены и участвуют в formatter-тестах.

### Дефекты, найденные после миграции на 261

- Template formatter может передать `null` indent для ASP внутри JavaScript;
  builder теперь безопасно использует `Indent.getNoneIndent()`.
- Внутренние комментарии-маркеры оканчивались `_` и ошибочно считались
  VBScript line continuation. Проверка continuation теперь выполняется lexer-ом.
- Горизонтальный whitespace перед `%>` накапливался при каждом проходе.
- Однострочные управляющие ASP-блоки внутри HTML-тега/атрибута ошибочно
  разворачивались в многострочную форму. Семантическое разворачивание теперь
  разрешено только для scriptlet, стоящего отдельно на строке.
- Spacing ASP-выражений применялся после HTML wrapping, поэтому первый и второй
  проходы могли давать разную ширину строк. Spacing вынесен в pre-format stage.

## Проверенные сценарии

- Corpus-копия TTS (`test-asp-project/TTS`, HEAD `087d025`): все 47 файлов
  `Bugs/`, первые 100 файлов `customers/` в порядке сортировки теста.
  Потерь непробельных символов и изменений VB tokens не обнаружено.
- `Bugs/index.asp`: штатное editor action, участок rsViewers внутри/вне
  viewport, форматирование после Undo — существующие проверки проходят.
- `Bugs/index.asp`, `Bugs/BugAdd.asp`, `customers/orderinfo.asp`: покрытие
  непробельного текста leaf-блоками полное; два editor reformat подряд не
  удаляют символы. Сценарий orderinfo существенно медленнее маленьких файлов.
- 11 синтетических смешанных случаев: ASP внутри HTML/атрибутов/JS/CSS/
  textarea/комментариев, cross-template control flow, незакрытый scriptlet,
  ошибочный ASP и директива. Потери текста и дыр покрытия не обнаружено.
- 28 выделений с разными границами вокруг ASP/HTML/JS и внутри строкового
  литерала: текст сохраняется.
- 4 цикла edit → commit → highlighting → Undo → editor reformat на
  большом синтетическом файле: текст и оба PSI представления согласованы.
- Финальный прогон также выполнен на актуальном TTS (HEAD `017d5bf`, 2275
  подходящих файлов всего): 47 файлов `Bugs/` плюс перечисленные editor/model
  сценарии на актуальных index.asp/BugAdd.asp/orderinfo.asp проходят. Это не
  полный corpus-аудит. Команда:

  ```sh
  ./gradlew test --tests '*AspTtsFormattingSafetyTest' \
    --tests '*AspFormattingModelSafetyTest' --tests '*AspEditorReformatSafetyTest' \
    -PttsProjectDir=/Users/dvamuch/work-projects/TTS \
    -PttsPathFilter=Bugs/ -PttsBatchSize=100 --console=plain
  ```

- После миграции на PhpStorm 2026.1.2 выполнен новый прогон актуального TTS
  (HEAD `521be0b`, 2244 подходящих файла): все 22 файла `Bugs/`, `adovbs.inc`,
  пять DAL-файлов и `core/helpers.inc` сохранили непробельный текст, поток
  VBScript-токенов и не получили новых parser errors. `Bugs/index.asp` и
  `Bugs/BugAdd.asp` идемпотентны уже после первого прохода.

На момент этого расследования проверки выполнялись на PhpStorm 2026.1.2. Они
**не воспроизводили исходную пользовательскую потерю кода**, поэтому нельзя считать
неизвестную первопричину окончательно закрытой. Нужен следующий точный
файл/состояние пользователя. При этом найденные на 261 отдельные дефекты уже
закрыты регрессионными тестами.

### Текущее тестовое окружение

После обновления рабочей машины с macOS 13 тестовая матрица перенесена на
PhpStorm 2026.2.2 (PS-262.10315.130) и JVM target 25. Gradle запускается на
JBR установленной PhpStorm и автоматически provision-ит объявленный toolchain,
если его нет локально. Точная сборка платформы по-прежнему контролируется
`AspPlatformCompatibilityTest`; исторические результаты выше относятся к 261.
Для сборки используется IntelliJ Platform Gradle Plugin 2.19.0. На 262 проходят
все 174 обычных теста и задача `buildPlugin`. После добавления operation guard
повторно прошли 21 файл `Bugs/`, 8 файлов `core/`/DAL и editor/model-сценарии
для `Bugs/index.asp`, `Bugs/BugAdd.asp`, `Bugs/BugInfo.asp` и
`customers/orderinfo.asp`.

16 сентября 2026 года выполнен полный formatter safety-аудит всех 2200
поддерживаемых файлов актуальной рабочей копии TTS (Git HEAD `cb5be77`, включая
локальные незакоммиченные изменения). Проверка шла девятью независимыми
батчами на PhpStorm 2026.2.2/JVM 25. Во всех батчах `Violations: 0`: не
изменились непробельный текст и поток ASP/VBScript-токенов, новых parser errors
не появилось. Отчёты записаны в
`build/reports/tts-format-safety-batch-0000.txt` … `0008.txt`; исходная рабочая
копия TTS не изменялась тестом.

20 сентября 2026 года закрыт известный пробельный дрейф второго прохода на
`customers/orderinfo.asp`. Причиной была повторная подача добавленного
post-format слоем VBScript-отступа в HTML formatter на malformed legacy HTML.
Теперь pre-format снимает точный ранее добавляемый суффикс, после чего
post-format вычисляет семантический отступ заново. Полные `Bugs/index.asp`,
`Bugs/BugAdd.asp` и `customers/orderinfo.asp` идемпотентны после первого
Reformat Code. Исключение из теста удалено.

Operation guard также хранит снимки раздельно для каждого файла и привязывает
их к текущей IDE-команде. Если форматирование было отменено до postprocessor,
его устаревший снимок не может восстановить текст поверх следующей команды.
Снимок удаляется и при исключении внутри собственного pre-format этапа.

После этих изменений полный formatter safety-аудит повторён на актуальной
рабочей копии TTS HEAD `75312cb`, включая локально изменённые
`Bugs/BugInfo.asp` и `customers/orderinfo.asp`. Все 2200 файлов снова прошли
девять батчей с `Violations: 0`; тесты не записывали изменения в TTS.

21 сентября 2026 года на полном `customers/orderinfo.asp` отдельно сравнены
реальные editor-reformat проходы с `HTML_ALIGN_TEXT=false` и `true`. Оба режима
идемпотентны и сохраняют код, но `true` меняет 76 участков обычного HTML-текста
и воспроизводит экстремальные отступы до 285 пробелов. Это поведение штатного
HTML formatter; для TTS настройку `Align text` следует отключить. В ASP formatter
добавлены независимые исправления: lexer-based spacing для incomplete legacy
фрагментов, continuation indent для строк с `_`, выравнивание закрывающей
скобки и ASP-expression на отдельной строке quoted HTML attribute. Регрессии
проверяются при обоих значениях `HTML_ALIGN_TEXT` и двумя проходами.

После этих исправлений снова выполнены все девять corpus-батчей на TTS HEAD
`75312cb`: проверены 2200/2200 файлов, во всех отчётах `Violations: 0`.
`AspTtsFormattingSafetyTest` принудительно использует `Preserve existing`,
поскольку смена регистра ключевых слов — ожидаемое style-преобразование, а
назначение этого gate — обнаруживать потерю символов, изменение потока токенов
и рост parser errors. Default `Title Case` проверяется отдельными integration-
тестами.

22 сентября 2026 года повторно проверен исходный `customers/orderinfo.asp` из
TTS HEAD и отдельно текущая рабочая копия. Причиной оставшегося расхождения
первого прохода было сочетание двух механизмов: platform HTML formatter мог
разнести содержимое исходного inline scriptlet-а, а semantic post-pass
вычислял позицию delimiter-а по глубине до `Else` / `End If`. Теперь post-pass
использует pre-format snapshot для сохранения выбранного inline/multiline вида,
а глубина host-блока берётся как минимальная логическая глубина его строк.
Если один delimiter отделён от кода переносом, оба delimiter-а становятся
отдельными выровненными строками; следующий HTML не остаётся на строке `%>`.
Обе политики доступны в `Editor | Code Style | VBScript` и включены по
умолчанию. На исходном и уже отформатированном `orderinfo.asp` первый и второй
editor reformat идентичны при `HTML_ALIGN_TEXT=false`; код и токены сохранены.
Специальное перестроение многострочного ASP внутри quoted HTML attributes в
эту итерацию намеренно не добавлялось.

После этого полностью повторены девять corpus-батчей на той же рабочей копии
TTS: проверены 2200/2200 файлов, включая `customers/orderinfo.asp` и его крупные
архивные варианты. Во всех отчётах `Violations: 0`; непробельное содержимое,
поток ASP/VBScript-токенов и число parser errors не изменились.

## Производительность полного Reformat Code

На `customers/orderinfo.asp` размером около 476 КБ исходная контрольная точка
реального editor action составляла 7,86 с для первого и 6,29 с для повторного
прохода. IDE в это время выглядела зависшей.

Профилирование показало два архитектурных источника лишней работы:

- каждый HTML formatting block заново обходил всё template PSI в поиске ASP
  outer elements;
- post-format многократно пересчитывал offsets, коммитил PSI между стадиями и
  применял большое число отдельных whitespace edits.

Список outer elements теперь кэшируется на версию PSI и выбирается бинарным
поиском только для нужного диапазона. Fragment extraction выполняется одним
последовательным проходом, whitespace edits применяются пакетно, неизменившиеся
фрагменты не записываются, а лишний временный reformat VBScript PSI удалён.
Длинные циклы содержат cancellation points.

Итог на той же машине и рабочей копии: 3,23 с первый проход и 1,84 с второй.
Optional TTS-тест удерживает более свободные пределы 6 и 4 секунды с учётом
test harness. Дополнительный trace включается только системным свойством через
`-PttsFormatPerformanceTrace=true`.

## Статус исходной гипотезы о потере на границе ASP/HTML

Непокрытый непробельный диапазон или рассинхронизация Document/PSI остаются
правдоподобным объяснением исторического дефекта платформенной formatting-
стадии, но на поддерживаемой платформе 262 он не воспроизводится. Покрытие
formatting blocks проверяется отдельными тестами, а whole-operation guard теперь
обнаруживает и откатывает такое изменение независимо от его источника.

JetBrains прямо предупреждает: все непробельные символы должны быть покрыты
нижними форматирующими блоками, иначе промежутки могут быть удалены:
[Code Formatter — Introduction](https://plugins.jetbrains.com/docs/intellij/code-formatting.html#introduction).

## Тестовые изменения

- `AspTtsFormattingSafetyTest`: актуальный lexer/native PSI, самотест oracle,
  фиксация первого изменения непробельного текста со стеком. При обнаружении
  потери сохраняются `before.txt`, `after.txt`, `first-change.txt` в
  `build/reports/tts-format-diagnostics/<relative-path>/` (не в Git).
- `AspFormattingModelSafetyTest`: покрытие блоками, synthetic mixed cases,
  editor selections, incremental edits/Undo, command/cancellation snapshots,
  performance limits и важные TTS-файлы.

План исправления и критерии приёмки внесены в `DEVELOPMENT_PLAN.md`, приоритет 0.
