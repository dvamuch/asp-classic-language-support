#!/bin/bash

# Скрипт для запуска PhpStorm с плагином и мониторинга логов

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== ASP Classic Plugin - Запуск и мониторинг ===${NC}\n"

# 1. Останавливаем старые процессы
echo -e "${YELLOW}1. Останавливаем старые процессы IDE...${NC}"
pkill -9 -f "gradlew runIde" 2>/dev/null || true
pkill -9 -f "idea-sandbox" 2>/dev/null || true
sleep 2

# 2. Очищаем старые логи
echo -e "${YELLOW}2. Очищаем старые логи...${NC}"
LOG_DIR="$PROJECT_DIR/build/idea-sandbox/PS-2024.3.1.1/log"
rm -f "$LOG_DIR/idea.log" 2>/dev/null || true

# 3. Собираем плагин
echo -e "${YELLOW}3. Собираем плагин...${NC}"
./gradlew clean build --quiet || {
    echo -e "${RED}Ошибка сборки!${NC}"
    exit 1
}
echo -e "${GREEN}✓ Плагин собран${NC}\n"

# 4. Запускаем IDE в фоне
echo -e "${YELLOW}4. Запускаем PhpStorm...${NC}"
./gradlew runIde > /tmp/asp-ide-stdout.log 2>&1 &
IDE_PID=$!
echo -e "${GREEN}✓ IDE запущен (PID: $IDE_PID)${NC}"

# 5. Ждем создания файла логов
echo -e "${YELLOW}5. Ожидаем запуска IDE и создания логов...${NC}"
WAIT_COUNT=0
while [ ! -f "$LOG_DIR/idea.log" ] && [ $WAIT_COUNT -lt 60 ]; do
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
    echo -n "."
done
echo ""

if [ ! -f "$LOG_DIR/idea.log" ]; then
    echo -e "${RED}Ошибка: Файл логов не создан за 60 секунд${NC}"
    echo -e "${YELLOW}Проверьте /tmp/asp-ide-stdout.log${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Логи найдены: $LOG_DIR/idea.log${NC}\n"

# 6. Показываем инструкции
echo -e "${GREEN}=== IDE запущен! ===${NC}\n"
echo -e "${YELLOW}Что делать дальше:${NC}"
echo "1. Откройте test-example.asp в IDE"
echo "2. Проверьте, что файл распознаётся как ASP"
echo "3. Проверьте HTML подсветку и автодополнение"
echo ""
echo -e "${YELLOW}Мониторинг логов:${NC}"
echo "  - Этот терминал показывает ошибки в реальном времени"
echo "  - Полные логи: $LOG_DIR/idea.log"
echo "  - Stdout IDE: /tmp/asp-ide-stdout.log"
echo ""
echo -e "${YELLOW}Для остановки:${NC}"
echo "  - Закройте IDE или нажмите Ctrl+C в этом терминале"
echo ""
echo -e "${GREEN}=== Мониторинг логов (показываются только ERROR и ASP-related) ===${NC}\n"

# 7. Мониторим логи
tail -f "$LOG_DIR/idea.log" | grep --line-buffered -i "error\|exception\|asp\|assertion" | while read line; do
    if echo "$line" | grep -qi "error\|exception\|assertion"; then
        echo -e "${RED}$line${NC}"
    else
        echo -e "${GREEN}$line${NC}"
    fi
done

# Cleanup при выходе
trap "kill $IDE_PID 2>/dev/null || true" EXIT

