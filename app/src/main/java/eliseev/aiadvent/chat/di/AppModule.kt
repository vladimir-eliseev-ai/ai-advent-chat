package eliseev.aiadvent.chat.di

import android.content.Context
import eliseev.aiadvent.chat.BuildConfig
import eliseev.aiadvent.chat.data.database.ChatDatabase
import eliseev.aiadvent.chat.data.model.AppSettings
import eliseev.aiadvent.chat.data.model.SystemPromptProvider
import eliseev.aiadvent.chat.data.repository.ChatRepository
import eliseev.aiadvent.chat.data.sender.MessageSender
import eliseev.aiadvent.chat.data.service.HistoryCompressionService
import eliseev.aiadvent.chat.data.store.ChatMessageStore
import eliseev.aiadvent.chat.data.store.PersistentChatMessageStore
import eliseev.aiadvent.chat.domain.usecase.GetMessagesUseCase
import eliseev.aiadvent.chat.domain.usecase.GetSystemPromptUseCase
import eliseev.aiadvent.chat.domain.usecase.GetUserSettingsUseCase
import eliseev.aiadvent.chat.domain.usecase.SaveUserSettingsUseCase
import eliseev.aiadvent.chat.domain.usecase.SendMessageUseCase
import eliseev.aiadvent.chat.domain.usecase.SendSimpleMessageUseCase
import eliseev.aiadvent.chat.presentation.chat.ChatViewModel
import eliseev.aiadvent.chat.presentation.logictasks.LogicTasksViewModel
import eliseev.aiadvent.chat.presentation.simplechat.SimpleChatViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single(named("deepseekApiKey")) { BuildConfig.DEEPSEEK_API_KEY }
    single(named("ollamaBaseUrl")) { BuildConfig.OLLAMA_BASE_URL }
    
    single {
        SystemPromptProvider(androidContext())
    }
    
    single {
        AppSettings(androidContext())
    }
    
    // Room Database
    single {
        ChatDatabase.getDatabase(androidContext())
    }
    
    // Персистентные хранилища для разных экранов
    single(named("simpleChatPersistentStore")) {
        PersistentChatMessageStore(
            database = get(),
            storeName = "simple_chat"
        )
    }
    
    single(named("logicTasksPersistentStore")) {
        PersistentChatMessageStore(
            database = get(),
            storeName = "logic_tasks"
        )
    }
    
    single(named("defaultPersistentStore")) {
        PersistentChatMessageStore(
            database = get(),
            storeName = "default"
        )
    }
    
    // Отдельные хранилища для разных экранов
    single(named("simpleChatStore")) {
        ChatMessageStore(get(named("simpleChatPersistentStore")))
    }
    
    single(named("logicTasksStore")) {
        ChatMessageStore(get(named("logicTasksPersistentStore")))
    }
    
    // Для обратной совместимости
    single {
        ChatMessageStore(get(named("defaultPersistentStore")))
    }
}

val repositoryModule = module {
    single { MessageSender(get(), get(), get(), get(), get()) }
    single { HistoryCompressionService(get<MessageSender>()) }
    
    single {
        ChatRepository(
            messageSender = get(),
            messageStore = get(),
            historyCompressionService = get(),
            appSettings = get()
        )
    }
    single(named("simpleChatRepository")) {
        ChatRepository(
            messageSender = get(),
            messageStore = get(named("simpleChatStore")),
            historyCompressionService = get(),
            appSettings = get()
        )
    }
    single(named("logicTasksRepository")) {
        ChatRepository(
            messageSender = get(),
            messageStore = get(named("logicTasksStore")),
            historyCompressionService = get(),
            appSettings = get()
        )
    }
}

val useCaseModule = module {
    single { GetSystemPromptUseCase(get()) }
    single { GetUserSettingsUseCase(get(), get()) }
    single { SaveUserSettingsUseCase(get(), get()) }
    
    // Основные UseCase
    single { GetMessagesUseCase(get()) }
    single { SendMessageUseCase(get()) }
    single { SendSimpleMessageUseCase(get(), get()) }
    
    // UseCase для разных экранов
    single(named("simpleChatSendSimpleMessageUseCase")) {
        SendSimpleMessageUseCase(get(named("simpleChatRepository")), get())
    }
    single(named("logicTasksSendMessageUseCase")) {
        SendMessageUseCase(get(named("logicTasksRepository")))
    }
    single(named("simpleChatGetMessagesUseCase")) {
        GetMessagesUseCase(get(named("simpleChatRepository")))
    }
    single(named("logicTasksGetMessagesUseCase")) {
        GetMessagesUseCase(get(named("logicTasksRepository")))
    }
}

val viewModelModule = module {
    viewModelOf(::ChatViewModel)
    viewModel {
        LogicTasksViewModel(
            sendMessageUseCase = get(named("logicTasksSendMessageUseCase")),
            getMessagesUseCase = get(named("logicTasksGetMessagesUseCase")),
            getUserSettingsUseCase = get(),
            saveUserSettingsUseCase = get()
        )
    }
    viewModel {
        SimpleChatViewModel(
            sendSimpleMessageUseCase = get(named("simpleChatSendSimpleMessageUseCase")),
            getMessagesUseCase = get(named("simpleChatGetMessagesUseCase")),
            getUserSettingsUseCase = get(),
            saveUserSettingsUseCase = get()
        )
    }
}
