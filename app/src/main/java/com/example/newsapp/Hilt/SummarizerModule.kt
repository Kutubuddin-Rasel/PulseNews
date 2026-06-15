package com.example.newsapp.Hilt

import com.example.newsapp.data.util.nlp.LocalSummarizer
import com.example.newsapp.data.util.nlp.TextRankSummarizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * W2 (DIP): consumers depend on the [LocalSummarizer] abstraction; the concrete TextRank
 * engine is constructed once here. `@Provides` (not `@Binds`) so we can build it with its
 * default NLP collaborators without forcing `@Inject` constructors onto each one. `@Singleton`
 * means the preprocessor / similarity / scorer / heuristic engine are not rebuilt per call.
 */
@Module
@InstallIn(SingletonComponent::class)
object SummarizerModule {

    @Provides
    @Singleton
    fun provideLocalSummarizer(): LocalSummarizer = TextRankSummarizer()
}
