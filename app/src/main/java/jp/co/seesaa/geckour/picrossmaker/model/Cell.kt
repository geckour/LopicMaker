package jp.co.seesaa.geckour.picrossmaker.model

import android.graphics.Point

class Cell(val coordinate: Point = Point(0, 0), var state: Boolean? = false) {
    var str = ""

    fun getState(): Boolean {
        return state ?: false
    }

    fun getStateNullable(): Boolean? {
        return state
    }
}