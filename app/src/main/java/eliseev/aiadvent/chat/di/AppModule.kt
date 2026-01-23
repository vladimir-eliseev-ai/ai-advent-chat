package eliseev.aiadvent.chat.di

import android.content.Context
import eliseev.aiadvent.chat.BuildConfig
import eliseev.aiadvent.chat.data.model.SystemPromptProvider
import eliseev.aiadvent.chat.data.repository.ChatRepository
import eliseev.aiadvent.chat.data.store.ChatMessageStore
import eliseev.aiadvent.chat.domain.usecase.CompressHistoryUseCase
import eliseev.aiadvent.chat.domain.usecase.SendMessageUseCase
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
    
    // Отдельные хранилища для разных экранов
    single(named("simpleChatStore")) {
        ChatMessageStore()
    }
    
    single(named("logicTasksStore")) {
        ChatMessageStore()
    }
    
    // Для обратной совместимости
    single {
        ChatMessageStore()
    }
}

val repositoryModule = module {
    single { ChatRepository(get(), get(), get()) }
}

val useCaseModule = module {
    single { SendMessageUseCase(get()) }
    single { CompressHistoryUseCase(get()) }
}

val viewModelModule = module {
    viewModelOf(::ChatViewModel)
    viewModel {
        LogicTasksViewModel(
            repository = get(),
            messageStore = get(named("logicTasksStore")),
            systemPromptProvider = get()
        )
    }
    viewModel {
        SimpleChatViewModel(
            repository = get(),
            messageStore = get(named("simpleChatStore")),
            systemPromptProvider = get()
        )
    }
}
