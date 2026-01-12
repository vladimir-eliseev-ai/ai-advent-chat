package eliseev.aiadvent.chat.di

import eliseev.aiadvent.chat.data.api.DeepSeekApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.dsl.module
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ApiKeyInterceptor(
    private val apiKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        if (apiKey.isBlank()) {
            throw IllegalStateException("API ключ не установлен. Добавьте DEEPSEEK_API_KEY в local.properties")
        }
        
        val original = chain.request()
        
        val request = original.newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .build()
        
        return chain.proceed(request)
    }
}

val networkModule = module {
    
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    single {
        HttpLoggingInterceptor { message ->
            Timber.d(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    single {
        ApiKeyInterceptor(get<String>())
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(get<ApiKeyInterceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(get<OkHttpClient>())
            .addConverterFactory(get<Json>().asConverterFactory(contentType))
            .build()
    }

    single {
        get<Retrofit>().create(DeepSeekApi::class.java)
    }
}
