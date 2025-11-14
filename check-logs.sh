#!/bin/bash

# Скрипт для проверки логов IDE

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$PROJECT_DIR/build/idea-sandbox/PS-2024.3.1.1/log"
LOG_FILE="$LOG_DIR/idea.log"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Анализ логов ASP Plugin ===${NC}\n"

if [ ! -f "$LOG_FILE" ]; then
    echo -e "${RED}Файл логов не найден: $LOG_FILE${NC}"
    echo -e "${YELLOW}Возможно, IDE еще не запущен?${NC}"
    exit 1
fi

# Проверяем ошибки ASP
echo -e "${YELLOW}1. Ошибки связанные с ASP:${NC}"
ASP_ERRORS=$(grep -i "asp.*error\|asp.*exception\|aspclassic" "$LOG_FILE" 2>/dev/null | tail -20)
if [ -z "$ASP_ERRORS" ]; then
    echo -e "${GREEN}✓ Ошибок ASP не найдено${NC}"
else
    echo -e "${RED}Найдены ошибки:${NC}"
    echo "$ASP_ERRORS"
fi
echo ""

# Проверяем общие ошибки
echo -e "${YELLOW}2. Последние ERROR записи (все):${NC}"
ERRORS=$(grep "ERROR" "$LOG_FILE" 2>/dev/null | tail -10)
if [ -z "$ERRORS" ]; then
    echo -e "${GREEN}✓ Ошибок не найдено${NC}"
else
    echo "$ERRORS"
fi
echo ""

# Проверяем исключения
echo -e "${YELLOW}3. Последние исключения:${NC}"
EXCEPTIONS=$(grep -A 3 "Exception\|AssertionError" "$LOG_FILE" 2>/dev/null | tail -20)
if [ -z "$EXCEPTIONS" ]; then
    echo -e "${GREEN}✓ Исключений не найдено${NC}"
else
    echo -e "${RED}Найдены исключения:${NC}"
    echo "$EXCEPTIONS"
fi
echo ""

# Проверяем загрузку плагина
echo -e "${YELLOW}4. Информация о плагине:${NC}"
PLUGIN_INFO=$(grep -i "asp classic\|aspclassic\|asp.*plugin" "$LOG_FILE" 2>/dev/null | grep -v "ERROR" | tail -10)
if [ -z "$PLUGIN_INFO" ]; then
    echo -e "${YELLOW}Информация о плагине не найдена${NC}"
else
    echo "$PLUGIN_INFO"
fi
echo ""

# Статистика
echo -e "${BLUE}=== Статистика ===${NC}"
TOTAL_ERRORS=$(grep -c "ERROR" "$LOG_FILE" 2>/dev/null || echo "0")
TOTAL_WARNS=$(grep -c "WARN" "$LOG_FILE" 2>/dev/null || echo "0")
echo "Всего ERROR: $TOTAL_ERRORS"
echo "Всего WARN: $TOTAL_WARNS"
echo ""

echo -e "${BLUE}Полный лог: ${NC}$LOG_FILE"
echo -e "${BLUE}Размер лога: ${NC}$(du -h "$LOG_FILE" | cut -f1)"

