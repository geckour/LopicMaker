package com.geckour.lopicmaker.util

import android.graphics.*
import android.util.Size
import android.widget.ImageView
import com.geckour.lopicmaker.Constant.GRID_UNIT
import com.geckour.lopicmaker.data.model.Problem

open class CanvasUtil(val size: Point) {
    val cells: MutableList<Problem.Cell> = mutableListOf()
    val prevCells: MutableList<Problem.Cell> = mutableListOf()
    private val sizeBlankArea = Point(0, 0)

    init {
        val blankArea = getNumBlankArea()
        sizeBlankArea.set(blankArea, blankArea)
        initCells()
    }

    private fun initCells() {
        initCells(this.cells)
    }

    private fun initCells(cells: MutableList<Problem.Cell>) {
        cells.clear()
        for (i in 0 until size.y) (0 until size.x).mapTo(cells) {
            Problem.Cell(
                Point(
                    it,
                    i
                )
            )
        }
    }

    fun setCells(cells: List<Problem.Cell>): Bitmap? {
        this.cells.clear()
        this.cells.addAll(cells)
        var bitmap = createCanvasImage()
        this.cells
            .filter { it.state == Problem.Cell.State.Fill }
            .forEach { bitmap = onEditCanvasImage(bitmap, it, true) }

        return bitmap
    }

    fun overrideKeysFromCells(cells: List<Problem.Cell>): Bitmap? = clearExceptKeys(setCells(cells))

    private fun clearExceptKeys(image: Bitmap?): Bitmap? {
        var bitmap: Bitmap? = image
        cells.forEach {
            it.state = Problem.Cell.State.Blank
            bitmap = onEditCanvasImage(bitmap, it, false)
        }

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
                    + pointDIff.y.toDouble() * pointDIff.y.toDouble()
        ).toFloat()
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

        val bitmap = Bitmap.createBitmap(
            GRID_UNIT * (sizeBlankArea.x + size.x),
            GRID_UNIT * (sizeBlankArea.y + size.y),
            Bitmap.Config.ARGB_8888
        )
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

        val blankWidth = sizeBlankArea.x * GRID_UNIT.toFloat()
        val blankHeight = sizeBlankArea.y * GRID_UNIT.toFloat()

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(255, 96, 96, 96)
        path = Path()
        path.addRect(0f, 0f, blankWidth, blankHeight, Path.Direction.CW)
        canvas.drawPath(path, paint)

        paint.color = Color.BLACK
        paint.textSize = GRID_UNIT.toFloat()
        val bounds = Rect()
        paint.getTextBounds("0", 0, "0".length, bounds)
        val textBaseHeight = blankHeight - GRID_UNIT / 2 - ((paint.descent() + paint.ascent()) / 2)
        for (i in 0 until size.x) {
            canvas.drawText("0", blankWidth + (i + 0.5f) * GRID_UNIT - bounds.exactCenterX(), textBaseHeight, paint)
        }
        val textBaseWidth = blankWidth - 0.5f * GRID_UNIT - bounds.exactCenterX()
        for (i in 1..size.y) {
            canvas.drawText(
                "0",
                textBaseWidth,
                blankHeight + (i - 0.5f) * GRID_UNIT - ((paint.descent() + paint.ascent()) / 2),
                paint
            )
        }

