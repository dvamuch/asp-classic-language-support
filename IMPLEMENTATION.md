# ASP Classic Language Support - Implementation Details

## Архитектура плагина

Плагин реализован как минимальная обёртка над HTML парсером IntelliJ IDEA.

### Основные компоненты

#### 1. **AspLanguage.kt**
Определяет язык ASP как отдельный язык в IDE.
- Название: "ASP Classic"
- Case-insensitive

#### 2. **AspFileType.kt**
Регистрирует тип файла для .asp и .asa файлов.
- Использует иконку asp.svg
- Привязан к языку ASP

#### 3. **AspParserDefinition.kt**
Ключевой компонент - делегирует всю работу HTML парсеру:
- Использует HTML lexer
- Использует HTML parser
- Использует HTML token types
- Создает ASP файлы с HTML содержимым

#### 4. **AspFile.kt**
PSI представление ASP файла.
- Использует HTML парсинг внутри
- Отображается как "ASP File" в IDE

### Как это работает

1. **Регистрация типа файла**: При открытии .asp файла, IDE распознает его как ASP тип
2. **Парсинг**: AspParserDefinition делегирует парсинг HTML парсеру
3. **Результат**: Файл получает всю функциональность HTML:
   - Подсветка синтаксиса HTML
   - Автодополнение тегов и атрибутов
   - Валидация HTML
   - Навигация по тегам
   - Code folding
   - И всё остальное, что поддерживает HTML в IDE

### Plugin.xml

Регистрирует:
- `fileType` - тип файла ASP
- `lang.parserDefinition` - определение парсера

Зависимости:
- `com.intellij.modules.platform` - базовая платформа
- `com.intellij.modules.lang` - языковая поддержка
- `com.intellij.modules.xml` - поддержка XML/HTML

## Преимущества подхода

✅ Минимальный код - делегирование HTML парсеру
✅ Полная поддержка HTML из коробки
✅ Автоматические обновления вместе с HTML парсером IDE
✅ Стабильность - используется проверенный HTML парсер

## Что поддерживается

- ✅ Подсветка HTML синтаксиса
- ✅ Автодополнение HTML тегов
- ✅ Валидация HTML
- ✅ Навигация по структуре
- ✅ Code folding
- ✅ Сопоставление тегов
- ✅ Форматирование кода
- ✅ Все остальные HTML фичи IDE

## Что НЕ поддерживается (пока)

- ❌ Подсветка VBScript внутри `<% %>` блоков
- ❌ Автодополнение ASP объектов (Request, Response, и т.д.)
- ❌ Парсинг ASP директив

## Файловая структура

```
src/main/kotlin/com/dmitry/aspclassic/
├── AspLanguage.kt           # Определение языка
├── AspFileType.kt           # Регистрация типа файла
├── AspIcons.kt              # Иконка
├── AspParserDefinition.kt   # Делегирование к HTML парсеру
└── psi/
    └── AspFile.kt           # PSI файл

src/main/resources/
├── META-INF/
│   └── plugin.xml           # Конфигурация плагина
└── icons/
    └── asp.svg              # Иконка для .asp файлов
```

## Возможное развитие

Для добавления поддержки VBScript блоков можно:
1. Создать custom lexer с режимами (HTML / VBScript)
2. Использовать Language Injection для VBScript блоков
3. Добавить custom parser для ASP директив

