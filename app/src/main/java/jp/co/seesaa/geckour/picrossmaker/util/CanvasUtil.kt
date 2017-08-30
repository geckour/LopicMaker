package jp.co.seesaa.geckour.picrossmaker.util

import android.graphics.*
import android.util.Size
import android.widget.ImageView
import jp.co.seesaa.geckour.picrossmaker.Constant.unit
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import java.util.*

open class CanvasUtil(val size: Point) {
    val cells: ArrayList<Cell> = ArrayList()
    private val sizeBlankArea = Point(0, 0)

    init {
        val blankArea = getNumBlankArea()
        sizeBlankArea.set(blankArea, blankArea)
        initCells()
    }

    companion object {
        val BUNDLE_NAME_CELLS = "cells"
    }

    private fun initCells() {
        initCells(this.cells)
    }

    private fun initCells(cells: ArrayList<Cell>) {
        cells.clear()
        for (i in 0 until size.y) (0 until size.x).mapTo(cells) { Cell(Point(it, i)) }
    }

    fun setCells(cells: List<Cell>): Bitmap? {
        this.cells.clear()
        this.cells.addAll(cells)
        var bitmap = createCanvasImage()
        this.cells
                .filter { it.getState() }
                .forEach { bitmap = onEditCanvasImage(bitmap, it, true) }

        return bitmap
    }

    private fun getNumBlankArea(): Int {
        val l = Math.max(size.x, size.y)
        return when {
            l in 1..2 -> 1
            l in 3..4 -> 2
            l > 4 -> 3
            else -> 0
        }
    }

    fun getPointDiff(p1: PointF, p2: PointF): PointF = PointF(p2.x - p1.x, p2.y - p1.y)

    fun getScale(diff1: Float, diff2: Float): Float = if (diff1 == 0f) 1f else diff2 / diff1

