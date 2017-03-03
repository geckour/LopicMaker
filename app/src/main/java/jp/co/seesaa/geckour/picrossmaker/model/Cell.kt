package jp.co.seesaa.geckour.picrossmaker.model

import android.graphics.Point
import android.os.Parcel
import android.os.Parcelable
import android.util.Size

data class Cell(val coordinate: Point = Point(0, 0),
                private var state: Boolean? = false): Parcelable {

    var str = ""

    constructor(parcel: Parcel?): this() {
        val size = parcel?.readSize() ?: Size(0, 0)
        this.coordinate.set(size.width, size.height)
        this.state = parcel?.readInt() ?: 0 > 0
        this.str = parcel?.readString() ?: ""
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel?, flags: Int) {
        parcel?.writeSize(Size(coordinate.x, coordinate.y))
        parcel?.writeInt(if (getState()) 1 else 0)
        parcel?.writeString(str)
    }

    fun getState(): Boolean {
        return state ?: false
    }

    fun getStateNullable(): Boolean? {
        return state
    }

    fun setState(state: Boolean?) {
        this.state = state
    }

    companion object {
        val CREATOR = object: Parcelable.Creator<Cell> {
            override fun createFromParcel(parcel: Parcel?) = Cell(parcel)

            override fun newArray(size: Int): Array<out Cell> = Array(size, { i -> Cell() })
        }

        data class Catalog(val cells: List<Cell>)
    }
}