package jp.co.seesaa.geckour.picrossmaker.util

import android.graphics.*
import android.util.Log
import android.util.Size
import android.util.SizeF
import android.widget.ImageView
import jp.co.seesaa.geckour.picrossmaker.Constant.Companion.unit
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import java.util.*

class Algorithm {
    companion object {
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

            val numBlankArea = getNumBlankArea(size)

            val unitX = canvas.width.toFloat() / (size.width + numBlankArea)
            val unitY = canvas.height.toFloat() / (size.height + numBlankArea)
            //if (Math.abs(unitX - unitY) > 0.1f) return null

            val cellX = (pointer.x / unitX).toInt() - numBlankArea
            val cellY = (pointer.y / unitY).toInt() - numBlankArea
            if (cellX < 0 || size.width < cellX || cellY < 0 || size.height < cellY) return null

            return Point(cellX, cellY)
        }

        fun createCanvasImage(canvas: ImageView?, size: Size): Bitmap? {
            if (canvas == null || size.width < 1 || size.height < 1) return null

            val numBlankArea = getNumBlankArea(size)
            Log.d("createCanvasImage", "numBlankArea: $numBlankArea")

            val bitmap = Bitmap.createBitmap(unit * (numBlankArea + size.width), unit * (numBlankArea + size.height), Bitmap.Config.ARGB_8888)
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
            for (i in 1..(numBlankArea + size.height) - 1) {
                path = Path()
                if (i == numBlankArea) p.color = Color.BLACK

                val height = c.height.toFloat() * i / (numBlankArea + size.height)
                path.moveTo(0f, height)
                path.lineTo(c.width.toFloat(), height)
                path.close()

                c.drawPath(path, p)

                p.color = lineColor
            }
            for (i in 1..(numBlankArea + size.width) - 1) {
                path = Path()
                if (i == numBlankArea) p.color = Color.BLACK

                val width = c.width.toFloat() * i / (numBlankArea + size.width)
                path.moveTo(width, 0f)
                path.lineTo(width, c.height.toFloat())
                path.close()

                c.drawPath(path, p)

                p.color = lineColor
            }

            val blankWidth = numBlankArea * unit.toFloat()

            p.style = Paint.Style.FILL
            p.color = Color.argb(255, 96, 96, 96)
            path = Path()
            path.addRect(0f, 0f, blankWidth, blankWidth, Path.Direction.CW)
            c.drawPath(path, p)

            p.color = Color.BLACK
            p.textSize = unit.toFloat()
            val bounds: Rect = Rect()
            p.getTextBounds("0", 0, "0".length, bounds)
            val textBaseHeight = blankWidth - unit / 2 - ((p.descent() + p.ascent()) / 2)
            for (i in 0..size.width - 1) {
                c.drawText("0", blankWidth + (i + 0.5f) * unit - bounds.exactCenterX(), textBaseHeight, p)
            }
            val textBaseWidth = blankWidth - 0.5f * unit - bounds.exactCenterX()
            for (i in 1..size.height) {
                c.drawText("0", textBaseWidth, blankWidth + (i - 0.5f) * unit - ((p.descent() + p.ascent()) / 2), p)
            }

            return bitmap
        }

        fun onEditCanvasImage(image: Bitmap?, size: Size, cells: List<Cell>, cell: Cell): Bitmap? {
            if (image == null || size.width < 1 || size.height < 1) return null

            val canvas = Canvas(image)
            val paint = Paint()
            val path = Path()
            val numBlank = getNumBlankArea(size)

            paint.style = Paint.Style.FILL
            paint.color = if (cell.state) Color.BLACK else Color.WHITE
            val left = (numBlank + cell.coordinate.x) * unit.toFloat() + 1f
            val top = (numBlank + cell.coordinate.y) * unit.toFloat() + 1f
            val rect = RectF(left, top, left + unit.toFloat() - 2f, top + unit.toFloat() - 2f)
            path.addRect(rect, Path.Direction.CW)
            canvas.drawPath(path, paint)

            refreshHints(canvas, size, cells, cell)

            return image
        }

        fun refreshHints(canvas: Canvas, size: Size, cells: List<Cell>, cell: Cell): Canvas {
            val row = getCellsInRow(cells, cell.coordinate.y, size) ?: return canvas
            val column = getCellsInColumn(cells, cell.coordinate.x, size) ?: return canvas
            val hintsRow = getHints(row)
            val hintsColumn = getHints(column)

            val paint = Paint()
            paint.isAntiAlias = true
            paint.style = Paint.Style.FILL
            paint.textSize = unit.toFloat()

            val numBlank = getNumBlankArea(size)
            val bounds: Rect = Rect()
            paint.getTextBounds("0", 0, "0".length, bounds)

            val textBaseHeight = (numBlank + cell.coordinate.y + 0.5f) * unit - ((paint.descent() + paint.ascent()) / 2)
            val initialWidth = (numBlank - hintsRow.size + 0.5f) * unit - bounds.exactCenterX()
            paint.color = Color.WHITE
            for (i in 0..numBlank - 1) {
                val path = Path()
                val left = i * unit + 1f
                val top = (numBlank + cell.coordinate.y) * unit + 1f
                val rect = RectF(left, top, left + unit - 2f, top + unit - 2f)
                path.addRect(rect, Path.Direction.CW)
                canvas.drawPath(path, paint)
            }
            paint.color = Color.BLACK
            for ((index, value) in hintsRow.withIndex()) {
                canvas.drawText(value.toString(), initialWidth + index * unit, textBaseHeight, paint)
            }

            val textBaseWidth = (numBlank + cell.coordinate.x + 0.5f) * unit - bounds.exactCenterX()
            val initialHeight = (numBlank - hintsColumn.size + 0.5f) * unit - ((paint.descent() + paint.ascent()) / 2)
            paint.color = Color.WHITE
            for (i in 0..numBlank - 1) {
                val path = Path()
                val left = (numBlank + cell.coordinate.x) * unit + 1f
                val top = i * unit + 1f
                val rect = RectF(left, top, left + unit - 2f, top + unit - 2f)
                path.addRect(rect, Path.Direction.CW)
                canvas.drawPath(path, paint)
            }
            paint.color = Color.BLACK
            for ((index, value) in hintsColumn.withIndex()) {
                canvas.drawText(value.toString(), textBaseWidth, initialHeight + index * unit, paint)
            }

            return canvas
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
    }
}