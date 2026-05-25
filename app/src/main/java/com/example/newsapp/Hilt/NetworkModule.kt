package com.example.newsapp.Hilt

import com.example.newsapp.Api.NewsApi
import com.example.newsapp.data.util.AppTelemetry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.google.gson.Gson

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {
    @Provides
    @Singleton
    fun provideTelemetry(privacyPrefs: com.example.newsapp.data.repository.PrivacyPreferencesRepository): AppTelemetry = AppTelemetry(privacyPrefs)

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideRequestTracingInterceptor(telemetry: AppTelemetry): Interceptor {
        return Interceptor { chain ->
            val requestId = telemetry.requestId()
            val request = chain.request().newBuilder()
                .header("X-Request-ID", requestId)
                .build()
            telemetry.info("Network", "[$requestId] ${request.method} ${request.url}")
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        requestTracingInterceptor: Interceptor,
        firebaseTokenInterceptor: FirebaseTokenInterceptor
    ): OkHttpClient {
        // Increased to 60 seconds to accommodate Render.com free-tier backend cold starts
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(requestTracingInterceptor)
            .addInterceptor(firebaseTokenInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun providesRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://pulsenewsbackend.me/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePulseApi(retrofit: Retrofit): com.example.newsapp.Api.PulseBackendApi {
        return retrofit.create(com.example.newsapp.Api.PulseBackendApi::class.java)
    }
}
