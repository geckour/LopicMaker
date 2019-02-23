package com.geckour.lopicmaker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.geckour.lopicmaker.data.dao.ProblemDao
import com.geckour.lopicmaker.data.model.Problem
import com.geckour.lopicmaker.util.RoomConverters

@Database(entities = [Problem::class], version = 1)
@TypeConverters(RoomConverters::class)
abstract class DB : RoomDatabase() {

    companion object {
        private const val DB_NAME = "lopic_maker.db"

        @Volatile
        private var instance: DB? = null

        fun getInstance(context: Context): DB =
            instance ?: synchronized(this) {
                Room.databaseBuilder(context, DB::class.java, DB_NAME).build().apply {
                    instance = this
                }
            }
    }

    abstract fun problemDao(): ProblemDao
}