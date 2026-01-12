package eliseev.aiadvent.chat.di

import eliseev.aiadvent.chat.BuildConfig
import eliseev.aiadvent.chat.data.repository.ChatRepository
import eliseev.aiadvent.chat.domain.usecase.SendMessageUseCase
import eliseev.aiadvent.chat.presentation.chat.ChatViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { BuildConfig.DEEPSEEK_API_KEY }
}

val repositoryModule = module {
    single { ChatRepository(get()) }
}

val useCaseModule = module {
    single { SendMessageUseCase(get()) }
}

val viewModelModule = module {
    viewModelOf(::ChatViewModel)
}
