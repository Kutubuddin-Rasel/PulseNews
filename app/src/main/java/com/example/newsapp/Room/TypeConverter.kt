package com.example.newsapp.Room

import androidx.room.TypeConverter
import com.example.newsapp.module.Source
import com.google.gson.Gson

class TypeConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromSource(source: Source): String {
        return gson.toJson(source)
    }

    @TypeConverter
    fun toSource(sourceJson: String): Source {
        return gson.fromJson(sourceJson, Source::class.java)
    }

    @TypeConverter
    fun fromProvenance(provenance: com.example.newsapp.domain.model.Provenance?): String? {
        return provenance?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toProvenance(provenanceJson: String?): com.example.newsapp.domain.model.Provenance? {
        return provenanceJson?.let { gson.fromJson(it, com.example.newsapp.domain.model.Provenance::class.java) }
    }
}
