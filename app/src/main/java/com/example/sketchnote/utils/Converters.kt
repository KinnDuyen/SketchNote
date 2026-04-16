package com.example.sketchnote.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList()
        else Gson().fromJson(value, object : TypeToken<List<String>>() {}.type)
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return Gson().toJson(list)
    }

    // KHÔNG cần 2 method này vì:
    // Room tự động xử lý String -> String
    // @TypeConverter
    // fun fromSketchData(value: String): String = value
    //
    // @TypeConverter
    // fun toSketchData(data: String): String = data
}