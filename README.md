# AI Advent Chat

Android приложение для общения с AI через Deepseek API, построенное с использованием Jetpack Compose и современных Android практик.

## 🚀 Технологии

- **UI**: Jetpack Compose + Material Design 3
- **Архитектура**: Clean Architecture + MVVM
- **Сеть**: Retrofit 2 + Kotlinx Serialization
- **DI**: Hilt
- **Асинхронность**: Kotlin Coroutines + Flow
- **Логирование**: Timber

## 📋 Требования

- Android Studio Hedgehog или новее
- JDK 11 или выше
- Min SDK: 24 (Android 7.0)
- Target SDK: 36
- Kotlin 2.0.21

## ⚙️ Настройка

### 1. Получение API ключа

1. Перейдите на [Deepseek Platform](https://platform.deepseek.com/api_keys)
2. Зарегистрируйтесь или войдите в аккаунт
3. Создайте новый API ключ
4. Скопируйте ключ (он понадобится на следующем шаге)

### 2. Настройка API ключа

Добавьте ваш API ключ в файл `local.properties` (находится в корне проекта):

```properties
DEEPSEEK_API_KEY=ваш_api_ключ_здесь
```

### 3. Синхронизация проекта

1. Откройте проект в Android Studio
2. Дождитесь завершения индексации
3. Нажмите **File → Sync Project with Gradle Files** (или кнопку Sync в панели уведомлений)
4. Дождитесь завершения синхронизации

### 4. Запуск приложения

1. Подключите Android устройство или запустите эмулятор
2. Нажмите **Run** (▶️) или используйте горячую клавишу `Shift + F10`
3. Дождитесь установки и запуска приложения

## 📱 Использование

1. **Отправка сообщений**: Введите текст в поле ввода и нажмите кнопку отправки (или Enter)
2. **Просмотр истории**: Все сообщения отображаются в хронологическом порядке
3. **Индикатор загрузки**: При ожидании ответа от AI отображается индикатор загрузки
4. **Обработка ошибок**: Ошибки отображаются через Snackbar с понятными сообщениями

## 🏗️ Архитектура проекта

```
app/src/main/java/eliseev/aiadvent/chat/
├── data/                    # Data слой
│   ├── api/                # API интерфейсы и DTO
│   │   ├── DeepSeekApi.kt
│   │   └── dto/
│   ├── model/              # Data модели
│   └── repository/         # Репозитории
│
├── domain/                 # Domain слой
│   ├── model/             # Domain модели
│   └── usecase/           # Use Cases
│
├── presentation/          # Presentation слой
│   └── chat/
│       ├── ChatViewModel.kt
│       ├── ChatScreen.kt
│       └── components/
│
└── di/                     # Dependency Injection
    ├── AppModule.kt
    └── NetworkModule.kt
```

## 🔧 Конфигурация

### Изменение базового URL API

Если нужно изменить базовый URL API, отредактируйте файл:
```
app/src/main/java/eliseev/aiadvent/chat/di/NetworkModule.kt
```

Найдите строку:
```kotlin
.baseUrl("https://api.deepseek.com/")
```

### Изменение системного промпта

Системный промпт настраивается в `ChatViewModel.kt`:
```kotlin
ChatMessage(
    role = MessageRole.SYSTEM,
    content = "You are a helpful assistant."
)
```

## 🐛 Решение проблем

### Ошибка: "Empty response from API"
- Проверьте правильность API ключа в `local.properties`
- Убедитесь, что ключ активен на платформе Deepseek
- Проверьте подключение к интернету

### Ошибка: "Unknown error occurred"
- Проверьте логи в Logcat для детальной информации
- Убедитесь, что API ключ имеет необходимые разрешения
- Проверьте лимиты использования API

### Приложение не компилируется
- Убедитесь, что все зависимости синхронизированы
- Выполните **Build → Clean Project**, затем **Build → Rebuild Project**
- Проверьте версию Android Studio и Gradle

### API ключ не читается
- Убедитесь, что файл `local.properties` находится в корне проекта
- Проверьте формат: `DEEPSEEK_API_KEY=ваш_ключ` (без пробелов вокруг `=`)
- Выполните синхронизацию Gradle после изменения файла

## 📝 Особенности реализации

- ✅ Только вертикальная ориентация экрана
- ✅ Material Design 3 с поддержкой темной темы
- ✅ Обработка ошибок с понятными сообщениями
- ✅ Индикаторы загрузки
- ✅ Автоматическая прокрутка к новым сообщениям
- ✅ Валидация ввода (нельзя отправить пустое сообщение)
- ✅ Безопасное хранение API ключа (не в коде)

## 📄 Лицензия

Этот проект создан для образовательных целей.

## 👤 Автор

Елисеев Владимир

---
