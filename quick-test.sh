#!/bin/bash

# Быстрый запуск для тестирования
# Использование: ./quick-test.sh

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

TEST_PROJECT="/Users/dmitry-zap/PhpstormProjects/asp-test-project"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${GREEN}🚀 Быстрый тест ASP плагина${NC}\n"

# Остановить старые процессы
echo -e "${YELLOW}1. Остановка старых процессов...${NC}"
pkill -9 -f "runIde" 2>/dev/null || true
pkill -9 java 2>/dev/null || true
sleep 2

# Сборка
echo -e "${YELLOW}2. Сборка плагина...${NC}"
./gradlew clean build --quiet
echo -e "${GREEN}✓ Готово${NC}\n"

# Запуск
echo -e "${YELLOW}3. Запуск IDE с тестовым проектом...${NC}"
echo -e "${CYAN}   Проект: $TEST_PROJECT${NC}"
echo -e "${CYAN}   Файл: CustomerMail2Send.asp${NC}\n"

# Запускаем с аргументами для открытия проекта
./gradlew runIde \
    -Dide.open.project="$TEST_PROJECT" \
    --args="$TEST_PROJECT $TEST_PROJECT/CustomerMail2Send.asp" &

echo -e "${GREEN}✓ IDE запускается...${NC}"
echo -e "${CYAN}   Логи: build/idea-sandbox/PS-2024.3.1.1/log/idea.log${NC}\n"

# Ждем немного и показываем логи
sleep 15
echo -e "${YELLOW}Проверка логов через 15 секунд...${NC}\n"

./check-logs.sh

echo -e "\n${GREEN}IDE должен открыться с вашим проектом!${NC}"
echo -e "${CYAN}Для мониторинга логов запустите: ./check-logs.sh${NC}"

