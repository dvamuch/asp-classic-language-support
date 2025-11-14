#!/bin/bash

# Полный цикл тестирования плагина:
# 1. Сборка плагина
# 2. Запуск IDE с тестовым проектом
# 3. Мониторинг логов
# 4. Перезапуск после закрытия IDE

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

# Настройки
TEST_PROJECT="/Users/dmitry-zap/PhpstormProjects/asp-test-project"
TEST_FILE="$TEST_PROJECT/CustomerMail2Send.asp"
LOG_DIR="$PROJECT_DIR/build/idea-sandbox/PS-2024.3.1.1/log"
LOG_FILE="$LOG_DIR/idea.log"

# Цвета
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

clear
echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║        ASP Classic Plugin - Автоматическое тестирование       ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}\n"

# Функция для остановки IDE
stop_ide() {
    echo -e "${YELLOW}⏹  Останавливаю IDE...${NC}"
    pkill -9 -f "runIde" 2>/dev/null || true
    pkill -9 -f "idea-sandbox" 2>/dev/null || true
    pkill -9 java 2>/dev/null || true
    sleep 3
    echo -e "${GREEN}✓ IDE остановлен${NC}\n"
}

# Функция для сборки плагина
build_plugin() {
    echo -e "${YELLOW}🔨 Сборка плагина...${NC}"
    ./gradlew clean build --quiet
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Плагин собран успешно${NC}\n"
        return 0
    else
        echo -e "${RED}✗ Ошибка сборки!${NC}"
        return 1
    fi
}

# Функция для запуска IDE
start_ide() {
    echo -e "${YELLOW}🚀 Запуск PhpStorm...${NC}"
    echo -e "${CYAN}   Проект: $TEST_PROJECT${NC}"
    echo -e "${CYAN}   Файл: CustomerMail2Send.asp${NC}\n"
    
    # Очищаем старые логи
    rm -f "$LOG_FILE" 2>/dev/null || true
    
    # Запускаем IDE в фоне
    nohup ./gradlew runIde \
        -Dide.open.project="$TEST_PROJECT" \
        -Dide.open.file="$TEST_FILE" \
        > /tmp/asp-ide-output.log 2>&1 &
    
    IDE_PID=$!
    echo -e "${GREEN}✓ IDE запущен (PID: $IDE_PID)${NC}\n"
    
    # Ждем создания лог-файла
    echo -e "${YELLOW}⏳ Ожидание запуска IDE...${NC}"
    WAIT_COUNT=0
    while [ ! -f "$LOG_FILE" ] && [ $WAIT_COUNT -lt 60 ]; do
        sleep 1
        WAIT_COUNT=$((WAIT_COUNT + 1))
        printf "."
    done
    echo ""
    
    if [ ! -f "$LOG_FILE" ]; then
        echo -e "${RED}✗ Лог-файл не создан. Проверьте /tmp/asp-ide-output.log${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✓ IDE запущен успешно${NC}\n"
    return 0
}

# Функция для проверки логов
check_logs() {
    echo -e "${BLUE}═══ Проверка логов ===${NC}\n"
    
    sleep 5  # Даем время IDE запуститься
    
    # Проверяем ошибки ASP
    ASP_ERRORS=$(grep -i "asp.*error\|asp.*exception\|aspclassic" "$LOG_FILE" 2>/dev/null | grep -v "INFO" | tail -5)
    
    if [ -z "$ASP_ERRORS" ]; then
        echo -e "${GREEN}✓ Ошибок ASP не найдено${NC}"
    else
        echo -e "${RED}⚠ Найдены ошибки ASP:${NC}"
        echo "$ASP_ERRORS"
    fi
    
    # Проверяем критические ошибки
    CRITICAL=$(grep -i "ERROR\|SEVERE\|AssertionError" "$LOG_FILE" 2>/dev/null | tail -3)
    
    if [ -z "$CRITICAL" ]; then
        echo -e "${GREEN}✓ Критических ошибок не найдено${NC}"
    else
        echo -e "${RED}⚠ Критические ошибки:${NC}"
        echo "$CRITICAL"
    fi
    
    echo ""
}

# Функция для мониторинга логов в реальном времени
monitor_logs() {
    echo -e "${BLUE}═══ Мониторинг логов (Ctrl+C для остановки) ===${NC}\n"
    echo -e "${CYAN}Показываются только ошибки и важные события...${NC}\n"
    
    tail -f "$LOG_FILE" 2>/dev/null | grep --line-buffered -i "error\|exception\|assertion\|asp" | while read line; do
        if echo "$line" | grep -qi "error\|exception\|assertion"; then
            echo -e "${RED}$line${NC}"
        else
            echo -e "${CYAN}$line${NC}"
        fi
    done
}

# Основной цикл
while true; do
    # Останавливаем предыдущий экземпляр
    stop_ide
    
    # Собираем плагин
    if ! build_plugin; then
        echo -e "${RED}Не могу продолжить без успешной сборки${NC}"
        exit 1
    fi
    
    # Запускаем IDE
    if ! start_ide; then
        echo -e "${RED}Не удалось запустить IDE${NC}"
        exit 1
    fi
    
    # Даем время IDE полностью запуститься
    sleep 10
    
    # Проверяем логи
    check_logs
    
    # Инструкции для пользователя
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                     IDE ЗАПУЩЕН!                              ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════╝${NC}\n"
    
    echo -e "${CYAN}📋 Тестовый проект открыт:${NC} $TEST_PROJECT"
    echo -e "${CYAN}📄 Файл открыт:${NC} CustomerMail2Send.asp\n"
    
    echo -e "${YELLOW}🧪 Что проверить:${NC}"
    echo "  1. Файл .asp имеет правильную иконку"
    echo "  2. HTML теги подсвечиваются"
    echo "  3. Ctrl+Space показывает автодополнение HTML"
    echo "  4. Навигация по тегам работает (клик на <html>)"
    echo "  5. Форматирование работает (Ctrl+Alt+L)"
    echo ""
    
    echo -e "${YELLOW}⌨️  Команды:${NC}"
    echo "  ${GREEN}m${NC} - Показать мониторинг логов в реальном времени"
    echo "  ${GREEN}l${NC} - Проверить логи сейчас"
    echo "  ${GREEN}r${NC} - Перезапустить IDE (пересобрать плагин)"
    echo "  ${GREEN}q${NC} - Выход"
    echo ""
    
    # Ждем команды от пользователя
    while true; do
        read -p "$(echo -e ${CYAN}Команда:${NC}) " -n 1 -r
        echo ""
        
        case $REPLY in
            m|M)
                monitor_logs
                ;;
            l|L)
                check_logs
                ;;
            r|R)
                echo -e "\n${YELLOW}Перезапуск IDE...${NC}\n"
                break
                ;;
            q|Q)
                echo -e "\n${YELLOW}Завершение работы...${NC}"
                stop_ide
                echo -e "${GREEN}Готово!${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}Неизвестная команда. Используйте: m, l, r или q${NC}"
                ;;
        esac
    done
done

