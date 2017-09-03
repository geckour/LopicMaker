package jp.co.seesaa.geckour.picrossmaker.util

import android.graphics.Point
import org.sat4j.core.VecInt
import org.sat4j.specs.ContradictionException
import org.sat4j.specs.ISolver
import org.sat4j.specs.TimeoutException
import org.sat4j.tools.SolverDecorator

class UniqueSolutionCounter(solver: ISolver, val size: Point): SolverDecorator<ISolver>(solver) {
    private var lowerBound: Long = 0
    private val length: Int = size.x * size.y

    fun lowerBound(): Long = this.lowerBound

    @Throws(TimeoutException::class)
    fun countSolutions(): Long {
        this.lowerBound = 0
        var trivialfalsity = false

        var latest = intArrayOf()
        while (!trivialfalsity && isSatisfiable(true)) {
            val last = model().copyOf(length)
            if ((last contentEquals latest).not()) {
                this.lowerBound++
                latest = last.copyOf()
            }

            val clause = VecInt(last.size)
            last.forEach {
                clause.push(-it)
            }
            try {
                addClause(clause)
            } catch (e: ContradictionException) {
                trivialfalsity = true
            }

        }
        return this.lowerBound
    }

    private fun getSolutionString(model: IntArray): String {
        return StringBuilder().apply {
            append("\n")
            model.forEachIndexed { index, i ->
                append("$i ")
                if ((index + 1).rem(size.x) == 0) append("\n")
            }
        }.toString()
    }
}