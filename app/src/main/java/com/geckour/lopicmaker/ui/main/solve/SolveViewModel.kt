package com.geckour.lopicmaker.ui.main.solve

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geckour.lopicmaker.R
import com.geckour.lopicmaker.data.DB
import com.geckour.lopicmaker.data.model.Problem
import com.geckour.lopicmaker.databinding.FragmentEditorBinding
import com.geckour.lopicmaker.ui.main.MainViewModel
import com.geckour.lopicmaker.util.Algorithm
import com.geckour.lopicmaker.util.SingleLiveEvent
import kotlinx.coroutines.launch
import java.sql.Timestamp

class SolveViewModel : ViewModel() {

    private var problemId: Long = -1L
    internal val problem = SingleLiveEvent<Problem>()

    internal var draft = true
    private var algorithm: Algorithm? = null
    private val nonNullAlgorithm: Algorithm get() = requireNotNull(algorithm)
    private var algorithm4Ref: Algorithm? = null
    private val nonNullAlgorithm4Ref: Algorithm get() = requireNotNull(algorithm4Ref)

    internal val snackbarStringResId = SingleLiveEvent<@androidx.annotation.StringRes Int>()

    internal val pointPrev0 = PointF(-1f, -1f)
    internal val pointPrev1 = PointF(-1f, -1f)
    private var prevAction: Int = MotionEvent.ACTION_CANCEL

    internal fun initCanvas(
        context: Context,
        binding: FragmentEditorBinding,
        problemId: Long
    ) {
        this.problemId = problemId
        viewModelScope.launch {
            DB.getInstance(context).problemDao().get(problemId)?.apply {
                this@SolveViewModel.problem.value = this
                algorithm = Algorithm(Point(keysHorizontal.size, keysVertical.size))
                algorithm4Ref = Algorithm(keysHorizontal, keysVertical)
                initCanvas(binding)
            }
        }
    }

    private fun initCanvas(binding: FragmentEditorBinding) {
        val cells = nonNullAlgorithm4Ref.getSolution()
        val bitmap = nonNullAlgorithm.overrideKeysFromCells(cells)
        binding.canvas.setImageBitmap(bitmap)
    }

    internal fun onScale(
        binding: FragmentEditorBinding,
        pointCurrent0: PointF,
        pointCurrent1: PointF
    ): Boolean {
        if (pointPrev0.x < 0f) pointPrev0.set(pointCurrent0)
        if (pointPrev1.x < 0f) pointPrev1.set(pointCurrent1)
        val scale = nonNullAlgorithm.getScale(
            nonNullAlgorithm.getPointDiff(pointPrev0, pointPrev1).length(),
            nonNullAlgorithm.getPointDiff(pointCurrent0, pointCurrent1).length()
        )
        val pointMidPrev = nonNullAlgorithm.getPointMid(pointPrev0, pointPrev1)
        val pointMidCurrent = nonNullAlgorithm.getPointMid(pointCurrent0, pointCurrent1)
        val diff = nonNullAlgorithm.getPointDiff(pointMidPrev, pointMidCurrent)
        binding.canvas.translationX = binding.canvas.translationX.plus(diff.x)
        binding.canvas.translationY = binding.canvas.translationY.plus(diff.y)
        binding.canvas.scaleX = binding.canvas.scaleX.times(scale)
        binding.canvas.scaleY = binding.canvas.scaleY.times(scale)
        pointPrev0.set(pointCurrent0)
        pointPrev1.set(pointCurrent1)
        return true
    }

    internal fun onDrag(binding: FragmentEditorBinding, pointCurrent0: PointF): Boolean {
        if (pointPrev0.x < 0f) pointPrev0.set(pointCurrent0)
        val diff = nonNullAlgorithm.getPointDiff(pointPrev0, pointCurrent0)
        binding.canvas.translationX = binding.canvas.translationX.plus(diff.x)
        binding.canvas.translationY = binding.canvas.translationY.plus(diff.y)
        pointPrev0.set(pointCurrent0)
        return true
    }

    internal fun onTouchCanvas(
        context: Context,
        binding: FragmentEditorBinding,
        event: MotionEvent,
        fabLeftMode: MainViewModel.FabLeftMode?
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                pointPrev0.set(-1f, -1f)
                pointPrev1.set(-1f, -1f)

                if (nonNullAlgorithm4Ref.checkCellsWithSolution(nonNullAlgorithm.cells)) {
                    viewModelScope.launch {
                        problem.value?.apply {
                            if (this.thumb == null) {
                                thumb = nonNullAlgorithm.getThumbnailImage()
                                DB.getInstance(context).problemDao().update(
                                    this.copy(thumb = thumb, editedAt = Timestamp(System.currentTimeMillis()))
                                )
                            }
                        }
                        snackbarStringResId.postValue(R.string.solve_fragment_message_solved)
                    }
                }
            }

            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_MOVE -> {
                when (prevAction) {
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_POINTER_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        nonNullAlgorithm.prevCells.apply {
                            clear()
                            addAll(nonNullAlgorithm.cells.map { it.copy() })
                        }
                    }
                }

                val pointCurrent = PointF(event.x, event.y)
                val coordCurrent = nonNullAlgorithm.getCoordinateFromTouchPoint(
                    binding.canvas,
                    pointCurrent
                ) ?: return true
                val coordPrev = nonNullAlgorithm.getCoordinateFromTouchPoint(
                    binding.canvas,
                    pointPrev0
                ) ?: Point(-1, -1)
                if (!coordCurrent.equals(coordPrev.x, coordPrev.y)) {
                    val cell = nonNullAlgorithm.getCellByCoordinate(coordCurrent) ?: return true

                    when (event.action) {
                        MotionEvent.ACTION_MOVE -> {
                            val cellPrev = nonNullAlgorithm.getCellByCoordinate(coordPrev) ?: run {
                                pointPrev0.set(-1f, -1f)
                                return true
                            }
                            cell.state = cellPrev.state
                        }
                        else -> {
                            val state = cell.state
                            cell.state = when (state) {
                                Problem.Cell.State.Fill, Problem.Cell.State.MarkNotFill -> Problem.Cell.State.Blank
                                Problem.Cell.State.Blank -> {
                                    when (fabLeftMode) {
                                        MainViewModel.FabLeftMode.FILL -> Problem.Cell.State.Fill
                                        else -> Problem.Cell.State.MarkNotFill
                                    }
                                }
                            }
                        }
                    }

                    val bitmap =
                        nonNullAlgorithm.onEditCanvasImage(
                            (binding.canvas.drawable as BitmapDrawable).bitmap,
                            cell,
                            false
                        )
                    binding.canvas.setImageBitmap(bitmap)
                }
                pointPrev0.set(pointCurrent)
            }
        }
        prevAction = event.action

        return true
    }
}