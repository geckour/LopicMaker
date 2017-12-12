package jp.co.seesaa.geckour.picrossmaker.model

import android.graphics.Point

data class Cell(
        val coordinate: Point = Point(0, 0),
        private var state: Boolean? = false
) {

    fun getState(): Boolean {
        return state ?: false
    }

    fun getStateOrNull(): Boolean? {
        return state
    }

    fun setState(state: Boolean?) {
        this.state = state
    }

    data class Catalog(val cells: List<Cell>)
}