# Changelog - Добавление поддержки Ollama

## Что было добавлено

### 1. API и DTO для Ollama
- ✅ `OllamaApi.kt` - интерфейс Retrofit для Ollama API
- ✅ `OllamaChatRequestDto.kt` - DTO для запросов к Ollama
- ✅ `OllamaResponseDto.kt` - DTO для ответов от Ollama
- ✅ `ApiProvider.kt` - enum для выбора между DeepSeek и Ollama

### 2. Обновленная сетевая конфигурация
- ✅ `NetworkModule.kt` - добавлены отдельные Retrofit клиенты для DeepSeek и Ollama
- ✅ Ollama клиент работает без API ключа
- ✅ Настраиваемый базовый URL для Ollama из `local.properties`

### 3. Расширенный Repository
- ✅ `ChatRepository.kt` - поддержка обоих API провайдеров
- ✅ Автоматическое переключение между DeepSeek и Ollama
- ✅ Обработка специфичных ошибок для каждого провайдера

### 4. Управление настройками
- ✅ `SystemPromptProvider.kt` - сохранение выбранного провайдера и моделей
- ✅ Отдельное хранение модели для DeepSeek и Ollama
- ✅ SharedPreferences для персистентности настроек

### 5. UI компоненты
- ✅ `ApiProviderSettingsDialog.kt` - диалог выбора API провайдера и модели
- ✅ Обновлены `SettingsDialog.kt` - добавлена кнопка настроек API
- ✅ Все экраны (Chat, LogicTasks, SimpleChat) поддерживают новый диалог

### 6. ViewModel обновления
- ✅ `ChatViewModel.kt` - методы для работы с API провайдером
- ✅ `LogicTasksViewModel.kt` - методы для работы с API провайдером
- ✅ `SimpleChatViewModel.kt` - методы для работы с API провайдером

### 7. Конфигурация и документация
- ✅ `build.gradle.kts` - чтение `OLLAMA_BASE_URL` из local.properties
- ✅ `local.properties` - добавлен `OLLAMA_BASE_URL`
- ✅ `strings.xml` - строки для UI элементов Ollama
- ✅ `README.md` - обновлена документация
- ✅ `OLLAMA_SETUP.md` - подробная инструкция по настройке Ollama

## Файлы изменены

### Созданные файлы
1. `app/src/main/java/eliseev/aiadvent/chat/data/api/OllamaApi.kt`
2. `app/src/main/java/eliseev/aiadvent/chat/data/api/dto/OllamaChatRequestDto.kt`
3. `app/src/main/java/eliseev/aiadvent/chat/data/api/dto/OllamaResponseDto.kt`
4. `app/src/main/java/eliseev/aiadvent/chat/data/model/ApiProvider.kt`
5. `app/src/main/java/eliseev/aiadvent/chat/presentation/chat/components/ApiProviderSettingsDialog.kt`
6. `OLLAMA_SETUP.md`
7. `CHANGELOG_OLLAMA.md`

### Модифицированные файлы
1. `app/build.gradle.kts` - добавлен OLLAMA_BASE_URL
2. `local.properties` - добавлен OLLAMA_BASE_URL
3. `app/src/main/java/eliseev/aiadvent/chat/di/NetworkModule.kt` - два Retrofit клиента
4. `app/src/main/java/eliseev/aiadvent/chat/di/AppModule.kt` - новые зависимости
5. `app/src/main/java/eliseev/aiadvent/chat/data/repository/ChatRepository.kt` - поддержка двух API
6. `app/src/main/java/eliseev/aiadvent/chat/data/model/SystemPromptProvider.kt` - методы для API провайдера
7. `app/src/main/java/eliseev/aiadvent/chat/presentation/chat/ChatViewModel.kt` - методы API провайдера
8. `app/src/main/java/eliseev/aiadvent/chat/presentation/chat/ChatScreen.kt` - новый диалог
9. `app/src/main/java/eliseev/aiadvent/chat/presentation/chat/components/SettingsDialog.kt` - кнопка API провайдера
10. `app/src/main/java/eliseev/aiadvent/chat/presentation/logictasks/LogicTasksViewModel.kt` - методы API провайдера
11. `app/src/main/java/eliseev/aiadvent/chat/presentation/logictasks/LogicTasksScreen.kt` - новый диалог
12. `app/src/main/java/eliseev/aiadvent/chat/presentation/simplechat/SimpleChatViewModel.kt` - методы API провайдера
13. `app/src/main/java/eliseev/aiadvent/chat/presentation/simplechat/SimpleChatScreen.kt` - новый диалог
14. `app/src/main/res/values/strings.xml` - новые строки
15. `README.md` - обновленная документация

## Как использовать

### Шаг 1: Установите Ollama
```bash
brew install ollama
ollama serve
```

### Шаг 2: Загрузите модель
```bash
ollama pull llama3.2:1b
```

### Шаг 3: Настройте приложение
1. Убедитесь, что в `local.properties` есть:
   ```properties
   OLLAMA_BASE_URL=http://10.0.2.2:11434/
   ```

2. Запустите приложение

3. Откройте **Настройки** → **API провайдер и модель**

4. Выберите **Ollama (локальный)**

5. Введите название модели: `llama3.2:1b`

6. Нажмите **Сохранить**

### Шаг 4: Отправьте сообщение
Теперь все запросы будут отправляться к локальной Ollama вместо DeepSeek!

## Преимущества

### DeepSeek API
- ✅ Высокое качество ответов
- ✅ Не требует мощного компьютера
- ✅ Работает на любом устройстве
- ❌ Требует интернет
- ❌ Платный (хотя и недорогой)

### Ollama
- ✅ Полностью бесплатный
- ✅ Работает офлайн
- ✅ Полный контроль над моделями
- ✅ Приватность (данные не уходят в интернет)
- ❌ Требует мощный компьютер
- ❌ Нужно скачивать модели

## Рекомендуемые модели для сравнения

Для учебного задания по сравнению моделей используйте:

1. **Маленькая:** `llama3.2:1b` (~1GB, быстрая)
2. **Средняя:** `mistral:7b` (~4GB, качественная)
3. **Большая:** `llama3.1:8b` (~5GB, мощная)

## Дополнительная информация

См. подробную инструкцию в [OLLAMA_SETUP.md](OLLAMA_SETUP.md)
