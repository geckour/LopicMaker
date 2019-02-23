package com.geckour.lopicmaker.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.room.TypeConverter
import com.geckour.lopicmaker.App
import com.geckour.lopicmaker.data.model.Problem
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.sql.Timestamp

class RoomConverters {
    @TypeConverter
    fun fromBitmap(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    @TypeConverter
    fun toBitmap(base64String: String): Bitmap {
        val byteArray = Base64.decode(base64String, 0)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    @TypeConverter
    fun fromCells(cells: List<Problem.Cell>): String =
        App.gson.toJson(cells)

    @TypeConverter
    fun toCells(json: String): List<Problem.Cell> {
        val type = object : TypeToken<List<Problem.Cell>>() {}.type
        return App.gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromCluster(cluster: List<List<Int>>): String =
        App.gson.toJson(cluster)

    @TypeConverter
    fun toCluster(json: String): List<List<Int>> {
        val type = object : TypeToken<List<List<Int>>>() {}.type
        return App.gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromSource(source: Problem.Source): Int =
        source.ordinal

    @TypeConverter
    fun toSource(ordinal: Int): Problem.Source =
        Problem.Source.values()[ordinal]

    @TypeConverter
    fun fromTimestamp(timestamp: Timestamp): Long =
        timestamp.time

    @TypeConverter
    fun toTimestamp(time: Long): Timestamp =
        Timestamp(time)

    @TypeConverter
    fun fromTags(tags: List<String>): String =
        App.gson.toJson(tags)

    @TypeConverter
    fun toTags(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return App.gson.fromJson(json, type)
    }
}