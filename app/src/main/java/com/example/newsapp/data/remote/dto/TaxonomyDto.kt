package com.example.newsapp.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class TaxonomyDto(
    @SerializedName("version")
    val version: String,
    
    @SerializedName("categories")
    val categories: Map<String, List<String>>
)
