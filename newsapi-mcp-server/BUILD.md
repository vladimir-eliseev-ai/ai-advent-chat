# Инструкция по сборке NewsAPI MCP Server

## Проблема: Java не найдена

Если вы видите ошибку "Unable to locate a Java Runtime", нужно настроить Java.

## Решение 1: Использовать Gradle из основного проекта

Если у вас установлен Android Studio, используйте Gradle из основного проекта:

```bash
cd /Users/Eliseev.Vladimir20/AndroidStudioProjects/AIAdventChat
./gradlew :newsapi-mcp-server:shadowJar
```

Но сначала нужно добавить модуль в `settings.gradle.kts` (см. ниже).

## Решение 2: Собрать через Android Studio / IntelliJ IDEA

1. Откройте Android Studio
2. File → Open → выберите папку `newsapi-mcp-server`
3. Дождитесь синхронизации Gradle
4. В терминале Android Studio выполните:
   ```bash
   ./gradlew shadowJar
   ```

## Решение 3: Настроить JAVA_HOME вручную

Если Java установлена через Android Studio:

```bash
# Найдите путь к Java
/usr/libexec/java_home -V

# Установите JAVA_HOME (замените путь на ваш)
export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || /usr/libexec/java_home -v 11 2>/dev/null)

# Соберите проект
cd newsapi-mcp-server
./gradlew shadowJar
```

## Решение 4: Установить Java отдельно

Если Java не установлена:

1. Скачайте JDK 11 или выше с https://adoptium.net/
2. Установите
3. Настройте JAVA_HOME:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home
   ```
4. Соберите проект

## Проверка сборки

После успешной сборки файл должен появиться здесь:
```
newsapi-mcp-server/build/libs/newsapi-mcp-server-1.0.0.jar
```

Проверьте:
```bash
ls -lh newsapi-mcp-server/build/libs/newsapi-mcp-server-1.0.0.jar
```

## Быстрый способ (если Android Studio открыт)

1. Откройте терминал в Android Studio (View → Tool Windows → Terminal)
2. Выполните:
   ```bash
   cd newsapi-mcp-server
   ./gradlew shadowJar
   ```

Android Studio автоматически настроит Java для Gradle.
