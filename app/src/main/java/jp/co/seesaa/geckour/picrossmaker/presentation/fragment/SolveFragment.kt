package jp.co.seesaa.geckour.picrossmaker.presentation.fragment

import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import jp.co.seesaa.geckour.picrossmaker.App
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentEditorBinding
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider
import jp.co.seesaa.geckour.picrossmaker.model.Problem
import jp.co.seesaa.geckour.picrossmaker.presentation.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.util.*
import kotlinx.coroutines.experimental.Job
import java.sql.Timestamp

class SolveFragment : Fragment() {

    enum class ArgKeys {
        CANVAS_SIZE,
        PROBLEM_ID,
        DRAFT
    }

    enum class TouchMode {
        Edit,
        Scale
    }

    enum class EditMode {
        Fill,
        MarkNotFill
    }

    companion object {
        fun newInstance(vararg args: Pair<ArgKeys, Any>): SolveFragment? =
                if ((args.find { it.first == ArgKeys.PROBLEM_ID }?.second as? Long)?.let { it > -1 } == true) {
                    SolveFragment().apply {
                        arguments = Bundle().apply {
                            for (arg in args) {
                                when (arg.second) {
                                    is Long -> putLong(arg.first.name, arg.second as Long)
                                    is String -> putString(arg.first.name, arg.second as String)
                                    is Int -> putInt(arg.first.name, arg.second as Int)
                                    is Boolean -> putBoolean(arg.first.name, arg.second as Boolean)
                                }
                            }
                        }
                    }
                } else null

        val TAG: String = this::class.java.simpleName
    }

    private var problemId = -1L
    private var problem: Problem? = null
    private lateinit var binding: FragmentEditorBinding
    private val pointPrev0 = PointF(-1f, -1f)
    private val pointPrev1 = PointF(-1f, -1f)
    private var satisfactionState = Algorithm.SatisfactionState.Unsatisfiable
    private val algorithm: Algorithm by lazy {
        Algorithm(problem?.let { Point(it.keysHorizontal.keys.size, it.keysVertical.keys.size) }
                ?: Point(0, 0))
    }
    private val algorithm4ref: Algorithm by lazy {
        problem?.let { Algorithm(it.keysHorizontal.keys.toList(), it.keysVertical.keys.toList()) }
                ?: algorithm
    }
    private val jobList: ArrayList<Job> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        problemId = arguments?.let { if (it.containsKey(ArgKeys.PROBLEM_ID.name)) it.getLong(ArgKeys.PROBLEM_ID.name, -1) else -1 } ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentEditorBinding.inflate(inflater, container, false).apply { binding = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FIXME: 不正な問題IDを与えられた場合は前のFragmentに戻る・ダイアログを表示
        if (arguments?.containsKey(ArgKeys.CANVAS_SIZE.name)?.not() != false && problemId > -1) {
            ui(jobList) {
                this@SolveFragment.problem = async { OrmaProvider.db.selectFromProblem().idEq(problemId).valueOrNull() }?.await()

                this@SolveFragment.problem?.apply {
                    mainActivity?.supportActionBar?.title = getString(R.string.action_bar_title_solve_with_title, title)
                }
                        ?: run { mainActivity?.supportActionBar?.setTitle(R.string.action_bar_title_solve) }

                initCanvas(savedInstanceState)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.canvas.setOnTouchListener { _, event -> onTouchCanvas(event) }
        binding.cover.setOnTouchListener { _, event ->
            return@setOnTouchListener if (getMode() == TouchMode.Scale) {
                when (event.action) {
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        pointPrev0.set(-1f, -1f)
                        pointPrev1.set(-1f, -1f)
                        true
                    }

                    else -> {
                        val p0 = PointF(event.getX(0), event.getY(0))
                        if (event.pointerCount > 1) {
                            val p1 = PointF(event.getX(1), event.getY(1))
                            onScale(p0, p1)
                        } else onDrag(p0)
                    }
                }
            } else false
        }
    }

    override fun onResume() {
        super.onResume()

        mainActivity?.binding?.appBarMain?.fabRight?.apply {
            tag = TouchMode.Edit
            setImageResource(R.drawable.ic_crop_free_white_24px)
            setOnClickListener {
                val mode = tag as TouchMode
                (it as FloatingActionButton).setImageResource(
                        when (mode) {
                            TouchMode.Edit -> R.drawable.ic_edit_white_24px
                            TouchMode.Scale -> R.drawable.ic_crop_free_white_24px
                        })
                tag = when (mode) {
                    TouchMode.Edit -> TouchMode.Scale
                    TouchMode.Scale -> TouchMode.Edit
                }
            }
            show()
        }

        mainActivity?.binding?.appBarMain?.fabLeft?.apply {
            tag = EditMode.Fill
            setImageResource(R.drawable.ic_fill)
            setOnClickListener {
                when (tag) {
                    EditMode.Fill -> {
                        tag = EditMode.MarkNotFill
                        setImageResource(R.drawable.ic_close_white_24px)
                    }
                    else -> {
                        tag = EditMode.Fill
                        setImageResource(R.drawable.ic_fill)
                    }
                }
            }
            show()
        }
    }

    override fun onPause() {
        super.onPause()

        jobList.apply {
            forEach { it.cancel() }
            clear()
        }
        mainActivity?.binding?.appBarMain?.fabLeft?.hide()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(CanvasUtil.BUNDLE_NAME_CELLS, ArrayList(algorithm.cells.map { App.gson.toJson(it) }))
    }

    private fun onScale(pointCurrent0: PointF, pointCurrent1: PointF): Boolean {
        if (pointPrev0.x < 0f) pointPrev0.set(pointCurrent0)
        if (pointPrev1.x < 0f) pointPrev1.set(pointCurrent1)
        val scale = algorithm.getScale(algorithm.getPointDiff(pointPrev0, pointPrev1).length(), algorithm.getPointDiff(pointCurrent0, pointCurrent1).length())
        val pointMidPrev = algorithm.getPointMid(pointPrev0, pointPrev1)
        val pointMidCurrent = algorithm.getPointMid(pointCurrent0, pointCurrent1)
        val diff = algorithm.getPointDiff(pointMidPrev, pointMidCurrent)
        binding.canvas.translationX = binding.canvas.translationX.plus(diff.x)
        binding.canvas.translationY = binding.canvas.translationY.plus(diff.y)
        binding.canvas.scaleX = binding.canvas.scaleX.times(scale)
        binding.canvas.scaleY = binding.canvas.scaleY.times(scale)
        pointPrev0.set(pointCurrent0)
        pointPrev1.set(pointCurrent1)
        return true
    }

    private fun onDrag(pointCurrent0: PointF): Boolean {
        if (pointPrev0.x < 0f) pointPrev0.set(pointCurrent0)
        val diff = algorithm.getPointDiff(pointPrev0, pointCurrent0)
        binding.canvas.translationX = binding.canvas.translationX.plus(diff.x)
        binding.canvas.translationY = binding.canvas.translationY.plus(diff.y)
        pointPrev0.set(pointCurrent0)
        return true
    }

    private var prevAction: Int = MotionEvent.ACTION_CANCEL
    private fun onTouchCanvas(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                pointPrev0.set(-1f, -1f)
                pointPrev1.set(-1f, -1f)

                if (algorithm4ref.checkCellsWithSolution(algorithm.cells)) {
                    if (problem?.thumb == null) {
                        problem?.apply {
                            thumb = algorithm.getThumbnailImage()
                            ui(jobList) {
                                async {
                                    OrmaProvider.db.updateProblem().idEq(id)
                                            .thumb(thumb)
                                            .editedAt(Timestamp(System.currentTimeMillis()))
                                            .execute()
                                }
                            }
                        }
                    }

                    mainActivity?.binding?.appBarMain?.contentMain?.container?.let {
                        showSnackbar(it, R.string.solve_fragment_message_solved)
                    }
                }
            }

            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                when (prevAction) {
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        algorithm.prevCells.apply {
                            clear()
                            addAll(algorithm.cells.map { it.copy() })
                        }
                    }
                }

