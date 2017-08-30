package jp.co.seesaa.geckour.picrossmaker.fragment

import android.content.Context
import android.content.DialogInterface
import android.databinding.DataBindingUtil
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
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import jp.co.seesaa.geckour.picrossmaker.*
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentEditorBinding
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import jp.co.seesaa.geckour.picrossmaker.model.DraftProblem
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider
import jp.co.seesaa.geckour.picrossmaker.model.Problem
import jp.co.seesaa.geckour.picrossmaker.util.Algorithm
import jp.co.seesaa.geckour.picrossmaker.util.CanvasUtil
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment.Companion.showSnackbar
import kotlinx.coroutines.experimental.Job
import kotlin.collections.ArrayList

class EditorFragment: RxFragment(), MyAlertDialogFragment.IListener, Pikkel by PikkelDelegate() {
    private var listener: IListener? = null
    private val size by state(Point(0, 0))
    private var draftId by state(-1L)
    private var problemId by state(-1L)
    lateinit private var binding: FragmentEditorBinding
    private val pointPrev0 = PointF(-1f, -1f)
    private val pointPrev1 = PointF(-1f, -1f)
    private var isSolvable by state(true)
    lateinit private var algorithm: Algorithm
    private val jobList: ArrayList<Job> = ArrayList()

    interface IListener {
        fun onCanvasSizeError(size: Size)
    }

    enum class ArgKeys {
        CANVAS_SIZE,
        PROBLEM_ID,
        DRAFT_ID
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

        fun newInstance(id: Long, argKey: ArgKeys): EditorFragment? {
            if (id > 0) {
                return EditorFragment().apply {
                    arguments = Bundle().apply {
                        putLong(argKey.name, id)
                    }
                }
            }
            return null
        }

        val TAG = this::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        this.draftId = if (arguments.containsKey(ArgKeys.DRAFT_ID.name)) arguments.getLong(ArgKeys.DRAFT_ID.name, -1) else -1
        this.problemId = if (arguments.containsKey(ArgKeys.PROBLEM_ID.name)) arguments.getLong(ArgKeys.PROBLEM_ID.name, -1) else -1
        if (this.draftId < 0 && this.problemId < 0 && arguments.containsKey(ArgKeys.CANVAS_SIZE.name)) {
            val size = arguments.getSize(ArgKeys.CANVAS_SIZE.name)
            this.size.set(size.width, size.height)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_editor, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onRefresh(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        when {
            draftId > -1 -> {
                ui(jobList) {
                    val draftProblem = async { OrmaProvider.db.selectFromDraftProblem().idEq(draftId).valueOrNull() }.await()
                    draftProblem?.title?.let { (activity as? MainActivity)?.actionBar?.setTitle(getString(R.string.action_bar_title_edit_with_title, it)) }
                }
            }
            problemId > -1 -> {
                ui(jobList) {
                    val problem = async { OrmaProvider.db.selectFromProblem().idEq(problemId).valueOrNull() }.await()
                    problem?.title?.let { (activity as? MainActivity)?.actionBar?.setTitle(getString(R.string.action_bar_title_edit_with_title, it)) }
                }
            }
            else -> (activity as? MainActivity)?.actionBar?.setTitle(R.string.action_bar_title_edit)
        }

        (activity as MainActivity).binding.appBarMain.fab
                .apply {
                    tag = true
                    setImageResource(R.drawable.ic_crop_free_white_24px)
                    setOnClickListener {
                        val mode = it.tag as Boolean
                        (it as FloatingActionButton).setImageResource(if (mode) R.drawable.ic_edit_white_24px else R.drawable.ic_crop_free_white_24px)
                        it.tag = !mode
                    }
                }

        binding.canvas.setOnTouchListener { _, event -> onTouchCanvas(event) }
        binding.cover.setOnTouchListener { _, event ->
            return@setOnTouchListener if (!getMode()) {
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

    override fun onPause() {
        super.onPause()
        jobList.apply {
            forEach { it.cancel() }
            clear()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        menu?.clear()
        inflater?.inflate(R.menu.editor, menu)
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
        }
    }

    private fun onPositive(requestCode: MyAlertDialogFragment.RequestCode, result: Any?) {
        when (requestCode) {
            MyAlertDialogFragment.RequestCode.SAVE_PROBLEM -> {
                (result as? String)?.let {
                    if (it.isNotEmpty()) {
                        ui(jobList, {
                            showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_failure_save)
                        }) {
                            async { OrmaProvider.db.insertIntoProblem(createProblem(it)) }.await()
                            showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_message_complete_save)
                        }
                    } else null
                } ?: showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_invalid_title)
            }

            MyAlertDialogFragment.RequestCode.SAVE_DRAFT_PROBLEM -> {
                (result as? String)?.let {
                    if (it.isNotEmpty()) {
                        ui(jobList, {
                            showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_failure_save)
                        }) {
                            async { OrmaProvider.db.insertIntoDraftProblem(createDraftProblem(it)) }.await()
                            showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_message_complete_save)
                        }
                    } else null
                } ?: showSnackbar(activity.findViewById(R.id.container), R.string.editor_fragment_error_invalid_title)
            }

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

    private fun onTouchCanvas(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                pointPrev0.set(-1f, -1f)
                pointPrev1.set(-1f, -1f)

                Observable.just(algorithm.isSolvable())
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .compose(this.bindToLifecycle<Boolean>())
                        .subscribe { b -> isSolvable = b }
            }

            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                if (event.action == MotionEvent.ACTION_MOVE) isSolvable = false

                val pointCurrent = PointF(event.x, event.y)
                val coordCurrent = algorithm.getCoordinateFromTouchPoint(
                        binding.canvas,
                        pointCurrent) ?: return true
                val coordPrev = algorithm.getCoordinateFromTouchPoint(
                        binding.canvas,
                        pointPrev0) ?: Point(-1, -1)
                if (!coordCurrent.equals(coordPrev.x, coordPrev.y)) {
                    val cell = algorithm.getCellByCoordinate(coordCurrent) ?: return true

                    if (event.action == MotionEvent.ACTION_MOVE) {
                        val cellPrev = algorithm.getCellByCoordinate(coordPrev) ?: run {
                            pointPrev0.set(-1f, -1f)
                            return true
                        }
                        cell.setState(cellPrev.getState())
                    } else {
                        cell.setState(!cell.getState())
                    }

                    val bitmap = algorithm.onEditCanvasImage((binding.canvas.drawable as BitmapDrawable).bitmap, cell, true)
                    binding.canvas.setImageBitmap(bitmap)
                }
                pointPrev0.set(pointCurrent)
            }
        }

