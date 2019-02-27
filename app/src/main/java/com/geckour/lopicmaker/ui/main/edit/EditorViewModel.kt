package com.geckour.lopicmaker.ui.main.edit

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.view.MotionEvent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geckour.lopicmaker.App
import com.geckour.lopicmaker.R
import com.geckour.lopicmaker.data.DB
import com.geckour.lopicmaker.data.dao.upsert
import com.geckour.lopicmaker.data.model.Problem
import com.geckour.lopicmaker.databinding.FragmentEditorBinding
import com.geckour.lopicmaker.ui.main.MainViewModel
import com.geckour.lopicmaker.util.Algorithm
import com.geckour.lopicmaker.util.MyAlertDialogFragment
import com.geckour.lopicmaker.util.SingleLiveEvent
import com.geckour.lopicmaker.util.fromJson
import com.geckour.lopicmaker.util.solutionCount
import kotlinx.coroutines.launch
import timber.log.Timber
import java.sql.Timestamp

class EditorViewModel : ViewModel() {

    private var problemId: Long = -1L
    internal val problem = SingleLiveEvent<Problem>()

    internal var draft = true
    private var algorithm: Algorithm? = null
    private val nonNullAlgorithm: Algorithm get() = requireNotNull(algorithm)
    internal val satisfactionState = SingleLiveEvent<Algorithm.SatisfactionState>()

    internal val snackbarStringResId = SingleLiveEvent<@androidx.annotation.StringRes Int>()

    internal val pointPrev0 = PointF(-1f, -1f)
    internal val pointPrev1 = PointF(-1f, -1f)
    private var prevAction: Int = MotionEvent.ACTION_CANCEL

    internal val fabLeftVisible = SingleLiveEvent<Boolean>()
    internal val fabLeftMode = SingleLiveEvent<MainViewModel.FabLeftMode>()
    internal val toolbarTitleResId = SingleLiveEvent<Pair<@androidx.annotation.StringRes Int, List<String>>>()

    internal fun initCanvas(
        context: Context,
        binding: FragmentEditorBinding,
        problemId: Long,
        size: Point?
    ) {
        this.problemId = problemId
        viewModelScope.launch {
            val problem = DB.getInstance(context).problemDao().get(problemId)?.apply {
                this@EditorViewModel.problem.value = this
                algorithm = Algorithm(Point(keysHorizontal.size, keysVertical.size))
                initCanvas(binding, this)
            }
            if (problem == null) initCanvas(binding, size ?: return@launch)
        }
    }

    private fun initCanvas(binding: FragmentEditorBinding, problem: Problem) {
        val bitmap = problem.let {
            nonNullAlgorithm.setCells(it.cells)
        }
        binding.canvasImage = bitmap
    }

    private fun initCanvas(binding: FragmentEditorBinding, size: Point) {
        algorithm = Algorithm(size)
        binding.canvasImage = nonNullAlgorithm.createCanvasImage()
    }

    internal fun saveProblem(context: Context, data: String, draft: Boolean) {
        try {
            val problem = createProblem(data, draft) ?: return
            viewModelScope.launch {
                DB.getInstance(context).problemDao().insert(problem)
                snackbarStringResId.postValue(R.string.editor_fragment_message_complete_save)
            }
        } catch (t: Throwable) {
            Timber.e(t)
            snackbarStringResId.postValue(R.string.editor_fragment_error_failure_save)
        }
    }

    internal fun overwriteProblem(context: Context, data: String) {
        problem.value?.apply {
            try {
                viewModelScope.launch {
                    val metadata: MyAlertDialogFragment.ProblemMetadata? =
                        try {
                            App.gson.fromJson(data)
                        } catch (e: Exception) {
                            Timber.e(e)
                            null
                        }
                    this@apply.draft =
                        satisfactionState.value !=
                                Algorithm.SatisfactionState.Satisfiable
                    this@apply.editedAt = Timestamp(System.currentTimeMillis())
                    this@apply.thumb = nonNullAlgorithm.getThumbnailImage()
                    metadata?.let {
                        this@apply.title = it.title
                        this@apply.tags = it.tags
                    }
                    problem.postValue(this@apply)
                    val result = this@apply.upsert(DB.getInstance(context))
                    snackbarStringResId.postValue(
                        if (result > -1) R.string.editor_fragment_message_complete_save
                        else R.string.editor_fragment_error_failure_save
                    )
                }
            } catch (t: Throwable) {
                Timber.e(t)
                snackbarStringResId.postValue(R.string.editor_fragment_error_failure_save)
            }
        }
    }

