package jp.co.seesaa.geckour.picrossmaker.util

import android.graphics.*
import android.util.Log
import android.util.Size
import android.widget.ImageView
import jp.co.seesaa.geckour.picrossmaker.Constant.Companion.unit
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import java.util.*

class Algorithm {
    companion object {
        val sizeBlankArea = Point(0, 0)
        fun getNumBlankArea(size: Size?): Int {
            if (size == null) return 0
            if (size.width > 3 || size.height > 3) return 3
            return if (size.width < size.height) size.height else size.width
        }

        fun getPointDiff(p1: PointF, p2: PointF): PointF {
            return PointF(p2.x - p1.x, p2.y - p1.y)
        }

        fun getScale(diff1: Float, diff2: Float): Float {
            return if (diff1 == 0f) 1f else diff2 / diff1
        }

        fun getPointMid(p1: PointF, p2: PointF): PointF {
            return PointF((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
        }

        fun getCoordinate(canvas: ImageView?, pointer: PointF, size: Size): Point? {
            if (canvas == null || size.width < 1 || size.height < 1) return null


            val unitX = canvas.width.toFloat() / (size.width + sizeBlankArea.x)
            val unitY = canvas.height.toFloat() / (size.height + sizeBlankArea.y)
            //if (Math.abs(unitX - unitY) > 0.1f) return null

            val cellX = (pointer.x / unitX).toInt() - sizeBlankArea.x
            val cellY = (pointer.y / unitY).toInt() - sizeBlankArea.y
            if (cellX < 0 || size.width < cellX || cellY < 0 || size.height < cellY) return null

            return Point(cellX, cellY)
        }

        fun createCanvasImage(size: Size): Bitmap? {
            if (size.width < 1 || size.height < 1) return null

            val bitmap = Bitmap.createBitmap(unit * (sizeBlankArea.x + size.width), unit * (sizeBlankArea.y + size.height), Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)
            val p = Paint()
            p.isAntiAlias = true
            var path: Path

            p.color = Color.WHITE
            p.style = Paint.Style.FILL
            c.drawPaint(p)

            val lineColor = Color.argb(255, 128, 128, 128)
            p.color = lineColor
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1f
            for (i in 1..(sizeBlankArea.y + size.height) - 1) {
                path = Path()
                if (i == sizeBlankArea.y) p.color = Color.BLACK

                val height = c.height.toFloat() * i / (sizeBlankArea.y + size.height)
                path.moveTo(0f, height)
                path.lineTo(c.width.toFloat(), height)
                path.close()

                c.drawPath(path, p)

                p.color = lineColor
            }
            for (i in 1..(sizeBlankArea.x + size.width) - 1) {
                path = Path()
                if (i == sizeBlankArea.x) p.color = Color.BLACK

                val width = c.width.toFloat() * i / (sizeBlankArea.x + size.width)
                path.moveTo(width, 0f)
                path.lineTo(width, c.height.toFloat())
                path.close()

                c.drawPath(path, p)

                p.color = lineColor
            }

            val blankWidth = sizeBlankArea.x * unit.toFloat()
            val blankHeight = sizeBlankArea.y * unit.toFloat()

            p.style = Paint.Style.FILL
            p.color = Color.argb(255, 96, 96, 96)
            path = Path()
            path.addRect(0f, 0f, blankWidth, blankHeight, Path.Direction.CW)
            c.drawPath(path, p)

            p.color = Color.BLACK
            p.textSize = unit.toFloat()
            val bounds: Rect = Rect()
            p.getTextBounds("0", 0, "0".length, bounds)
            val textBaseHeight = blankHeight - unit / 2 - ((p.descent() + p.ascent()) / 2)
            for (i in 0..size.width - 1) {
                c.drawText("0", blankWidth + (i + 0.5f) * unit - bounds.exactCenterX(), textBaseHeight, p)
            }
            val textBaseWidth = blankWidth - 0.5f * unit - bounds.exactCenterX()
            for (i in 1..size.height) {
                c.drawText("0", textBaseWidth, blankHeight + (i - 0.5f) * unit - ((p.descent() + p.ascent()) / 2), p)
            }

            return bitmap
        }

        fun onEditCanvasImage(image: Bitmap?, size: Size, cells: List<Cell>, cell: Cell, refreshHints: Boolean): Bitmap? {
            if (image == null || size.width < 1 || size.height < 1) return null

            val canvas = Canvas(image)
            val paint = Paint()
            val path = Path()

            paint.style = Paint.Style.FILL
            paint.color = if (cell.state) Color.BLACK else Color.WHITE
            val left = (sizeBlankArea.x + cell.coordinate.x) * unit.toFloat() + 1f
            val top = (sizeBlankArea.y + cell.coordinate.y) * unit.toFloat() + 1f
            val rect = RectF(left, top, left + unit.toFloat() - 2f, top + unit.toFloat() - 2f)
            path.addRect(rect, Path.Direction.CW)
            canvas.drawPath(path, paint)

            return if (refreshHints) refreshHints(image, size, cells, cell) else image
        }

        fun refreshCanvasSize(image: Bitmap, drawAreaSize: Size, hintsLengths: Size, cells: List<Cell>): Bitmap? {
            if (hintsLengths.width > sizeBlankArea.x || hintsLengths.height > sizeBlankArea.y) {

                sizeBlankArea.set(if (hintsLengths.width > sizeBlankArea.x) hintsLengths.width else sizeBlankArea.x,
                        if (hintsLengths.height > sizeBlankArea.y) hintsLengths.height else sizeBlankArea.y)
                var bitmap = createCanvasImage(drawAreaSize) ?: return image

                for (cell in cells) bitmap = onEditCanvasImage(bitmap, drawAreaSize, cells, cell, false) ?: return image

                return bitmap
            }

            return null
        }

        fun refreshAllHints(image: Bitmap, size: Size, cells: List<Cell>): Bitmap {
            var bitmap = image
            val end = Math.max(size.width, size.height) - 1
            for (i in 0..end) {
                val cell = getCellByCoordinate(cells, Point(i % size.width, i % size.height)) ?: return image
                bitmap = refreshHints(image, size, cells, cell)
            }

            return bitmap
        }

        fun refreshHints(image: Bitmap, size: Size, cells: List<Cell>, cell: Cell): Bitmap {
            val row = getCellsInRow(cells, cell.coordinate.y, size) ?: return image
            val column = getCellsInColumn(cells, cell.coordinate.x, size) ?: return image
            val hintsRow = getHints(row)
            val hintsColumn = getHints(column)

            var bitmap = refreshCanvasSize(image, size, Size(hintsRow.size, hintsColumn.size), cells)
            if (bitmap != null) {
                bitmap = refreshAllHints(bitmap, size, cells)
            } else {
                bitmap = image
            }
            val canvas = Canvas(bitmap)

            val paint = Paint()
            paint.isAntiAlias = true
            paint.style = Paint.Style.FILL
            paint.textSize = unit.toFloat()

            val bounds: Rect = Rect()
            val textBaseHeight = (sizeBlankArea.y + cell.coordinate.y + 0.5f) * unit - ((paint.descent() + paint.ascent()) / 2)
            val initialWidth = (sizeBlankArea.x - hintsRow.size + 0.5f) * unit
            paint.color = Color.WHITE
            for (i in 0..sizeBlankArea.x - 1) {
                val path = Path()
                val left = i * unit + 1f
                val top = (sizeBlankArea.y + cell.coordinate.y) * unit + 1f
                val rect = RectF(left, top, left + unit - 2f, top + unit - 2f)
                path.addRect(rect, Path.Direction.CW)
                canvas.drawPath(path, paint)
            }
            paint.color = Color.BLACK
            for ((index, value) in hintsRow.withIndex()) {
                paint.getTextBounds(value.toString(), 0, value.toString().length, bounds)
                canvas.drawText(value.toString(), initialWidth - bounds.exactCenterX() + index * unit, textBaseHeight, paint)
            }

            val textBaseWidth = (sizeBlankArea.x + cell.coordinate.x + 0.5f) * unit
            val initialHeight = (sizeBlankArea.y - hintsColumn.size + 0.5f) * unit - ((paint.descent() + paint.ascent()) / 2)
            paint.color = Color.WHITE
            for (i in 0..sizeBlankArea.y - 1) {
                val path = Path()
                val left = (sizeBlankArea.x + cell.coordinate.x) * unit + 1f
                val top = i * unit + 1f
                val rect = RectF(left, top, left + unit - 2f, top + unit - 2f)
                path.addRect(rect, Path.Direction.CW)
                canvas.drawPath(path, paint)
            }
            paint.color = Color.BLACK
            for ((index, value) in hintsColumn.withIndex()) {
                paint.getTextBounds(value.toString(), 0, value.toString().length, bounds)
                canvas.drawText(value.toString(), textBaseWidth - bounds.exactCenterX(), initialHeight + index * unit, paint)
            }

            return bitmap
        }

        fun getHints(cells: List<Cell>): List<Int> {
            val hints: ArrayList<Int> = ArrayList()
            var stateBefore: Boolean? = null
            for (cell in cells) {
                if (stateBefore != null && !stateBefore && cell.state) hints.add(0)
                if (cell.state) {
                    if (hints.size < 1) hints.add(0)
                    hints[hints.lastIndex] += 1
                }

                stateBefore = cell.state
            }

            if (hints.size < 1) hints.add(0)

            return hints
        }

        fun getCellByCoordinate(cells: List<Cell>, coordinate: Point): Cell? {
            return cells.firstOrNull { it.coordinate.equals(coordinate.x, coordinate.y) }
        }

        fun getCellsInColumn(cells: List<Cell>, index: Int, size: Size): List<Cell>? {
            if (index < 0 || size.width < index) return null
            val cellsInRow: ArrayList<Cell> = ArrayList()
            for (i in 0..size.height - 1) {
                val cell = getCellByCoordinate(cells, Point(index, i)) ?: return null
                cellsInRow.add(cell)
            }

            return cellsInRow
        }

        fun getCellsInRow(cells: List<Cell>, index: Int, size: Size): List<Cell>? {
            if (index < 0 || size.height < index) return null
            val cellsInColumn: ArrayList<Cell> = ArrayList()
            for (i in 0..size.width - 1) {
                val cell = getCellByCoordinate(cells, Point(i, index)) ?: return null
                cellsInColumn.add(cell)
            }

            return cellsInColumn
        }

        fun isSolvable(cells: List<Cell>): Boolean {
            return true
        }
    }
}