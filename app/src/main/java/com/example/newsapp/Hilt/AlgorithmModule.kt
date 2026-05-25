package com.example.newsapp.Hilt

import com.example.newsapp.domain.util.AhoCorasickEngine
import com.example.newsapp.domain.util.KeywordMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.example.newsapp.domain.util.DynamicKeywordMatcher
import com.example.newsapp.data.repository.TaxonomyRepository

@Module
@InstallIn(SingletonComponent::class)
object AlgorithmModule {

    @Provides
    @Singleton
    fun provideKeywordMatcher(taxonomyRepository: TaxonomyRepository): KeywordMatcher {
        return DynamicKeywordMatcher(taxonomyRepository)
    }
}
