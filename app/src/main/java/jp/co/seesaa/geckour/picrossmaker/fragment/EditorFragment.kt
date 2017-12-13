package jp.co.seesaa.geckour.picrossmaker.fragment

import android.content.Context
import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.util.Log
import android.util.Size
import android.view.*
import com.github.yamamotoj.pikkel.Pikkel
import com.github.yamamotoj.pikkel.PikkelDelegate
import com.trello.rxlifecycle2.components.RxFragment
import jp.co.seesaa.geckour.picrossmaker.*
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
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
    private var draft by state(true)
    lateinit private var binding: FragmentEditorBinding
    private val pointPrev0 = PointF(-1f, -1f)
    private val pointPrev1 = PointF(-1f, -1f)
    private var satisfactionState by state(Algorithm.SatisfactionState.Unsatisfiable)
    lateinit private var algorithm: Algorithm
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
                arguments = Bundle().apply {
                    putSize(ArgKeys.CANVAS_SIZE.name, size)
                }
            }
        }

        fun newInstance(vararg args: Pair<ArgKeys, Any>): EditorFragment? {
            if (args.map { it.first }.contains(ArgKeys.PROBLEM_ID)) {
                if ((args.find { it.first == ArgKeys.PROBLEM_ID }?.second as? Long)?.let { it > -1 } == true)
                return EditorFragment().apply {
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
            }

            return null
        }

        val TAG: String = this::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        this.problemId = if (arguments.containsKey(ArgKeys.PROBLEM_ID.name)) arguments.getLong(ArgKeys.PROBLEM_ID.name, -1) else -1
        this.draft = if (arguments.containsKey(ArgKeys.DRAFT.name)) arguments.getBoolean(ArgKeys.DRAFT.name, true) else true
        if (this.problemId < 0 && arguments.containsKey(ArgKeys.CANVAS_SIZE.name)) {
            val size = arguments.getSize(ArgKeys.CANVAS_SIZE.name)
            this.size.set(size.width, size.height)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_editor, container, false)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        when {
            problemId > -1 -> {
                ui(jobList) {
                    val problem = async { OrmaProvider.db.selectFromProblem().idEq(problemId).valueOrNull() }.await()
                    problem?.title?.let { (activity as? MainActivity)?.actionBar?.setTitle(getString(R.string.action_bar_title_edit_with_title, it)) }
                }
            }
            else -> (activity as? MainActivity)?.actionBar?.setTitle(R.string.action_bar_title_edit)
        }

        onRefresh(savedInstanceState)

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

        (activity as MainActivity).binding.navView.menu.findItem(R.id.nav_editor).isChecked = true
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
            // タイトルの重複チェック後問題を保存
            MyAlertDialogFragment.RequestCode.SAVE_PROBLEM -> {
                (result as? String)?.let {
                    if (it.isNotEmpty()) {
                        ui(jobList, { showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_failure_save) }) {
                            async { OrmaProvider.db.selectFromProblem().titleEq(it).lastOrNull() }.await()?.let {
                                onSaveCanvas(true, result as String?)
                            } ?: run {
                                async { OrmaProvider.db.insertIntoProblem(createProblem(it, false)) }.await()
                                showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_message_complete_save)
                            }
                        }
                    } else null
                } ?: showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_invalid_title)
            }

            // タイトルの重複チェック後下書きを保存
            MyAlertDialogFragment.RequestCode.SAVE_DRAFT_PROBLEM -> {
                (result as? String)?.let {
                    if (it.isNotEmpty()) {
                        ui(jobList, { showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_failure_save) }) {
                            async { OrmaProvider.db.selectFromProblem().titleEq(it).lastOrNull() }.await()?.let {
                                onSaveCanvas(true, result as String?)
                            } ?: run {
                                async { OrmaProvider.db.insertIntoProblem(createProblem(it, true)) }.await()
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
                        ui(jobList, {
                            showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_failure_save)
                        }) {
                            val problem =
                                    async { OrmaProvider.db.selectFromProblem().titleEq(it).lastOrNull() }.await()?.apply {
                                        draft = satisfactionState != Algorithm.SatisfactionState.Satisfiable
                                        editedAt = Timestamp(System.currentTimeMillis())
                                    }

                            problem?.apply {
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

    private fun onNeutral(requestCode: MyAlertDialogFragment.RequestCode, result: Any?) {
        when (requestCode) {
            MyAlertDialogFragment.RequestCode.CONFIRM_BEFORE_SAVE -> onSaveCanvas(optional = result as String?)
            else -> {}
        }
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
                        else -> cell.setState(!cell.getState())
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

    private fun onRefresh(savedInstanceState: Bundle?) {
        when {
            this.problemId > -1 -> {
                ui(jobList) {
                    val bitmap = async {
                        OrmaProvider.db.selectFromProblem().idEq(this@EditorFragment.problemId).valueOrNull()?.let {
                            this@EditorFragment.size.set(it.keysVertical.keys.size, it.keysHorizontal.keys.size)
                            this@EditorFragment.algorithm = Algorithm(size)
                            val cells = savedInstanceState?.getStringArrayList(CanvasUtil.BUNDLE_NAME_CELLS)?.map { App.gson.fromJson(it, Cell::class.java) } ?: it.catalog.cells
                            algorithm.prevCells.apply {
                                clear()
                                addAll(cells)
                            }
                            return@async this@EditorFragment.algorithm.setCells(cells)
                        }
                    }.await()
                    binding.canvas.setImageBitmap(bitmap)
                }

            }
            else -> {
                this.algorithm = Algorithm(size)
                binding.canvas.setImageBitmap(algorithm.createCanvasImage())
            }
        }

        refreshSaveMenuIcon()
    }

    private fun onSaveCanvas(requireRename: Boolean = false, optional: String? = null) {
        val requestCode = when {
            requireRename -> MyAlertDialogFragment.RequestCode.CONFIRM_BEFORE_SAVE
            else -> {
                if (satisfactionState == Algorithm.SatisfactionState.Satisfiable) MyAlertDialogFragment.RequestCode.SAVE_PROBLEM
                else MyAlertDialogFragment.RequestCode.SAVE_DRAFT_PROBLEM
            }
        }
        val fragment = MyAlertDialogFragment.newInstance(
                resId = if (requireRename) null else R.layout.dialog_define_title,
                title = getString(
                        when {
                            requireRename -> R.string.dialog_alert_title_overwrite
                            else -> {
                                if (satisfactionState == Algorithm.SatisfactionState.Satisfiable) R.string.dialog_alert_title_save_problem
                                else R.string.dialog_alert_title_save_draft_problem
                            }
                        }),
                optional = optional,
                message = getString(
                        if (requireRename) R.string.dialog_alert_message_confirm_before_save
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

    private fun createProblem(title: String, draft: Boolean = this.draft, id: Long = -1L, genres: List<String> = listOf()): Problem {
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
                title,
                draft,
                genres,
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
            ui(jobList, {
                if (it is UninitializedPropertyAccessException) {
                    findItem(R.id.action_save)?.icon = activity.getDrawable(R.drawable.ic_save_white_24px).apply { setTint(Color.WHITE) }
                }
            }) {
                satisfactionState = algorithm.getSolutionCounter()?.let {
                    val count =
                            try { async { it.countSolutions() }.await() }
                            catch (e: TimeoutException) { it.lowerBound() }
                            finally { -1L }

                    algorithm.getSatisfactionState(count)
                } ?: let {
                    Log.d("refreshSaveMenuIcon", "solutionCounter is null")
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
        } ?: Log.d("refreshSaveMenuIcon", "this.menu is null")
    }
}