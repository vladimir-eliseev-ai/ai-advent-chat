package eliseev.aiadvent.chat.di

import eliseev.aiadvent.chat.data.api.DeepSeekApi
import eliseev.aiadvent.chat.data.api.OllamaApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.core.qualifier.named
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
        ApiKeyInterceptor(get(named("deepseekApiKey")))
    }

    // DeepSeek HTTP Client
    single(named("deepseekClient")) {
        OkHttpClient.Builder()
            .addInterceptor(get<ApiKeyInterceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Ollama HTTP Client (без API ключа)
    single(named("ollamaClient")) {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // DeepSeek Retrofit
    single(named("deepseekRetrofit")) {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(get(named("deepseekClient")))
            .addConverterFactory(get<Json>().asConverterFactory(contentType))
            .build()
    }

    // Ollama Retrofit
    single(named("ollamaRetrofit")) {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(get<String>(named("ollamaBaseUrl")))
            .client(get(named("ollamaClient")))
            .addConverterFactory(get<Json>().asConverterFactory(contentType))
            .build()
    }

    // API Instances
    single {
        get<Retrofit>(named("deepseekRetrofit")).create(DeepSeekApi::class.java)
    }

    single {
        get<Retrofit>(named("ollamaRetrofit")).create(OllamaApi::class.java)
    }
}
