package jp.co.seesaa.geckour.picrossmaker.util

import android.graphics.*
import android.util.Log
import android.util.Size
import android.widget.ImageView
import jp.co.seesaa.geckour.picrossmaker.Constant.Companion.unit
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import jp.co.seesaa.geckour.picrossmaker.model.KeysState
import org.sat4j.core.VecInt
import org.sat4j.minisat.SolverFactory
import org.sat4j.specs.IProblem
import java.util.*

class Algorithm {
    companion object {
        val sizeBlankArea = Point(0, 0)
        private val solver = SolverFactory.newDefault()
        var editing = false

        fun initCells(cells: ArrayList<Cell>, size: Size) {
            for (i in 0..size.height - 1) (0..size.width - 1).mapTo(cells) { Cell(Point(it, i)) }
        }

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

        fun getCoordinateFromTouchPoint(canvas: ImageView?, pointer: PointF, size: Size): Point? {
            if (canvas == null || size.width < 1 || size.height < 1) return null


            val unitX = canvas.width.toFloat() / (size.width + sizeBlankArea.x)
            val unitY = canvas.height.toFloat() / (size.height + sizeBlankArea.y)
            if (Math.abs(unitX - unitY) > 0.1f) return null

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

        fun onEditCanvasImage(image: Bitmap?, size: Size, cells: List<Cell>, cell: Cell, refreshkeys: Boolean): Bitmap? {
            if (image == null || size.width < 1 || size.height < 1) return null

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

            return if (refreshkeys) refreshKeys(image, size, cells, cell) else image
        }

        fun refreshCanvasSize(image: Bitmap, drawAreaSize: Size, keysLengths: Size, cells: List<Cell>): Bitmap? {
            if (keysLengths.width > sizeBlankArea.x || keysLengths.height > sizeBlankArea.y) {

                sizeBlankArea.set(if (keysLengths.width > sizeBlankArea.x) keysLengths.width else sizeBlankArea.x,
                        if (keysLengths.height > sizeBlankArea.y) keysLengths.height else sizeBlankArea.y)
                var bitmap = createCanvasImage(drawAreaSize) ?: return image

                for (cell in cells) bitmap = onEditCanvasImage(bitmap, drawAreaSize, cells, cell, false) ?: return image

                return bitmap
            }

            return null
        }

        fun refreshAllKeys(image: Bitmap, size: Size, cells: List<Cell>): Bitmap {
            var bitmap = image
            val end = Math.max(size.width, size.height) - 1
            for (i in 0..end) {
                val cell = getCellByCoordinate(cells, Point(i % size.width, i % size.height)) ?: return image
                bitmap = refreshKeys(image, size, cells, cell)
            }

            return bitmap
        }

        fun refreshKeys(image: Bitmap, size: Size, cells: List<Cell>, cell: Cell): Bitmap {
            val row = getCellsInRow(cells, cell.coordinate.y, size) ?: return image
            val column = getCellsInColumn(cells, cell.coordinate.x, size) ?: return image
            val keysRow = getKeys(row)
            val keysColumn = getKeys(column)

            var bitmap = refreshCanvasSize(image, size, Size(keysRow.size, keysColumn.size), cells)
            if (bitmap != null) {
                bitmap = refreshAllKeys(bitmap, size, cells)
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
            val initialWidth = (sizeBlankArea.x - keysRow.size + 0.5f) * unit
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
            for ((index, value) in keysRow.withIndex()) {
                paint.getTextBounds(value.toString(), 0, value.toString().length, bounds)
                canvas.drawText(value.toString(), initialWidth - bounds.exactCenterX() + index * unit, textBaseHeight, paint)
            }

            val textBaseWidth = (sizeBlankArea.x + cell.coordinate.x + 0.5f) * unit
            val initialHeight = (sizeBlankArea.y - keysColumn.size + 0.5f) * unit - ((paint.descent() + paint.ascent()) / 2)
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

                stateBefore = cell.state
            }

            if (keys.size < 1) keys.add(0)

            return keys
        }

        fun getCellByCoordinate(cells: List<Cell>, coordinate: Point): Cell? {
            return cells.firstOrNull { it.coordinate.equals(coordinate.x, coordinate.y) }
        }

        fun getCellIndexByCoordinate(cells: List<Cell>, coordinate: Point): Int {
            return cells.indexOfFirst { it.coordinate.equals(coordinate.x, coordinate.y) }
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

        // FIXME
        fun isSolvable(size: Size, keysHorizontal: List<List<Int>>, keysVertical: List<List<Int>>): Boolean {
            solver.reset()

            val cells: ArrayList<Cell> = ArrayList()
            initCells(cells, size)

            // row
            for (i in 0..size.height - 1) {
                val keysStateRow = KeysState(size.width, keysHorizontal[i])

                if (!tseytinEncode1(keysStateRow)) return false
                if (!tseytinEncode2(keysStateRow)) return false
                if (!tseytinEncode3(keysStateRow, cells, Point(-1, i))) return false
            }

            // column
            for (i in 0..size.width - 1) {
                val keysStateColumn = KeysState(size.height, keysVertical[i])

                if (!tseytinEncode1(keysStateColumn)) return false
                if (!tseytinEncode2(keysStateColumn)) return false
                if (!tseytinEncode3(keysStateColumn, cells, Point(i, -1))) return false
            }

            return (solver as IProblem).isSatisfiable
        }

        fun tseytinEncode1(keysState: KeysState): Boolean {
            for (i in 0..keysState.keys.size - 1) {
                val v = VecInt()

                for (j in 0..keysState.slideMargin) {
                    v.push(keysState.getCnfVar(i, j) ?: return false)
                }

                solver.addClause(v)
            }

            for (i in 0..keysState.slideMargin) {
                for (j in 0..keysState.keys.size - 1) {
                    for (k in i + 1..keysState.slideMargin) {
                        val v = VecInt()
                        v.push(-(keysState.getCnfVar(j, k) ?: return false))
                        v.push(-(keysState.getCnfVar(j, i) ?: return false))

                        solver.addClause(v)
                    }
                }
            }

            return true
        }

        fun tseytinEncode2(keysState: KeysState): Boolean {
            for (i in 0..keysState.keys.size - 2) {
                for (j in 0..keysState.slideMargin) {
                    val v = VecInt()
                    v.push(-(keysState.getCnfVar(i, j) ?: return false))

                    for (k in 0..keysState.slideMargin) v.push(keysState.getCnfVar(i + 1, k) ?: return false)

                    solver.addClause(v)
                }
            }

            return true
        }

        fun tseytinEncode3(keysState: KeysState, cells: List<Cell>, coordinate: Point): Boolean {
            val isRow = coordinate.x < 0

            for (i in 0..keysState.lineSize - 1) {
                if (isRow) coordinate.x = i else coordinate.y = i
                val v = VecInt()
                val cellIndex = getCellIndexByCoordinate(cells, coordinate) + 1
                if (cellIndex < 1) return false
                v.push(-cellIndex)

                for ((j, key) in keysState.keys.withIndex()) {
                    for (k in 0..keysState.slideMargin) {
                        for (l in 0..key - 1) {
                            if (i == (keysState.getPreKeysSum(j) ?: return false) + j + k + l) {
                                v.push(keysState.getCnfVar(j, k) ?: return false)
                            }
                        }
                    }

                    solver.addClause(v)
                }
            }

            for (i in 0..keysState.lineSize - 1) {
                if (isRow) coordinate.x = i else coordinate.y = i
                val cellIndex = getCellIndexByCoordinate(cells, coordinate) + 1
                if (cellIndex < 1) return false

                for ((j, key) in keysState.keys.withIndex()) {
                    for (k in 0..keysState.slideMargin) {
                        for (l in 0..key - 1) {
                            if (i == (keysState.getPreKeysSum(j) ?: return false) + j + k + l) {
                                val v = VecInt()
                                v.push(cellIndex)
                                v.push(keysState.getCnfVar(j, k) ?: return false)

                                solver.addClause(v)
                            }
                        }
                    }
                }
            }

            return true
        }
    }
}