package com.example.newsapp.domain.util

import com.example.newsapp.data.repository.TaxonomyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicKeywordMatcher @Inject constructor(
    private val taxonomyRepository: TaxonomyRepository
) : KeywordMatcher {

    @Volatile
    private var engine: AhoCorasickEngine? = null

    init {
        // Observe the DataStore flow on a background thread.
        // When a new dictionary arrives, we compile a new AhoCorasick engine
        // and swap the pointer. Since 'engine' is @Volatile, the swap is thread-safe.
        CoroutineScope(Dispatchers.Default).launch {
            taxonomyRepository.dictionaryFlow.collectLatest { dictionary ->
                // This builds the Trie and failure links
                val newEngine = AhoCorasickEngine(dictionary)
                // Atomic pointer swap
                engine = newEngine
            }
        }
    }

    override fun matchFrequencies(text: String): Map<String, Int> {
        return engine?.matchFrequencies(text) ?: emptyMap()
    }
}