        return bitmap
    }

    fun onEditCanvasImage(image: Bitmap?, cell: Problem.Cell, refreshKeys: Boolean): Bitmap? {
        if (image == null || size.x < 1 || size.y < 1) return null

        val canvas = Canvas(image)
        val paint = Paint()
        val path = Path()
        paint.color = when (cell.state) {
            Problem.Cell.State.Fill, Problem.Cell.State.MarkNotFill -> Color.BLACK
            else -> Color.WHITE
        }
        when (cell.state) {
            Problem.Cell.State.Fill, Problem.Cell.State.Blank -> {
                val left = (sizeBlankArea.x + cell.coordinate.x) * GRID_UNIT.toFloat() + 1f
                val top = (sizeBlankArea.y + cell.coordinate.y) * GRID_UNIT.toFloat() + 1f
                val rect = RectF(left, top, left + GRID_UNIT.toFloat() - 2f, top + GRID_UNIT.toFloat() - 2f)

                paint.style = Paint.Style.FILL
                path.addRect(rect, Path.Direction.CW)
            }
            else -> {
                val left = (sizeBlankArea.x + cell.coordinate.x + 0.1f) * GRID_UNIT.toFloat() + 1f
                val top = (sizeBlankArea.y + cell.coordinate.y + 0.1f) * GRID_UNIT.toFloat() + 1f
                val rect =
                    RectF(left, top, left + GRID_UNIT.toFloat() * 0.8f - 2f, top + GRID_UNIT.toFloat() * 0.8f - 2f)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                path.moveTo(rect.left, rect.top)
                path.lineTo(rect.right, rect.bottom)
                path.moveTo(rect.right, rect.top)
                path.lineTo(rect.left, rect.bottom)
            }
        }
        canvas.drawPath(path, paint)

        return if (refreshKeys) refreshKeys(image, cell) else image
    }

    private fun refreshCanvasSize(image: Bitmap, keysLengths: Size): Bitmap? {
        if (keysLengths.width > sizeBlankArea.x || keysLengths.height > sizeBlankArea.y) {

            sizeBlankArea.set(
                if (keysLengths.width > sizeBlankArea.x) keysLengths.width else sizeBlankArea.x,
                if (keysLengths.height > sizeBlankArea.y) keysLengths.height else sizeBlankArea.y
            )
            var bitmap = createCanvasImage() ?: return image

            this.cells
                .filter { it.state == Problem.Cell.State.Fill }
                .forEach { bitmap = onEditCanvasImage(bitmap, it, true) ?: return null }

            return bitmap
        }

        return null
    }

    private fun refreshKeysInLine(image: Bitmap): Bitmap {
        var bitmap = image
        val end = Math.max(size.x, size.y) - 1
        for (i in 0..end) {
            val cell = getCellByCoordinate(Point(i % size.x, i % size.y)) ?: return image
            bitmap = refreshKeys(image, cell)
        }

        return bitmap
    }

    private fun refreshKeys(image: Bitmap, cell: Problem.Cell): Bitmap {
        val row = getCellsInRow(cell.coordinate.y) ?: return image
        val column = getCellsInColumn(cell.coordinate.x) ?: return image
        val keysRow = getKeys(row)
        val keysColumn = getKeys(column)

        var bitmap = refreshCanvasSize(image, Size(keysRow.size, keysColumn.size))
        bitmap = if (bitmap != null) {
            refreshKeysInLine(bitmap)
        } else {
            image
        }
        val canvas = Canvas(bitmap)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.textSize = GRID_UNIT.toFloat()

        val bounds = Rect()
        val textBaseHeight =
            (sizeBlankArea.y + cell.coordinate.y + 0.5f) * GRID_UNIT - ((paint.descent() + paint.ascent()) / 2)
        val initialWidth = (sizeBlankArea.x - keysRow.size + 0.5f) * GRID_UNIT
        paint.color = Color.WHITE
        for (i in 0 until sizeBlankArea.x) {
            val path = Path()
            val left = i * GRID_UNIT + 1f
            val top = (sizeBlankArea.y + cell.coordinate.y) * GRID_UNIT + 1f
            val rect = RectF(left, top, left + GRID_UNIT - 2f, top + GRID_UNIT - 2f)
            path.addRect(rect, Path.Direction.CW)
            canvas.drawPath(path, paint)
        }
        paint.color = Color.BLACK
        for ((index, value) in keysRow.withIndex()) {
            paint.getTextBounds(value.toString(), 0, value.toString().length, bounds)
            canvas.drawText(
                value.toString(),
                initialWidth - bounds.exactCenterX() + index * GRID_UNIT,
                textBaseHeight,
                paint
            )
        }

        val textBaseWidth = (sizeBlankArea.x + cell.coordinate.x + 0.5f) * GRID_UNIT
        val initialHeight =
            (sizeBlankArea.y - keysColumn.size + 0.5f) * GRID_UNIT - ((paint.descent() + paint.ascent()) / 2)
        paint.color = Color.WHITE
        for (i in 0 until sizeBlankArea.y) {
            val path = Path()
            val left = (sizeBlankArea.x + cell.coordinate.x) * GRID_UNIT + 1f
            val top = i * GRID_UNIT + 1f
            val rect = RectF(left, top, left + GRID_UNIT - 2f, top + GRID_UNIT - 2f)
            path.addRect(rect, Path.Direction.CW)
            canvas.drawPath(path, paint)
        }
        paint.color = Color.BLACK
        for ((index, value) in keysColumn.withIndex()) {
            paint.getTextBounds(value.toString(), 0, value.toString().length, bounds)
            canvas.drawText(
                value.toString(),
                textBaseWidth - bounds.exactCenterX(),
                initialHeight + index * GRID_UNIT,
                paint
            )
        }

        return bitmap
    }

    fun getKeys(cells: List<Problem.Cell>): List<Int> {
        val keys: ArrayList<Int> = arrayListOf(0)
        cells.forEachIndexed { i, cell ->
            if ((keys.size > 1 || keys.last() != 0) && cells.getOrNull(i - 1)?.state != Problem.Cell.State.Fill && cell.state == Problem.Cell.State.Fill) keys.add(
                0
            ) // □→■になった時にキー追加
            if (cell.state == Problem.Cell.State.Fill) {
                keys[keys.lastIndex] += 1
            }
        }

        if (keys.size < 1) keys.add(0)

        return keys
    }

    fun getCellByCoordinate(coordinate: Point): Problem.Cell? =
        cells.firstOrNull { it?.coordinate?.equals(coordinate.x, coordinate.y) == true }

    fun getCellIndexByCoordinate(coordinate: Point): Int =
        if (coordinate.x in 0 until size.x && coordinate.y in 0 until size.y) coordinate.x + coordinate.y * size.x + 1 else -1

    fun getCellsInColumn(index: Int): List<Problem.Cell>? {
        if (index < 0 || size.x < index) return null
        val cellsInColumn: ArrayList<Problem.Cell> = ArrayList()
        for (i in 0 until size.y) {
            val cell = getCellByCoordinate(Point(index, i)) ?: return null
            cellsInColumn.add(cell)
        }

        return cellsInColumn
    }

    fun getCellsInRow(index: Int): List<Problem.Cell>? {
        if (index < 0 || size.y < index) return null
        val cellsInRow: ArrayList<Problem.Cell> = ArrayList()
        for (i in 0 until size.x) {
            val cell = getCellByCoordinate(Point(i, index)) ?: return null
            cellsInRow.add(cell)
        }

        return cellsInRow
    }

    fun getThumbnailImage(cells: List<Problem.Cell>? = null): Bitmap {
        val cs = cells ?: this.cells
        val u = 5f
        val image = Bitmap.createBitmap(size.x * u.toInt(), size.y * u.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        val paint = Paint()
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        for (c in cs) {
            if (c.state == Problem.Cell.State.Fill) {
                val path = Path()
                val left = c.coordinate.x * u
                val top = c.coordinate.y * u
                path.addRect(left, top, left + u, top + u, Path.Direction.CW)
                path.close()
                canvas.drawPath(path, paint)
            }
        }

        return image
    }
}