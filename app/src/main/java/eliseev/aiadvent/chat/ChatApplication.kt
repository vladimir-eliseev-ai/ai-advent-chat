package eliseev.aiadvent.chat

import android.app.Application
import eliseev.aiadvent.chat.di.appModule
import eliseev.aiadvent.chat.di.networkModule
import eliseev.aiadvent.chat.di.repositoryModule
import eliseev.aiadvent.chat.di.useCaseModule
import eliseev.aiadvent.chat.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class ChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        startKoin {
            androidContext(this@ChatApplication)
            modules(
                appModule,
                networkModule,
                repositoryModule,
                useCaseModule,
                viewModelModule
            )
        }
    }
}

