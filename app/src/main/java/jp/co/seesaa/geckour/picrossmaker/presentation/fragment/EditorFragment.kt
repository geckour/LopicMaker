package jp.co.seesaa.geckour.picrossmaker.presentation.fragment

import android.content.Context
import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.util.Size
import android.view.*
import com.github.yamamotoj.pikkel.Pikkel
import com.github.yamamotoj.pikkel.PikkelDelegate
import com.trello.rxlifecycle2.components.RxFragment
import jp.co.seesaa.geckour.picrossmaker.*
import jp.co.seesaa.geckour.picrossmaker.presentation.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentEditorBinding
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider
import jp.co.seesaa.geckour.picrossmaker.model.Problem
import jp.co.seesaa.geckour.picrossmaker.util.*
import kotlinx.coroutines.experimental.Job
import org.sat4j.specs.TimeoutException
import timber.log.Timber
import java.sql.Timestamp
import kotlin.collections.ArrayList

class EditorFragment: RxFragment(), MyAlertDialogFragment.IListener, Pikkel by PikkelDelegate() {
    private var listener: IListener? = null
    private val size by state(Point(0, 0))
    private var problemId by state(-1L)
    private var problem: Problem? = null
    private var draft by state(true)
    private lateinit var binding: FragmentEditorBinding
    private val pointPrev0 = PointF(-1f, -1f)
    private val pointPrev1 = PointF(-1f, -1f)
    private var satisfactionState by state(Algorithm.SatisfactionState.Unsatisfiable)
    private val algorithm: Algorithm by lazy { Algorithm(size) }
    private val jobList: ArrayList<Job> = ArrayList()
    private var menu: Menu? = null

    interface IListener {
        fun onCanvasSizeError(size: Size)
    }

    enum class ArgKeys {
        CANVAS_SIZE,
        PROBLEM_ID,
        DRAFT
    }

    enum class Mode {
        Edit,
        Scale
    }

    companion object {
        fun newInstance(size: Size, listener: IListener): EditorFragment? {
            if (size.width < 1 || size.height < 1) {
                listener.onCanvasSizeError(size)
                return null
            }

            return EditorFragment().apply {
                arguments = Bundle().apply { putSize(ArgKeys.CANVAS_SIZE.name, size) }
            }
        }

        fun newInstance(vararg args: Pair<ArgKeys, Any>): EditorFragment? =
                if ((args.find { it.first == ArgKeys.PROBLEM_ID }?.second as? Long)?.let { it > -1 } == true) {
                    EditorFragment().apply {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        this.problemId = if (arguments.containsKey(ArgKeys.PROBLEM_ID.name)) arguments.getLong(ArgKeys.PROBLEM_ID.name, -1) else -1

        this.draft = if (arguments.containsKey(ArgKeys.DRAFT.name)) arguments.getBoolean(ArgKeys.DRAFT.name, true) else true
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_editor, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!arguments.containsKey(ArgKeys.CANVAS_SIZE.name) && problemId > -1) {
            ui(jobList) {
                this@EditorFragment.problem = async { OrmaProvider.db.selectFromProblem().idEq(problemId).valueOrNull() }.await()
                this@EditorFragment.problem?.apply {
                    size.set(this.keysHorizontal.keys.size, this.keysVertical.keys.size)
                    (activity as? MainActivity)?.supportActionBar?.title = getString(R.string.action_bar_title_edit_with_title, title)
                } ?: run { (activity as? MainActivity)?.supportActionBar?.setTitle(R.string.action_bar_title_edit) }

                initCanvas(savedInstanceState)
            }
        } else {
            arguments.getSize(ArgKeys.CANVAS_SIZE.name).apply { size.set(this.width, this.height) }

            initCanvas(savedInstanceState)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.canvas.setOnTouchListener { _, event -> onTouchCanvas(event) }
        binding.cover.setOnTouchListener { _, event ->
            return@setOnTouchListener if (getMode() == Mode.Scale) {
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
                        }
                        else onDrag(p0)
                    }
                }
            } else false
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        listener = activity as? IListener
    }

