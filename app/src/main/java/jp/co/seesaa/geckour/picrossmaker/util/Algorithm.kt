package jp.co.seesaa.geckour.picrossmaker.util

import android.graphics.Point
import android.util.Log
import jp.co.seesaa.geckour.picrossmaker.model.KeyStates
import org.sat4j.core.VecInt
import org.sat4j.minisat.SolverFactory
import org.sat4j.specs.IProblem
import java.util.*

class Algorithm(size: Point): CanvasUtil(size) {
    private val solver = SolverFactory.newDefault()

    // FIXME
    fun isSolvable(): Boolean {
        solver.reset()
        KeyStates.varCount = size.x * size.y

        val keysHorizontal: List<List<Int>> = (0 until size.y)
                .mapTo(ArrayList()) {
                    val cellsInRow = getCellsInRow(it) ?: return false
                    getKeys(cellsInRow)
                }
        val keysVertical: List<List<Int>> = (0 until size.x)
                .mapTo(ArrayList()) {
                    val cellsInColumn = getCellsInColumn(it) ?: return false
                    getKeys(cellsInColumn)
                }

        // row
        (0 until size.y).forEach {
            val keyStatesRow = KeyStates(size.x, keysHorizontal[it])

            if (!tseytinEncode1(keyStatesRow)) return false
            if (!tseytinEncode2(keyStatesRow)) return false
            if (!tseytinEncode3(keyStatesRow, it, true)) return false
        }

        // column
        (0 until size.x).forEach {
            val keyStatesColumn = KeyStates(size.y, keysVertical[it])

            if (!tseytinEncode1(keyStatesColumn)) return false
            if (!tseytinEncode2(keyStatesColumn)) return false
            if (!tseytinEncode3(keyStatesColumn, it, false)) return false
        }

        val isSolvable = (solver as IProblem).isSatisfiable
        Log.d("isSolvable", "solvable: $isSolvable")
        if (isSolvable) Log.d("isSolvable", "solution: ${getSolutionString(solver.model())}")
        return isSolvable
    }

    private fun tseytinEncode1(keyStates: KeyStates): Boolean {
        for (i in 0 until keyStates.keys.size) {
            val v = VecInt()

            for (j in 0..keyStates.slideMargin) {
                v.push(keyStates.getCnfVar(i, j) ?: return false)
            }

            solver.addClause(v)
        }

        for (i in 0..keyStates.slideMargin) {
            for (j in 0 until keyStates.keys.size) {
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

    private fun tseytinEncode2(keyStates: KeyStates): Boolean {
        for (i in 0..keyStates.keys.size - 2) {
            for (j in 0..keyStates.slideMargin) {
                val v = VecInt()
                v.push(-(keyStates.getCnfVar(i, j) ?: return false))

                for (k in j..keyStates.slideMargin) {
                    v.push(keyStates.getCnfVar(i + 1, k) ?: return false)
                }

                solver.addClause(v)
            }
        }

        return true
    }

    private fun tseytinEncode3(keyStates: KeyStates, index: Int, isRow: Boolean): Boolean {
        for (i in 0 until keyStates.lineSize) {
            val coordinate = if (isRow) Point(i, index) else Point(index, i)
            val cellIndex = getCellIndexByCoordinate(coordinate)
            if (cellIndex < 0) return false

            for ((j, key) in keyStates.keys.withIndex()) {
                for (k in 0..keyStates.slideMargin) {
                    (0 until key)
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

        for (i in 0 until keyStates.lineSize) {
            val coordinate = if (isRow) Point(i, index) else Point(index, i)
            val cellIndex = getCellIndexByCoordinate(coordinate)
            if (cellIndex < 0) return false

            for ((j, key) in keyStates.keys.withIndex()) {
                for (k in 0..keyStates.slideMargin) {
                    for (l in 0 until key) {
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

    private fun getSolutionString(model: IntArray): String {
        val lastIndex = model.lastIndex
        var solution = "\n"
        for (i in 0 until size.x * size.y) {
            if (i > lastIndex) break
            solution += if (model[i] > 0) "■ " else "× "
            if ((i + 1).rem(size.x) == 0) solution += "\n"
        }

        return solution
    }
}