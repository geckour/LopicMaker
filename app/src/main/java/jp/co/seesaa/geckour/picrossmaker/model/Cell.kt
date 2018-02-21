package jp.co.seesaa.geckour.picrossmaker.model

import android.graphics.Point

data class Cell(
        val coordinate: Point = Point(0, 0),
        private var state: State = State.Blank
) {
    enum class State {
        Fill,
        Blank,
        MarkNotFill
    }

    fun getState(): State = state

    fun setState(state: State) {
        this.state = state
    }

    data class Catalog(val cells: List<Cell>)
}