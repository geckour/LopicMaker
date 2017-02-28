package jp.co.seesaa.geckour.picrossmaker.model

import android.graphics.Point

data class Cell(val coordinate: Point = Point(0, 0),
                var state: Boolean? = false) {

    var str = ""

    fun getState(): Boolean {
        return state ?: false
    }

    fun getStateNullable(): Boolean? {
        return state
    }

    companion object {
        data class Catalog(val cells: List<Cell>)
    }
}