# Known limitations for 1.0

- Поддерживаемая линия IDE начинается с PhpStorm build `262.10315`. Более
  старые IDE намеренно не заявлены как совместимые.
- HTML-часть ASP форматирует встроенный HTML formatter. Для глубоко вложенных
  legacy-страниц рекомендуется отключить `Editor | Code Style | HTML | Other |
  Align text`, иначе платформа может создавать очень длинные отступы.
- ASP-код внутри сложных многострочных quoted HTML attributes сохраняется
  безопасно, но plugin пока не перестраивает структуру таких атрибутов.
- Анализ динамических COM-объектов консервативен: неизвестные member names не
  считаются ошибками, чтобы не создавать массовые false positives.
- `global.asa` и server-side `<script runat="server">` ещё не имеют отдельной
  ASP lifecycle-модели.
- Плагин нельзя включить или выключить без перезапуска IDE из-за нединамического
  extension point подсветки парных ключевых слов.
- Три вызова `ReadAction.compute(ThrowableComputable)` помечены deprecated в
  PhpStorm 2026.2/2026.3, но Plugin Verifier подтверждает бинарную совместимость.
  Их миграция запланирована после 1.0.
