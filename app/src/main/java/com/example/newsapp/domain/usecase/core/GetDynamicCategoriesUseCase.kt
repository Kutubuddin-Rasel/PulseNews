package com.example.newsapp.domain.usecase.core

import com.example.newsapp.domain.model.CategoryKey
import com.example.newsapp.domain.repository.TaxonomyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetDynamicCategoriesUseCase @Inject constructor(
    private val taxonomyRepository: TaxonomyRepository
) {
    operator fun invoke(): Flow<List<Pair<CategoryKey, String>>> {
        return taxonomyRepository.dictionaryFlow.map { dict ->
            val list = mutableListOf(CategoryKey.FOR_YOU to "For You")
            dict.keys.forEach { key ->
                val displayName = key.value.replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
                }
                list.add(key to displayName)
            }
            list
        }
    }
}