    private fun createProblem(data: String, draft: Boolean, id: Long = 0): Problem? {
        val metadata: MyAlertDialogFragment.ProblemMetadata =
            try {
                App.gson.fromJson(data)
            } catch (e: Exception) {
                Timber.e(e)
                return null
            }

        val thumb = nonNullAlgorithm.getThumbnailImage()
        val keysInRow: ArrayList<List<Int>> = ArrayList()
        (0 until nonNullAlgorithm.size.y).forEach {
            keysInRow.add(
                nonNullAlgorithm.getKeys(
                    nonNullAlgorithm.getCellsInRow(it)
                        ?: return@forEach
                )
            )
        }
        val keysInColumn: ArrayList<List<Int>> = ArrayList()
        (0 until nonNullAlgorithm.size.x).forEach {
            keysInColumn.add(
                nonNullAlgorithm.getKeys(
                    nonNullAlgorithm.getCellsInColumn(it)
                        ?: return@forEach
                )
            )
        }

        return Problem(
            id,
            metadata.title,
            draft,
            metadata.tags,
            keysInRow,
            keysInColumn,
            thumb,
            cells = nonNullAlgorithm.currentCells
        )
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

    internal fun onTouchCanvas(binding: FragmentEditorBinding, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                pointPrev0.set(-1f, -1f)
                pointPrev1.set(-1f, -1f)

                showUndoButtonIfAvailable()
                refreshSatisfactionState()
            }

            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_MOVE -> {
                when (prevAction) {
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_POINTER_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        nonNullAlgorithm.setCells()
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
                                Problem.Cell.State.Fill -> Problem.Cell.State.Blank
                                Problem.Cell.State.Blank -> Problem.Cell.State.Fill
                                else -> state
                            }
                        }
                    }

                    val bitmap =
                        nonNullAlgorithm.onEditCanvasImage(
                            binding.canvasImage, cell,
                            true
                        )
                    binding.canvasImage = bitmap
                }
                pointPrev0.set(pointCurrent)
            }
        }
        prevAction = event.action

        return true
    }

    internal fun onSaveCanvas(
        activity: FragmentActivity?,
        editorFragment: EditorFragment,
        optional: String? = null,
        overwrite: Boolean? = null
    ) {
        activity ?: return

        val requireOverwrite = overwrite ?: (problemId > -1)

        viewModelScope.launch {
            val requestCode =
                when {
                    requireOverwrite -> MyAlertDialogFragment.RequestCode.CONFIRM_BEFORE_SAVE
                    else -> {
                        if (satisfactionState.value == Algorithm.SatisfactionState.Satisfiable)
                            MyAlertDialogFragment.RequestCode.SAVE_PROBLEM
                        else MyAlertDialogFragment.RequestCode.SAVE_DRAFT_PROBLEM
                    }
                }
            val inheritOptional = DB.getInstance(activity).problemDao().get(problemId)?.let {
                App.gson.toJson(MyAlertDialogFragment.ProblemMetadata(it.title, it.tags))
            }
            showDialog(activity, editorFragment, requireOverwrite, inheritOptional ?: optional, requestCode)
        }
    }

    private fun showDialog(
        activity: FragmentActivity?,
        editorFragment: EditorFragment,
        requireOverwrite: Boolean,
        optional: String?,
        requestCode: MyAlertDialogFragment.RequestCode
    ) {
        activity ?: return

        val fragment = MyAlertDialogFragment.newInstance(
            resId = if (requireOverwrite) null else R.layout.dialog_define_title_and_tags,
            title = activity.getString(
                if (requireOverwrite) R.string.dialog_alert_title_overwrite
                else {
                    if (satisfactionState.value == Algorithm.SatisfactionState.Satisfiable) R.string.dialog_alert_title_save_problem
                    else R.string.dialog_alert_title_save_draft_problem
                }
            ),
            optional = optional,
            message = activity.getString(
                if (requireOverwrite) R.string.dialog_alert_message_confirm_before_save
                else {
                    when (satisfactionState.value) {
                        Algorithm.SatisfactionState.Satisfiable ->
                            R.string.dialog_alert_message_save_problem
                        Algorithm.SatisfactionState.ExistMultipleSolution ->
                            R.string.dialog_alert_message_save_draft_problem_exist_multiple_solution
                        else ->
                            R.string.dialog_alert_message_save_draft_problem_unsatisfiable
                    }
                }
            ),
            requestCode = requestCode,
            cancelable = true,
            targetFragment = editorFragment
        )

        fragment.show(activity.supportFragmentManager, MyAlertDialogFragment.getTag(requestCode))
    }

    internal fun refreshSatisfactionState() {
        viewModelScope.launch {
            val state = algorithm?.getSolutionCounter()?.let {
                nonNullAlgorithm.getSatisfactionState(it.solutionCount)
            } ?: run {
                Timber.d("solutionCounter is null")
                Algorithm.SatisfactionState.Unsatisfiable
            }
            satisfactionState.postValue(state)
        }
    }

    private fun showUndoButtonIfAvailable() {
        if (algorithm?.undoAvailable == true) {
            fabLeftMode.postValue(MainViewModel.FabLeftMode.UNDO)
            fabLeftVisible.postValue(true)
        }
    }

    private fun showRedoButtonIfAvailable() {
        if (algorithm?.redoAvailable == true) {
            fabLeftMode.postValue(MainViewModel.FabLeftMode.REDO)
            fabLeftVisible.postValue(true)
        }
    }

    internal fun onUndoClicked(binding: FragmentEditorBinding) {
        nonNullAlgorithm.undo()
        binding.canvasImage = nonNullAlgorithm.getNewBitmap()
        showRedoButtonIfAvailable()
    }

    internal fun onRedoClicked(binding: FragmentEditorBinding) {
        nonNullAlgorithm.redo()
        binding.canvasImage = nonNullAlgorithm.getNewBitmap()
        showUndoButtonIfAvailable()
    }
}