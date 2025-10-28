package com.example.cinephile.data.local.converters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType

class ListConverters {
    
    private val moshi = Moshi.Builder().build()
    
    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        if (value == null) return null
        val type: ParameterizedType = Types.newParameterizedType(List::class.java, Long::class.java)
        val adapter = moshi.adapter<List<Long>>(type)
        return adapter.toJson(value)
    }
    
    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        if (value == null) return null
        val type: ParameterizedType = Types.newParameterizedType(List::class.java, Long::class.java)
        val adapter = moshi.adapter<List<Long>>(type)
        return adapter.fromJson(value)
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        val type: ParameterizedType = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val type: ParameterizedType = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(value)
    }
    
    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        if (value == null) return null
        val type: ParameterizedType = Types.newParameterizedType(List::class.java, Int::class.java)
        val adapter = moshi.adapter<List<Int>>(type)
        return adapter.toJson(value)
    }
    
    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        if (value == null) return null
        val type: ParameterizedType = Types.newParameterizedType(List::class.java, Int::class.java)
        val adapter = moshi.adapter<List<Int>>(type)
        return adapter.fromJson(value)
    }
}
