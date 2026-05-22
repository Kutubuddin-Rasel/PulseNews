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

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {
    @Provides
    @Singleton
    fun provideTelemetry(): AppTelemetry = AppTelemetry()

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
    fun providesOkHttpClient(requestTracingInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(requestTracingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun providesRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://pulsenews-backend.onrender.com/")
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
