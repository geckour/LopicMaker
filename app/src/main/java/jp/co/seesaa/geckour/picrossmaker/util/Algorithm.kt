package jp.co.seesaa.geckour.picrossmaker.util

import android.graphics.Point
import android.util.Log
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import jp.co.seesaa.geckour.picrossmaker.model.KeyStates
import org.sat4j.core.VecInt
import org.sat4j.minisat.SolverFactory
import org.sat4j.specs.IProblem
import java.util.*

class Algorithm(size: Point): CanvasUtil(size) {
    private val solver = SolverFactory.newDefault()

    // FIXME
    fun isSolvable(keysHorizontal: List<List<Int>>, keysVertical: List<List<Int>>): Boolean {
        solver.reset()

        val cells: ArrayList<Cell> = ArrayList()
        initCells(cells)

        KeyStates.varCount = size.x * size.y

        // row
        for (i in 0..size.y - 1) {
            val keyStatesRow = KeyStates(size.x, keysHorizontal[i])

            if (!tseytinEncode1(keyStatesRow)) return false
            if (!tseytinEncode2(keyStatesRow)) return false
            if (!tseytinEncode3(keyStatesRow, i, true)) return false
        }

        // column
        for (i in 0..size.x - 1) {
            val keyStatesColumn = KeyStates(size.y, keysVertical[i])

            if (!tseytinEncode1(keyStatesColumn)) return false
            if (!tseytinEncode2(keyStatesColumn)) return false
            if (!tseytinEncode3(keyStatesColumn, i, false)) return false
        }

        val isSolvable = (solver as IProblem).isSatisfiable
        Log.d("isSolvable", "$isSolvable")
        return isSolvable
    }

    fun tseytinEncode1(keyStates: KeyStates): Boolean {
        for (i in 0..keyStates.actualKeys.size - 1) {
            val v = VecInt()

            for (j in 0..keyStates.slideMargin) {
                v.push(keyStates.getCnfVar(i, j) ?: return false)
            }

            solver.addClause(v)
        }

        for (i in 0..keyStates.slideMargin) {
            for (j in 0..keyStates.actualKeys.size - 1) {
                for (k in i + 1..keyStates.slideMargin) {
                    val v = VecInt()
                    v.push(-(keyStates.getCnfVar(j, k) ?: return false))
                    v.push(-(keyStates.getCnfVar(j, i) ?: return false))

                    solver.addClause(v)
                }
            }
        }

        return true
    }

    fun tseytinEncode2(keyStates: KeyStates): Boolean {
        for (i in 0..keyStates.actualKeys.size - 2) {
            for (j in 0..keyStates.slideMargin) {
                val v = VecInt()
                v.push(-(keyStates.getCnfVar(i, j) ?: return false))

                (j..keyStates.slideMargin).forEach {
                    v.push(keyStates.getCnfVar(i + 1, it) ?: return false)
                }

                solver.addClause(v)
            }
        }

        return true
    }

    fun tseytinEncode3(keyStates: KeyStates, index: Int, isRow: Boolean): Boolean {
        for (i in 0..keyStates.lineSize - 1) {
            val coordinate = if (isRow) Point(i, index) else Point(index, i)
            val cellIndex = getCellIndexByCoordinate(coordinate) + 1
            if (cellIndex < 1) return false

            for ((j, key) in keyStates.actualKeys.withIndex()) {
                for (k in 0..keyStates.slideMargin) {
                    (0..key - 1)
                            .filter { i == (keyStates.getPreKeysSum(j) ?: return false) + j + k + it }
                            .forEach {
                                val v = VecInt()
                                v.push(-cellIndex)
                                v.push(keyStates.getCnfVar(j, k) ?: return false)
                                solver.addClause(v)
                            }
                }
            }
        }

        for (i in 0..keyStates.lineSize - 1) {
            val coordinate = if (isRow) Point(i, index) else Point(index, i)
            val cellIndex = getCellIndexByCoordinate(coordinate) + 1
            if (cellIndex < 1) return false

            for ((j, key) in keyStates.actualKeys.withIndex()) {
                for (k in 0..keyStates.slideMargin) {
                    for (l in 0..key - 1) {
                        if (i == (keyStates.getPreKeysSum(j) ?: return false) + j + k + l) {
                            val v = VecInt()
                            v.push(cellIndex)
                            v.push(-(keyStates.getCnfVar(j, k) ?: return false))

                            solver.addClause(v)
                        }
                    }
                }
            }
        }

        return true
    }
}