        return true
    }

    private fun getMode(): Boolean =
            (activity as MainActivity).binding.appBarMain.fab.tag as Boolean

    private fun onRefresh(savedInstanceState: Bundle?) {
        when {
            this.draftId > -1 -> {
                ui(jobList) {
                    val bitmap = async {
                        OrmaProvider.db.selectFromDraftProblem().idEq(this@EditorFragment.draftId).valueOrNull()?.let {
                            this@EditorFragment.size.set(it.keysVertical.keys.size, it.keysHorizontal.keys.size)
                            this@EditorFragment.algorithm = Algorithm(size)
                            return@async this@EditorFragment.algorithm.setCells(
                                    savedInstanceState?.getStringArrayList(CanvasUtil.BUNDLE_NAME_CELLS)?.map { App.gson.fromJson(it, Cell::class.java) } ?: it.catalog.cells)
                        }
                    }.await()
                    binding.canvas.setImageBitmap(bitmap)
                }

            }
            this.problemId > -1 -> {
                ui(jobList) {
                    val bitmap = async {
                        OrmaProvider.db.selectFromProblem().idEq(this@EditorFragment.problemId).valueOrNull()?.let {
                            this@EditorFragment.size.set(it.keysVertical.keys.size, it.keysHorizontal.keys.size)
                            this@EditorFragment.algorithm = Algorithm(size)
                            return@async this@EditorFragment.algorithm.setCells(savedInstanceState?.getStringArrayList(CanvasUtil.BUNDLE_NAME_CELLS)?.map { App.gson.fromJson(it, Cell::class.java) } ?: it.catalog.cells)
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
    }

    private fun onSaveCanvas() {
        val requestCode = if (isSolvable) MyAlertDialogFragment.RequestCode.SAVE_PROBLEM else MyAlertDialogFragment.RequestCode.SAVE_DRAFT_PROBLEM
        val fragment = MyAlertDialogFragment.newInstance(
                resId = R.layout.dialog_define_title,
                title = getString(if (isSolvable) R.string.dialog_alert_title_save_problem else R.string.dialog_alert_title_save_draft_problem),
                message = getString(if (isSolvable) R.string.dialog_alert_message_save_problem else R.string.dialog_alert_message_save_draft_problem),
                requestCode = requestCode,
                cancelable = true,
                targetFragment = this
        )

        (activity as? MainActivity)?.let {
            fragment.show(it.fragmentManager, MyAlertDialogFragment.getTag(requestCode))
        }
    }

    private fun createProblem(title: String): Problem {
        val thumb = algorithm.getThumbnailImage()
        val keysInRow: ArrayList<List<Int>> = ArrayList()
        (0 until algorithm.size.y).forEach {
            keysInRow.add(algorithm.getKeys(algorithm.getCellsInRow(it) ?: return@forEach))
        }
        val keysInColumn: ArrayList<List<Int>> = ArrayList()
        (0 until algorithm.size.x).forEach {
            keysInColumn.add(algorithm.getKeys(algorithm.getCellsInColumn(it) ?: return@forEach))
        }

        return Problem(-1L, title, Problem.KeysCluster(*(keysInRow.toTypedArray())), Problem.KeysCluster(*(keysInColumn.toTypedArray())), thumb, catalog = Cell.Catalog(algorithm.cells))
    }

    private fun createDraftProblem(title: String): DraftProblem {
        val thumb = algorithm.getThumbnailImage()
        val keysInRow: ArrayList<List<Int>> = ArrayList()
        (0 until algorithm.size.y).forEach {
            keysInRow.add(algorithm.getKeys(algorithm.getCellsInRow(it) ?: return@forEach))
        }
        val keysInColumn: ArrayList<List<Int>> = ArrayList()
        (0 until algorithm.size.x).forEach {
            keysInColumn.add(algorithm.getKeys(algorithm.getCellsInColumn(it) ?: return@forEach))
        }

        return DraftProblem(-1L, title, Problem.KeysCluster(*(keysInRow.toTypedArray())), Problem.KeysCluster(*(keysInColumn.toTypedArray())), thumb, catalog = Cell.Catalog(algorithm.cells))
    }
}