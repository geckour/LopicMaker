package com.geckour.lopicmaker.data.dao

import androidx.room.*
import com.geckour.lopicmaker.data.DB
import com.geckour.lopicmaker.data.model.Problem

@Dao
interface ProblemDao {
    @Insert
    suspend fun insert(problem: Problem): Long

    @Update
    suspend fun update(problem: Problem): Int

    @Delete
    suspend fun delete(problem: Problem): Int

    @Query("select * from problem where id = :id")
    suspend fun get(id: Long): Problem?

    @Query("select * from problem")
    suspend fun getAll(): List<Problem>
}

suspend fun Problem.upsert(db: DB): Long =
    db.problemDao().get(id)?.let {
        db.problemDao().update(it.copy(id = it.id))
        it.id
    } ?: db.problemDao().insert(this)