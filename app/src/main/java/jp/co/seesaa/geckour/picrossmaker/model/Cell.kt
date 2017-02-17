package jp.co.seesaa.geckour.picrossmaker.model

import android.graphics.Point

class Cell(val coordinate: Point = Point(0, 0)) {
    var state = false
    var str = ""
}