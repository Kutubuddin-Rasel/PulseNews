package com.example.newsapp.Hilt

import com.example.newsapp.BuildConfig
import com.example.newsapp.domain.util.AppTelemetry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.squareup.moshi.Moshi
import com.example.newsapp.data.remote.TelemetryEventAdapter
import com.example.newsapp.data.remote.VerificationStatusAdapter

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {
    // AppTelemetry is now provided via constructor injection on AppTelemetryImpl and bound in ManagerModule.

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        // Custom adapters first; generated @JsonClass adapters are discovered reflection-free.
        .add(VerificationStatusAdapter())
        .add(TelemetryEventAdapter())
        .build()

    @Provides
    @Singleton
    fun provideRequestTracingInterceptor(telemetry: AppTelemetry): Interceptor {
        return Interceptor { chain ->
            val requestId = telemetry.requestId()
            val request = chain.request().newBuilder()
                .header("X-Request-ID", requestId)
                .build()
            // A5: only trace per-request method/URL in debug — in release this is Logcat noise +
            // a privacy leak (URLs carry article ids / query params) on every single call.
            if (BuildConfig.DEBUG) {
                telemetry.info("Network", "[$requestId] ${request.method} ${request.url}")
            }
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        @ApplicationContext context: Context,
        requestTracingInterceptor: Interceptor,
        firebaseTokenInterceptor: FirebaseTokenInterceptor
    ): OkHttpClient {
        // Backend is now Azure + Cloudflare (warm), so timeouts are tuned for fast failure on a
        // dead/flaky network rather than the old 60s "Render free-tier cold-start" rationale (A3).
        // A 10 MB disk cache lets the client honour backend Cache-Control/ETag, enabling
        // conditional GETs (304s) and instant back-navigation on public endpoints (A2).
        val cache = Cache(File(context.cacheDir, "http_cache"), 10L * 1024 * 1024)
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(requestTracingInterceptor)
            .addInterceptor(firebaseTokenInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun providesRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://pulsenewsbackend.me/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun providePulseApi(retrofit: Retrofit): com.example.newsapp.Api.PulseBackendApi {
        return retrofit.create(com.example.newsapp.Api.PulseBackendApi::class.java)
    }
}
