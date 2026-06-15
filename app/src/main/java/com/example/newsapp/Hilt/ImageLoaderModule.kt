package com.example.newsapp.Hilt

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * IMG1: a single tuned Coil [ImageLoader] for the whole app, surfaced through
 * [com.example.newsapp.NewsApplication]'s `ImageLoaderFactory`. Replaces Coil's untuned default
 * (which lets it spin up its own OkHttpClient and uses ARGB_8888 everywhere).
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        apiOkHttpClient: OkHttpClient
    ): ImageLoader {
        // Reuse the app's OkHttp connection pool + dispatcher (sockets/threads) via newBuilder(),
        // but strip the Firebase-auth and request-tracing interceptors and the API HTTP cache:
        // image hosts are third-party CDNs that must never receive our ID token, and Coil manages
        // its own image disk cache below (a separate OkHttp cache would just double-store bytes).
        val imageHttpClient = apiOkHttpClient.newBuilder()
            .apply {
                interceptors().clear()
                networkInterceptors().clear()
            }
            .cache(null)
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(imageHttpClient)
            // IMG2: cap in-memory bitmaps to 25% of app heap — enough to keep the visible feed warm
            // without pressuring the 846 MB target device.
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            // Dedicated 50 MB image disk cache, separate from the 10 MB API http_cache.
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            // RGB_565 halves bitmap memory vs ARGB_8888 for opaque news thumbnails.
            .allowRgb565(true)
            .crossfade(true)
            .build()
    }
}
