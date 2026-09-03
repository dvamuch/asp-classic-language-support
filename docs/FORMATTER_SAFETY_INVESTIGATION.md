# Диагностика сохранности форматирования, 2026-09-03

Исходная исследуемая версия: v0.0.4, commit `6eb1838`. Текущая исправленная
версия: v0.0.5. Исходники TTS только читаются; форматирование выполняется на
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

Кроме того, текущие fingerprints игнорируют EOL; одинаковые непробельные
символы сами по себе не гарантируют одинаковую семантику VBScript или HTML/JS.

### Миграция тестов на рабочую версию IDE

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

Актуальные проверки выполняются на PhpStorm 2026.1.2. Они пока **не
воспроизводят исходную пользовательскую потерю кода**, поэтому нельзя считать
неизвестную первопричину окончательно закрытой. Нужен следующий точный
файл/состояние пользователя. При этом найденные на 261 отдельные дефекты уже
закрыты регрессионными тестами.

## Рабочая гипотеза, которую ещё нужно проверить

Непокрытый непробельный диапазон, пересечение/выход за родительский Block или
рассинхронизация диапазонов Document/PSI на основном template-formatting этапе.
В пользу проверки этого механизма — характер потери на границе ASP/HTML и
поздняя защита post-format; против утверждения о доказанной причине — все
проверенные деревья на 261 пока покрывают исходник полностью.

JetBrains прямо предупреждает: все непробельные символы должны быть покрыты
нижними форматирующими блоками, иначе промежутки могут быть удалены:
[Code Formatter — Introduction](https://plugins.jetbrains.com/docs/intellij/code-formatting.html#introduction).

## Тестовые изменения

- `AspTtsFormattingSafetyTest`: актуальный lexer/native PSI, самотест oracle,
  фиксация первого изменения непробельного текста со стеком. При обнаружении
  потери сохраняются `before.txt`, `after.txt`, `first-change.txt` в
  `build/reports/tts-format-diagnostics/<relative-path>/` (не в Git).
- `AspFormattingModelSafetyTest`: покрытие блоками, synthetic mixed cases,
  editor selections, incremental edits/Undo и важные TTS-файлы.

План исправления и критерии приёмки внесены в `DEVELOPMENT_PLAN.md`, приоритет 0.