                if (event.action == MotionEvent.ACTION_MOVE) satisfactionState = Algorithm.SatisfactionState.Unsatisfiable

                val pointCurrent = PointF(event.x, event.y)
                val coordCurrent = algorithm.getCoordinateFromTouchPoint(
                        binding.canvas,
                        pointCurrent) ?: return true
                val coordPrev = algorithm.getCoordinateFromTouchPoint(
                        binding.canvas,
                        pointPrev0) ?: Point(-1, -1)
                if (!coordCurrent.equals(coordPrev.x, coordPrev.y)) {
                    val cell = algorithm.getCellByCoordinate(coordCurrent) ?: return true

                    when (event.action) {
                        MotionEvent.ACTION_MOVE -> {
                            val cellPrev = algorithm.getCellByCoordinate(coordPrev) ?: run {
                                pointPrev0.set(-1f, -1f)
                                return true
                            }
                            cell.setState(cellPrev.getState())
                        }
                        else -> {
                            val state = cell.getState()
                            cell.setState(
                                    when (state) {
                                        Cell.State.Fill, Cell.State.MarkNotFill -> Cell.State.Blank
                                        Cell.State.Blank -> {
                                            val editMode = mainActivity?.binding?.appBarMain?.fabLeft?.tag as? EditMode
                                                    ?: EditMode.Fill
                                            when (editMode) {
                                                EditMode.Fill -> Cell.State.Fill
                                                else -> Cell.State.MarkNotFill
                                            }
                                        }
                                    }
                            )
                        }
                    }

                    val bitmap = algorithm.onEditCanvasImage((binding.canvas.drawable as BitmapDrawable).bitmap, cell, false)
                    binding.canvas.setImageBitmap(bitmap)
                }
                pointPrev0.set(pointCurrent)
            }
        }
        prevAction = event.action

        return true
    }

    private fun getMode(): TouchMode? =
            mainActivity?.binding?.appBarMain?.fabRight?.tag as? TouchMode

    private fun initCanvas(savedInstanceState: Bundle?) {
        this.problem?.let {
            savedInstanceState?.let { bundle ->
                ui(jobList) {
                    val bitmap = async {
                        val cells = bundle.getStringArrayList(CanvasUtil.BUNDLE_NAME_CELLS)?.map { App.gson.fromJson(it, Cell::class.java) }
                                ?: it.catalog.cells
                        algorithm.prevCells.apply {
                            clear()
                            addAll(cells)
                        }

                        this@SolveFragment.algorithm.setCells(cells)
                    }?.await()
                    binding.canvas.setImageBitmap(bitmap)
                }
            } ?: run {
                val cells = algorithm4ref.getSolution()
                val bitmap = algorithm.overrideKeysFromCells(cells)
                binding.canvas.setImageBitmap(bitmap)
            }
        } ?: run {}
    }

    private val mainActivity get() = requireActivity() as? MainActivity
}