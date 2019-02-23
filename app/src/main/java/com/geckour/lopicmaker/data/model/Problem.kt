package com.geckour.lopicmaker.data.model

import android.graphics.Bitmap
import android.graphics.Point
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Timestamp

@Entity
data class Problem(
    @PrimaryKey(autoGenerate = true) var id: Long,
    var title: String = "no title",
    var draft: Boolean = true,
    var tags: List<String> = listOf(),
    var keysHorizontal: List<List<Int>> = emptyList(),
    var keysVertical: List<List<Int>> = emptyList(),
    var thumb: Bitmap? = null,
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),
    var editedAt: Timestamp = Timestamp(System.currentTimeMillis()),
    var source: Source = Source.OWN,
    var cells: List<Cell> = emptyList()
) {
    enum class Source {
        OWN,
        SERVER_ORIGIN,
        SERVER_OTHER,
        OTHER
    }


    data class Cell(
        val coordinate: Point = Point(0, 0),
        var state: State = State.Blank
    ) {
        enum class State {
            Fill,
            Blank,
            MarkNotFill
        }
    }
}