    fun getPointMid(p1: PointF, p2: PointF): PointF = PointF((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)

    fun getPointDistance(p1: PointF, p2: PointF): Float {
        val pointDIff = getPointDiff(p1, p2)
        return Math.sqrt(
                pointDIff.x.toDouble() * pointDIff.x.toDouble()
                + pointDIff.y.toDouble() * pointDIff.y.toDouble()).toFloat()
    }

    fun getCoordinateFromTouchPoint(canvas: ImageView?, pointer: PointF): Point? {
        if (canvas == null || size.x < 1 || size.y < 1) return null


        val unitX = canvas.width.toFloat() / (size.x + sizeBlankArea.x)
        val unitY = canvas.height.toFloat() / (size.y + sizeBlankArea.y)
        if (Math.abs(unitX - unitY) > 0.1f) return null

        val cellX = (pointer.x / unitX).toInt() - sizeBlankArea.x
        val cellY = (pointer.y / unitY).toInt() - sizeBlankArea.y
        if (cellX < 0 || size.x < cellX || cellY < 0 || size.y < cellY) return null

        return Point(cellX, cellY)
    }

    fun createCanvasImage(): Bitmap? {
        if (size.x < 1 || size.y < 1) return null

        val bitmap = Bitmap.createBitmap(unit * (sizeBlankArea.x + size.x), unit * (sizeBlankArea.y + size.y), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        var path: Path

        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawPaint(paint)

        val lineColor = Color.argb(255, 128, 128, 128)
        paint.color = lineColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        for (i in 1 until (sizeBlankArea.y + size.y)) {
            path = Path()
            if (i == sizeBlankArea.y) paint.color = Color.BLACK

            val height = canvas.height.toFloat() * i / (sizeBlankArea.y + size.y)
            path.moveTo(0f, height)
            path.lineTo(canvas.width.toFloat(), height)
            path.close()

            canvas.drawPath(path, paint)

            paint.color = lineColor
        }
        for (i in 1 until (sizeBlankArea.x + size.x)) {
            path = Path()
            if (i == sizeBlankArea.x) paint.color = Color.BLACK

            val width = canvas.width.toFloat() * i / (sizeBlankArea.x + size.x)
            path.moveTo(width, 0f)
            path.lineTo(width, canvas.height.toFloat())
            path.close()

            canvas.drawPath(path, paint)

            paint.color = lineColor
        }

        val blankWidth = sizeBlankArea.x * unit.toFloat()
        val blankHeight = sizeBlankArea.y * unit.toFloat()

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(255, 96, 96, 96)
        path = Path()
        path.addRect(0f, 0f, blankWidth, blankHeight, Path.Direction.CW)
        canvas.drawPath(path, paint)

        paint.color = Color.BLACK
        paint.textSize = unit.toFloat()
        val bounds = Rect()
        paint.getTextBounds("0", 0, "0".length, bounds)
        val textBaseHeight = blankHeight - unit / 2 - ((paint.descent() + paint.ascent()) / 2)
        for (i in 0 until size.x) {
            canvas.drawText("0", blankWidth + (i + 0.5f) * unit - bounds.exactCenterX(), textBaseHeight, paint)
        }
        val textBaseWidth = blankWidth - 0.5f * unit - bounds.exactCenterX()
        for (i in 1..size.y) {
            canvas.drawText("0", textBaseWidth, blankHeight + (i - 0.5f) * unit - ((paint.descent() + paint.ascent()) / 2), paint)
        }

        return bitmap
    }

    fun onEditCanvasImage(image: Bitmap?, cell: Cell, refreshKeys: Boolean): Bitmap? {
        if (image == null || size.x < 1 || size.y < 1) return null

        val canvas = Canvas(image)
        val paint = Paint()
        val path = Path()

        paint.style = Paint.Style.FILL
        paint.color = if (cell.getState()) Color.BLACK else Color.WHITE
        val left = (sizeBlankArea.x + cell.coordinate.x) * unit.toFloat() + 1f
        val top = (sizeBlankArea.y + cell.coordinate.y) * unit.toFloat() + 1f
        val rect = RectF(left, top, left + unit.toFloat() - 2f, top + unit.toFloat() - 2f)
        path.addRect(rect, Path.Direction.CW)
        canvas.drawPath(path, paint)

        return if (refreshKeys) refreshKeys(image, cell) else image
    }

    private fun refreshCanvasSize(image: Bitmap, keysLengths: Size): Bitmap? {
        if (keysLengths.width > sizeBlankArea.x || keysLengths.height > sizeBlankArea.y) {

            sizeBlankArea.set(if (keysLengths.width > sizeBlankArea.x) keysLengths.width else sizeBlankArea.x,
                    if (keysLengths.height > sizeBlankArea.y) keysLengths.height else sizeBlankArea.y)
            var bitmap = createCanvasImage() ?: return image

            this.cells
                    .filter(Cell::getState)
                    .forEach { bitmap = onEditCanvasImage(bitmap, it, true) ?: return null }

            return bitmap
        }

        return null
    }

    private fun refreshAllKeys(image: Bitmap): Bitmap {
        var bitmap = image
        val end = Math.max(size.x, size.y) - 1
        for (i in 0..end) {
            val cell = getCellByCoordinate(Point(i % size.x, i % size.y)) ?: return image
            bitmap = refreshKeys(image, cell)
        }

        return bitmap
    }

    private fun refreshKeys(image: Bitmap, cell: Cell): Bitmap {
        val row = getCellsInRow(cell.coordinate.y) ?: return image
        val column = getCellsInColumn(cell.coordinate.x) ?: return image
        val keysRow = getKeys(row)
        val keysColumn = getKeys(column)

        var bitmap = refreshCanvasSize(image, Size(keysRow.size, keysColumn.size))
        bitmap = if (bitmap != null) {
            refreshAllKeys(bitmap)
        } else {
            image
        }
        val canvas = Canvas(bitmap)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.textSize = unit.toFloat()

        val bounds = Rect()
        val textBaseHeight = (sizeBlankArea.y + cell.coordinate.y + 0.5f) * unit - ((paint.descent() + paint.ascent()) / 2)
        val initialWidth = (sizeBlankArea.x - keysRow.size + 0.5f) * unit
        paint.color = Color.WHITE
        for (i in 0 until sizeBlankArea.x) {
            val path = Path()
            val left = i * unit + 1f
            val top = (sizeBlankArea.y + cell.coordinate.y) * unit + 1f
            val rect = RectF(left, top, left + unit - 2f, top + unit - 2f)
            path.addRect(rect, Path.Direction.CW)
            canvas.drawPath(path, paint)
        }
        paint.color = Color.BLACK
        for ((index, value) in keysRow.withIndex()) {
            paint.getTextBounds(value.toString(), 0, value.toString().length, bounds)
            canvas.drawText(value.toString(), initialWidth - bounds.exactCenterX() + index * unit, textBaseHeight, paint)
        }

        val textBaseWidth = (sizeBlankArea.x + cell.coordinate.x + 0.5f) * unit
        val initialHeight = (sizeBlankArea.y - keysColumn.size + 0.5f) * unit - ((paint.descent() + paint.ascent()) / 2)
        paint.color = Color.WHITE
        for (i in 0 until sizeBlankArea.y) {
            val path = Path()
            val left = (sizeBlankArea.x + cell.coordinate.x) * unit + 1f
            val top = i * unit + 1f
            val rect = RectF(left, top, left + unit - 2f, top + unit - 2f)
            path.addRect(rect, Path.Direction.CW)
            canvas.drawPath(path, paint)
        }
        paint.color = Color.BLACK
        for ((index, value) in keysColumn.withIndex()) {
            paint.getTextBounds(value.toString(), 0, value.toString().length, bounds)
            canvas.drawText(value.toString(), textBaseWidth - bounds.exactCenterX(), initialHeight + index * unit, paint)
        }

        return bitmap
    }

    fun getKeys(cells: List<Cell>): List<Int> {
        val keys: ArrayList<Int> = ArrayList()
        var stateBefore: Boolean? = null
        for (cell in cells) {
            if (stateBefore != null && !stateBefore && cell.getState()) keys.add(0)
            if (cell.getState()) {
                if (keys.size < 1) keys.add(0)
                keys[keys.lastIndex] += 1
            }

            stateBefore = cell.getState()
        }

        if (keys.size < 1) keys.add(0)

        return keys
    }

    fun getCellByCoordinate(coordinate: Point): Cell? =
            cells.firstOrNull { it.coordinate.equals(coordinate.x, coordinate.y) }

    fun getCellIndexByCoordinate(coordinate: Point): Int = //return cells.indexOfFirst { it.coordinate.equals(coordinate.x, coordinate.y) }
            if (-1 < coordinate.x && coordinate.x < size.x && -1 < coordinate.y && coordinate.y < size.y) coordinate.x + coordinate.y * size.x else -1

    fun getCellsInColumn(index: Int): List<Cell>? {
        if (index < 0 || size.x < index) return null
        val cellsInRow: ArrayList<Cell> = ArrayList()
        for (i in 0 until size.y) {
            val cell = getCellByCoordinate(Point(index, i)) ?: return null
            cellsInRow.add(cell)
        }

        return cellsInRow
    }

    fun getCellsInRow(index: Int): List<Cell>? {
        if (index < 0 || size.y < index) return null
        val cellsInColumn: ArrayList<Cell> = ArrayList()
        for (i in 0 until size.x) {
            val cell = getCellByCoordinate(Point(i, index)) ?: return null
            cellsInColumn.add(cell)
        }

        return cellsInColumn
    }

    fun getThumbnailImage(): Bitmap {
        val u = 5f
        val image = Bitmap.createBitmap(size.x * u.toInt(), size.y * u.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        val paint = Paint()
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        for (cell in cells) {
            if (cell.getState()) {
                val path = Path()
                val left = cell.coordinate.x * u
                val top = cell.coordinate.y * u
                path.addRect(left, top, left + u, top + u, Path.Direction.CW)
                path.close()
                canvas.drawPath(path, paint)
            }
        }

        return image
    }
}