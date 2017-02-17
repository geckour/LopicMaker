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
        fun getHint(data: List<List<Boolean>>): List<List<Int>> {
            for (l in data) {
                for (b in l) {

                }
            }
            return ArrayList()
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

        fun getCoordinate(canvas: ImageView?, pointer: PointF, size: Size): Point? {
            if (canvas == null || size.width < 1 || size.height < 1) return null

            val numBlankArea = getNumBlankArea(size)

            val unitX = canvas.width.toFloat() / (size.width + numBlankArea)
            val unitY = canvas.height.toFloat() / (size.height + numBlankArea)
            //if (Math.abs(unitX - unitY) > 0.1f) return null

            val cX = (pointer.x / unitX).toInt() - numBlankArea
            val cY = (pointer.y / unitY).toInt() - numBlankArea
            if (cX < 0 || size.width < cX || cY < 0 || size.height < cY) return null

            return Point(cX, cY)
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
            val textBaseHeight = blankWidth - unit / 2 - ((p.descent() + p.ascent()) / 2)
            p.getTextBounds("0", 0, "0".length, bounds)
            for (i in 0..size.width - 1) {
                c.drawText("0", blankWidth + (i + 0.5f) * unit - bounds.exactCenterX(), textBaseHeight, p)
            }
            val textBaseWidth = blankWidth - 0.5f * unit - bounds.exactCenterX()
            for (i in 1..size.height) {
                c.drawText("0", textBaseWidth, blankWidth + (i - 0.5f) * unit - ((p.descent() + p.ascent()) / 2), p)
            }

            return bitmap
        }

        fun onEditCanvasImage(image: Bitmap?, size: Size, cell: Cell): Bitmap? {
            if (image == null || size.width < 1 || size.height < 1) return null

            val canvas = Canvas(image)
            val paint = Paint()
            val path = Path()
            val numBlank = getNumBlankArea(size)

            paint.style = Paint.Style.FILL
            paint.color = if (cell.state) Color.BLACK else Color.WHITE
            val startX = (numBlank + cell.coordinate.x) * unit.toFloat()
            val startY = (numBlank + cell.coordinate.y) * unit.toFloat()
            val rect = RectF(startX, startY, startX + unit.toFloat(), startY + unit.toFloat())
            Log.d("onEditCanvasImage", "rect: $rect")
            path.addRect(rect, Path.Direction.CW)
            canvas.drawPath(path, paint)

            return image
        }

        fun getCellByCoordinate(cells: List<Cell>, coordinate: Point): Cell? {
            return cells.firstOrNull { it.coordinate.equals(coordinate.x, coordinate.y) }
        }
    }
}