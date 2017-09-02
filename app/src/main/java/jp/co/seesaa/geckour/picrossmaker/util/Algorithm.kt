package jp.co.seesaa.geckour.picrossmaker.util

import android.graphics.Point
import jp.co.seesaa.geckour.picrossmaker.model.KeyStates
import org.sat4j.core.VecInt
import org.sat4j.minisat.SolverFactory
import org.sat4j.specs.ISolver
import java.util.*

class Algorithm(size: Point): CanvasUtil(size) {

    enum class SatisfactionState {
        Satisfiable,
        ExistMultipleSolution,
        Unsatisfiable
    }

    private val solver: ISolver = SolverFactory.newDefault().apply { timeoutMs = 500 }

    fun getSatisfactionState(solutionCount: Long) =
            when {
                solutionCount == 1L -> SatisfactionState.Satisfiable
                solutionCount > 1L -> SatisfactionState.ExistMultipleSolution
                else -> SatisfactionState.Unsatisfiable
            }

    fun getSolutionCounter(): UniqueSolutionCounter? {
        solver.reset()
        KeyStates.varCount = size.x * size.y

        val keysHorizontal: List<List<Int>> = (0 until size.y)
                .mapTo(ArrayList()) {
                    val cellsInRow = getCellsInRow(it) ?: return null
                    getKeys(cellsInRow)
                }
        val keysVertical: List<List<Int>> = (0 until size.x)
                .mapTo(ArrayList()) {
                    val cellsInColumn = getCellsInColumn(it) ?: return null
                    getKeys(cellsInColumn)
                }

        // row
        (0 until size.y).forEach {
            val keyStatesRow = KeyStates(size.x, keysHorizontal[it])

            if (tseytinEncode1(keyStatesRow).not()) return null
            if (tseytinEncode2(keyStatesRow).not()) return null
            if (tseytinEncode3(keyStatesRow, it, true).not()) return null
        }

        // column
        (0 until size.x).forEach {
            val keyStatesColumn = KeyStates(size.y, keysVertical[it])

            if (tseytinEncode1(keyStatesColumn).not()) return null
            if (tseytinEncode2(keyStatesColumn).not()) return null
            if (tseytinEncode3(keyStatesColumn, it, false).not()) return null
        }

        return UniqueSolutionCounter(solver, size)
    }

    private fun tseytinEncode1(keyStates: KeyStates): Boolean {
        for (i in 0..keyStates.keys.lastIndex) {
            val v = VecInt()

            for (j in 0..keyStates.slideMargin) {
                v.push(keyStates.getCnfVar(i, j) ?: return false)
            }

            solver.addClause(v)
        }

        for (i in 0..keyStates.slideMargin) {
            for (j in 0..keyStates.keys.lastIndex) {
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
        for (i in 0 until keyStates.keys.lastIndex) {
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

            val v = VecInt()
            v.push(-cellIndex)
            for ((j, key) in keyStates.keys.withIndex()) {
                for (k in 0..keyStates.slideMargin) {
                    (0 until key)
                            .filter { i == (keyStates.getPreKeysSum(j) ?: return false) + j + k + it }
                            .forEach {
                                v.push(keyStates.getCnfVar(j, k) ?: return false)
                            }
                }
            }

            solver.addClause(v)
        }

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
                                v.push(cellIndex)
                                v.push(-(keyStates.getCnfVar(j, k) ?: return false))

                                solver.addClause(v)
                            }
                    }
            }
        }

        return true
    }

    fun getSolutionString(): String =
            if (solver.isSatisfiable) {
                solver.findModel()?.let {
                    val lastIndex = it.lastIndex

                    StringBuilder().apply {
                        append("\n")
                        (0 until size.x * size.y).forEach { i ->
                            if (i > lastIndex) return@forEach
                            append(if (it[i] > 0) "■ " else "× ")
                            if ((i + 1).rem(size.x) == 0) append("\n")
                        }
                    }.toString()
                } ?: "There may be line of fulfilling their own"
            } else "no solution"
}