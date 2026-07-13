package com.example.newsapp.Room

import androidx.room.TypeConverter
import com.example.newsapp.data.remote.VerificationStatusAdapter
import com.example.newsapp.domain.model.Provenance
import com.example.newsapp.module.Source
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class TypeConverter {
    // Self-contained Moshi (Room instantiates this converter via its no-arg constructor). The
    // VerificationStatus adapter lets the sealed status inside Provenance round-trip as a string.
    private val moshi = Moshi.Builder()
        .add(VerificationStatusAdapter())
        .build()

    private val sourceAdapter = moshi.adapter(Source::class.java)
    private val provenanceAdapter = moshi.adapter(Provenance::class.java)
    private val stringListAdapter =
        moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))

    @TypeConverter
    fun fromSource(source: Source): String = sourceAdapter.toJson(source)

    @TypeConverter
    fun toSource(sourceJson: String): Source = sourceAdapter.fromJson(sourceJson)!!

    @TypeConverter
    fun fromProvenance(provenance: Provenance?): String? = provenance?.let { provenanceAdapter.toJson(it) }

    @TypeConverter
    fun toProvenance(provenanceJson: String?): Provenance? = provenanceJson?.let { provenanceAdapter.fromJson(it) }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? = list?.let { stringListAdapter.toJson(it) }

    @TypeConverter
    fun toStringList(json: String?): List<String>? = json?.let { stringListAdapter.fromJson(it) }
}
