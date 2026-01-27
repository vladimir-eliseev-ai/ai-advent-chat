# Инструкция по сборке NewsAPI MCP Server

## Быстрая сборка

```bash
cd newsapi-mcp-server
./gradlew shadowJar
```

После успешной сборки JAR файл будет здесь:
```
build/libs/newsapi-mcp-server-1.0.0.jar
```

## Если возникают ошибки

### Ошибка: Java не найдена

**Решение через Android Studio:**
1. Откройте Android Studio
2. File → Open → выберите папку `newsapi-mcp-server`
3. Дождитесь синхронизации Gradle
4. В терминале Android Studio выполните: `./gradlew shadowJar`

**Решение через командную строку:**
```bash
# Найдите Java
/usr/libexec/java_home -V

# Установите JAVA_HOME (замените на вашу версию)
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home -v 17 2>/dev/null)

# Соберите
./gradlew shadowJar
```

### Ошибка: JVM Target несовместим

Если видите ошибку про JVM target:
```bash
./gradlew clean --no-daemon
rm -rf ~/.gradle/caches/
./gradlew shadowJar
```

### Проверка сборки

После сборки проверьте наличие файла:
```bash
ls -lh build/libs/newsapi-mcp-server-1.0.0.jar
```

Если файл существует, сборка успешна! ✅

## Тестирование сервера

После сборки можно протестировать сервер вручную:

```bash
export NEWS_API_KEY="ваш_api_ключ"
java -jar build/libs/newsapi-mcp-server-1.0.0.jar
```

Сервер должен запуститься и ждать команды через stdin.