    override fun onResume() {
        super.onResume()

        mainActivity()?.binding?.appBarMain?.fabRight?.apply {
            tag = Mode.Edit
            setImageResource(R.drawable.ic_crop_free_white_24px)
            setOnClickListener {
                val mode = tag as Mode
                (it as FloatingActionButton).setImageResource(
                        when (mode) {
                            Mode.Edit -> R.drawable.ic_edit_white_24px
                            Mode.Scale -> R.drawable.ic_crop_free_white_24px
                        })
                tag = when (mode) {
                    Mode.Edit -> Mode.Scale
                    Mode.Scale -> Mode.Edit
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
        mainActivity()?.binding?.appBarMain?.fabLeft?.hide()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        menu?.clear()
        inflater?.inflate(R.menu.editor, menu)

        this.menu = menu

        refreshSaveMenuIcon()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        return when (id) {
            R.id.action_save -> {
                onSaveCanvas()
                true
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putStringArrayList(CanvasUtil.BUNDLE_NAME_CELLS, ArrayList(algorithm.cells.map { App.gson.toJson(it) }))
        saveInstanceState(outState)
    }

    override fun onResultAlertDialog(dialogInterface: DialogInterface, requestCode: MyAlertDialogFragment.RequestCode, resultCode: Int, result: Any?) {
        when (resultCode) {
            DialogInterface.BUTTON_POSITIVE -> {
                onPositive(requestCode, result)
            }

            DialogInterface.BUTTON_NEUTRAL -> {
                onNeutral(requestCode, result)
            }
        }
    }

    private fun onPositive(requestCode: MyAlertDialogFragment.RequestCode, result: Any?) {
        when (requestCode) {
            // 問題を保存
            MyAlertDialogFragment.RequestCode.SAVE_PROBLEM -> {
                (result as? String)?.let {
                    if (it.isNotEmpty()) {
                        ui(jobList, { showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_failure_save) }) {
                            Timber.d("result: $it")
                            createProblem(it, false)?.apply {
                                async { OrmaProvider.db.insertIntoProblem(this@apply) }.await()
                                showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_message_complete_save)
                            }
                        }
                    } else null
                } ?: showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_invalid_title)
            }

            // 下書きを保存
            MyAlertDialogFragment.RequestCode.SAVE_DRAFT_PROBLEM -> {
                (result as? String)?.let {
                    if (it.isNotEmpty()) {
                        ui(jobList, { showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_failure_save) }) {
                            createProblem(it, true)?.apply {
                                async { OrmaProvider.db.insertIntoProblem(this@apply) }.await()
                                showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_message_complete_save)
                            }
                        }
                    } else null
                } ?: showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_invalid_title)
            }

            // 上書きして問題 / 下書きを保存
            MyAlertDialogFragment.RequestCode.CONFIRM_BEFORE_SAVE -> {
                (result as? String)?.let {
                    if (it.isNotEmpty()) {
                        ui(jobList, { showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_failure_save) }) {
                            val specifyData: MyAlertDialogFragment.ProblemSpecifyData? =
                                    try { App.gson.fromJson(it) }
                                    catch (e: Exception) {
                                        Timber.e(e)
                                        null
                                    }
                            this@EditorFragment.problem?.apply {
                                draft = satisfactionState != Algorithm.SatisfactionState.Satisfiable
                                editedAt = Timestamp(System.currentTimeMillis())
                                specifyData?.let {
                                    title = it.title
                                    tags = it.tags
                                }

                                async { OrmaProvider.db.relationOfProblem().upsert(this@apply) }.await()
                                showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_message_complete_save)
                            }
                        }
                    } else null
                } ?: showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_invalid_title)
            }

            else -> {}
        }
    }

    private fun onNeutral(requestCode: MyAlertDialogFragment.RequestCode, result: Any?) =
            when (requestCode) {
                MyAlertDialogFragment.RequestCode.CONFIRM_BEFORE_SAVE -> onSaveCanvas(result as String?, false)
                else -> {}
            }

    private fun onScale (pointCurrent0: PointF, pointCurrent1: PointF): Boolean {
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

                showUndoButtonIfAvailable()
                refreshSaveMenuIcon()
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
                                        Cell.State.Fill -> Cell.State.Blank
                                        Cell.State.Blank -> Cell.State.Fill
                                        else -> state
                                    }
                            )
                        }
                    }

                    val bitmap = algorithm.onEditCanvasImage((binding.canvas.drawable as BitmapDrawable).bitmap, cell, true)
                    binding.canvas.setImageBitmap(bitmap)
                }
                pointPrev0.set(pointCurrent)
            }
        }
        prevAction = event.action

        return true
    }

    private fun getMode(): Mode? =
            mainActivity()?.binding?.appBarMain?.fabRight?.tag as? Mode

    private fun initCanvas(savedInstanceState: Bundle?) {
        if (this.problemId > -1) {
            ui(jobList) {
                val bitmap = async {
                    this@EditorFragment.problem?.let {
                        val cells = savedInstanceState?.getStringArrayList(CanvasUtil.BUNDLE_NAME_CELLS)?.map { App.gson.fromJson(it, Cell::class.java) } ?: it.catalog.cells
                        algorithm.prevCells.apply {
                            clear()
                            addAll(cells)
                        }

                        this@EditorFragment.algorithm.setCells(cells)
                    }
                }.await()
                binding.canvas.setImageBitmap(bitmap)
            }
        } else binding.canvas.setImageBitmap(algorithm.createCanvasImage())
    }

    private fun onSaveCanvas(optional: String? = null, overwrite: Boolean? = null) {
        val requireOverwrite = overwrite ?: (problemId >= 0)
        val requestCode =
                when {
                    requireOverwrite -> MyAlertDialogFragment.RequestCode.CONFIRM_BEFORE_SAVE
                    else -> {
                        if (satisfactionState == Algorithm.SatisfactionState.Satisfiable) MyAlertDialogFragment.RequestCode.SAVE_PROBLEM
                        else MyAlertDialogFragment.RequestCode.SAVE_DRAFT_PROBLEM
                    }
                }
        ui(jobList) {
            val inheritOptional = async { OrmaProvider.db.selectFromProblem().idEq(problemId).valueOrNull() }.await()?.let {
                App.gson.toJson(MyAlertDialogFragment.ProblemSpecifyData(it.title, it.tags))
            }
            showDialog(requireOverwrite, inheritOptional ?: optional, requestCode)
        }
    }

    private fun showDialog(requireOverwrite: Boolean, optional: String?, requestCode: MyAlertDialogFragment.RequestCode) {
        val fragment = MyAlertDialogFragment.newInstance(
                resId = if (requireOverwrite) null else R.layout.dialog_define_title_and_tags,
                title = getString(
                        if (requireOverwrite) R.string.dialog_alert_title_overwrite
                        else {
                            if (satisfactionState == Algorithm.SatisfactionState.Satisfiable) R.string.dialog_alert_title_save_problem
                            else R.string.dialog_alert_title_save_draft_problem
                        }),
                optional = optional,
                message = getString(
                        if (requireOverwrite) R.string.dialog_alert_message_confirm_before_save
                        else {
                            when (satisfactionState) {
                                Algorithm.SatisfactionState.Satisfiable -> R.string.dialog_alert_message_save_problem
                                Algorithm.SatisfactionState.ExistMultipleSolution -> R.string.dialog_alert_message_save_draft_problem_exist_multiple_solution
                                else -> R.string.dialog_alert_message_save_draft_problem_unsatisfiable
                            }
                        }
                ),
                requestCode = requestCode,
                cancelable = true,
                targetFragment = this
        )

        (activity as? MainActivity)?.let {
            fragment.show(it.fragmentManager, MyAlertDialogFragment.getTag(requestCode))
        }
    }

    private fun createProblem(data: String, draft: Boolean = this.draft, id: Long = -1L): Problem? {
        val specifyData: MyAlertDialogFragment.ProblemSpecifyData =
                try { App.gson.fromJson(data) }
                catch (e: Exception) {
                    Timber.e(e)
                    return null
                }

        val thumb = algorithm.getThumbnailImage()
        val keysInRow: ArrayList<List<Int>> = ArrayList()
        (0 until algorithm.size.y).forEach {
            keysInRow.add(algorithm.getKeys(algorithm.getCellsInRow(it) ?: return@forEach))
        }
        val keysInColumn: ArrayList<List<Int>> = ArrayList()
        (0 until algorithm.size.x).forEach {
            keysInColumn.add(algorithm.getKeys(algorithm.getCellsInColumn(it) ?: return@forEach))
        }

        return Problem(
                id,
                specifyData.title,
                draft,
                specifyData.tags,
                Problem.KeysCluster(*(keysInRow.toTypedArray())),
                Problem.KeysCluster(*(keysInColumn.toTypedArray())),
                thumb,
                catalog = Cell.Catalog(algorithm.cells)
        )
    }

    private fun showUndoButtonIfAvailable() {
        if (algorithm.prevCells.isNotEmpty()) {
            mainActivity()?.binding?.appBarMain?.fabLeft?.apply {
                setImageResource(R.drawable.ic_undo_black_24px)
                setOnClickListener {
                    val bitmap = algorithm.setCells(algorithm.prevCells)
                    algorithm.prevCells.clear()
                    binding.canvas.setImageBitmap(bitmap)
                    hide()
                }
                show()
            }
        }
    }

    private fun refreshSaveMenuIcon() {
        this.menu?.apply {
            ui(jobList) {
                satisfactionState = algorithm.getSolutionCounter()?.let {
                    val count =
                            try { async { it.countSolutions() }.await() }
                            catch (e: TimeoutException) { it.lowerBound() }
                            finally { -1L }

                    algorithm.getSatisfactionState(count)
                } ?: let {
                    Timber.d("solutionCounter is null")
                    Algorithm.SatisfactionState.Unsatisfiable
                }
                findItem(R.id.action_save)?.apply {
                    icon = activity.getDrawable(
                            if (satisfactionState == Algorithm.SatisfactionState.Satisfiable) R.drawable.ic_save_white_24px
                            else R.drawable.ic_bookmark_black_24px
                    ).apply {
                        setTint(Color.WHITE)
                    }
                }
            }
        } ?: Timber.d("this.menu is null")
    }
}