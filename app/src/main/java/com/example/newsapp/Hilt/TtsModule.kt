package com.example.newsapp.Hilt

import com.example.newsapp.domain.util.tts.CloudTtsEngine
import com.example.newsapp.domain.util.tts.NativeTtsEngine
import com.example.newsapp.domain.util.tts.TtsEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsModule {

    // Binds the CloudTtsEngine (which falls back to NativeTtsEngine) 
    // to the TtsEngine interface so ViewModels can seamlessly inject it.
    @Binds
    abstract fun bindTtsEngine(
        cloudTtsEngine: CloudTtsEngine
    ): TtsEngine
}
