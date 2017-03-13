package jp.co.seesaa.geckour.picrossmaker.fragment

import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.text.TextUtils
import android.util.Log
import android.util.Size
import android.view.*
import com.github.yamamotoj.pikkel.Pikkel
import com.github.yamamotoj.pikkel.PikkelDelegate
import com.trello.rxlifecycle2.components.RxFragment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import jp.co.seesaa.geckour.picrossmaker.Constant
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentEditorBinding
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import jp.co.seesaa.geckour.picrossmaker.model.DraftProblem
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider
import jp.co.seesaa.geckour.picrossmaker.model.Problem
import jp.co.seesaa.geckour.picrossmaker.util.Algorithm
import jp.co.seesaa.geckour.picrossmaker.util.CanvasUtil
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment
import timber.log.Timber
import java.util.*

class EditorFragment(): RxFragment(), Pikkel by PikkelDelegate() {
    lateinit var listener: IListener
    private val size by state(Point(0, 0))
    private var draftId by state(-1L)
    lateinit private var binding: FragmentEditorBinding
    private val pointPrev0 = PointF(-1f, -1f)
    private val pointPrev1 = PointF(-1f, -1f)
    private var isSolvable by state(true)
    lateinit private var algorithm: Algorithm

    interface IListener {
        fun onCanvasSizeError(size: Size)
    }

    constructor(listener: IListener): this() {
        this.listener = listener
    }

    companion object {
        fun newInstance(size: Size, listener: IListener): EditorFragment? {
            if (size.width < 1 || size.height < 1) {
                listener.onCanvasSizeError(size)
                return null
            }
            val fragment = EditorFragment(listener)
            val args = Bundle()
            args.putSize(Constant.ARGS_FRAGMENT_CANVAS_SIZE, size)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(id: Long, listener: IListener): EditorFragment? {
            if (id > 0) {
                val fragment = EditorFragment(listener)
                val args = Bundle()
                args.putLong(Constant.ARGS_FRAGMENT_DRAFT_ID, id)
                fragment.arguments = args
                return fragment
            }
            return null
        }

        val TAG = "editorFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        this.draftId = arguments.getLong(Constant.ARGS_FRAGMENT_DRAFT_ID, -1)
        if (this.draftId < 0) {
            val size = arguments.getSize(Constant.ARGS_FRAGMENT_CANVAS_SIZE)
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

        if (activity is MainActivity) {
            this.listener = (activity as MainActivity).editorFragmentListener
        }

        if (draftId > -1) {
            val title = OrmaProvider.db.selectFromDraftProblem().idEq(draftId).value().title
            (activity as MainActivity).supportActionBar?.setTitle(getString(R.string.action_bar_title_edit_with_title, title))
        } else {
            (activity as MainActivity).supportActionBar?.setTitle(R.string.action_bar_title_edit)
        }

        val fab = activity.findViewById(R.id.fab) as FloatingActionButton
        fab.tag = true
        fab.setImageResource(R.drawable.ic_crop_free_white_24px)
        fab.setOnClickListener {
            view ->
            run {
                val mode = view.tag as Boolean
                (view as FloatingActionButton).setImageResource(if (mode) R.drawable.ic_edit_white_24px else R.drawable.ic_crop_free_white_24px)
                view.tag = !mode
            }
        }

        binding.canvas.setOnTouchListener { view, event -> onTouchCanvas(event) }
        binding.cover.setOnTouchListener { view, event ->
            run {
                return@run if (!getMode()) {
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

        val nav = activity.findViewById(R.id.nav_view) as NavigationView
        nav.menu.findItem(R.id.nav_editor).isChecked = true
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
            R.id.action_settings -> {
                true
            }

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
        outState?.putParcelableArrayList(CanvasUtil.BUNDLE_NAME_CELLS, algorithm.cells)
        saveInstanceState(outState)
    }

    fun onScale (pointCurrent0: PointF, pointCurrent1: PointF): Boolean {
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

    fun onDrag(pointCurrent0: PointF): Boolean {
        if (pointPrev0.x < 0f) pointPrev0.set(pointCurrent0)
        val diff = algorithm.getPointDiff(pointPrev0, pointCurrent0)
        binding.canvas.translationX = binding.canvas.translationX.plus(diff.x)
        binding.canvas.translationY = binding.canvas.translationY.plus(diff.y)
        pointPrev0.set(pointCurrent0)
        return true
    }

    fun onTouchCanvas(event: MotionEvent): Boolean {
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

    fun getMode(): Boolean {
        return (activity.findViewById(R.id.fab) as FloatingActionButton).tag as Boolean
    }

    fun onRefresh(savedInstanceState: Bundle?) {
        if (this.draftId < 0) {
            this.algorithm = Algorithm(size)
            binding.canvas.setImageBitmap(algorithm.createCanvasImage())
        } else {
            val draftProblem = OrmaProvider.db.selectFromDraftProblem().idEq(this.draftId).value()
            this.size.set(draftProblem.keysVertical.keys.size, draftProblem.keysHorizontal.keys.size)
            this.algorithm = Algorithm(size)
            val bitmap = this.algorithm.setCells(
                    if (savedInstanceState == null) draftProblem.catalog.cells
                    else savedInstanceState.getParcelableArrayList(CanvasUtil.BUNDLE_NAME_CELLS))
            binding.canvas.setImageBitmap(bitmap)
        }
    }

    fun onSaveCanvas() {
        val fragment = MyAlertDialogFragment.Builder(object: MyAlertDialogFragment.IListener {
            override fun onResultAlertDialog(dialogInterface: DialogInterface, requestCode: Int, resultCode: Int, result: Any?) {
                when (resultCode) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        onPositive(requestCode, result)
                    }
                }
            }

            fun onPositive(requestCode: Int, result: Any?) {
                when (requestCode) {
                    MyAlertDialogFragment.Builder.REQUEST_CODE_SAVE_PROBLEM -> {
                        if (result != null && result is String && !TextUtils.isEmpty(result)) {
                            OrmaProvider.db.prepareInsertIntoProblemAsSingle()
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .map { inserter -> inserter.execute { createProblem(result) } }
                                    .compose(this@EditorFragment.bindToLifecycle<Long>())
                                    .subscribe({ id -> run {
                                        Snackbar.make(activity.findViewById(R.id.cover),
                                                R.string.editor_fragment_message_complete_save,
                                                Snackbar.LENGTH_SHORT).show()
                                    } }, { throwable -> run {
                                        throwable.printStackTrace()
                                        Snackbar.make(activity.findViewById(R.id.cover),
                                                R.string.editor_fragment_error_failure_save,
                                                Snackbar.LENGTH_SHORT).show()
                                    } })
                        } else {
                            showTitleErrorSnackbar()
                        }
                    }

                    MyAlertDialogFragment.Builder.REQUEST_CODE_SAVE_DRAFT_PROBLEM -> {
                        if (result != null && result is String && !TextUtils.isEmpty(result)) {
                            OrmaProvider.db.prepareInsertIntoDraftProblemAsSingle()
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .map { inserter -> inserter.execute { createDraftProblem(result) } }
                                    .compose(this@EditorFragment.bindToLifecycle<Long>())
                                    .subscribe({ id -> run {
                                        Snackbar.make(activity.findViewById(R.id.cover),
                                                R.string.editor_fragment_message_complete_save,
                                                Snackbar.LENGTH_SHORT).show()
                                    } }, { throwable -> run {
                                        throwable.printStackTrace()
                                        Snackbar.make(activity.findViewById(R.id.cover),
                                                R.string.editor_fragment_error_failure_save,
                                                Snackbar.LENGTH_SHORT).show()
                                    } })
                        } else {
                            showTitleErrorSnackbar()
                        }
                    }
                }
            }

            fun showTitleErrorSnackbar() {
                Snackbar.make(activity.findViewById(R.id.cover),
                        R.string.editor_fragment_error_invalid_title,
                        Snackbar.LENGTH_SHORT).show()
            }
        }, this)
                .setLayout(R.layout.dialog_define_title)
                .setTitle(getString(if (isSolvable) R.string.dialog_alert_title_save_problem else R.string.dialog_alert_title_save_draft_problem))
                .setMessage(getString(if (isSolvable) R.string.dialog_alert_message_save_problem else R.string.dialog_alert_message_save_draft_problem))
                .setRequestCode(if (isSolvable) MyAlertDialogFragment.Builder.REQUEST_CODE_SAVE_PROBLEM else MyAlertDialogFragment.Builder.REQUEST_CODE_SAVE_DRAFT_PROBLEM)
                .setCancelable(true)
                .commit()

        fragment.show(fragmentManager, if (isSolvable) MyAlertDialogFragment.Builder.TAG_SAVE_PROBLEM else MyAlertDialogFragment.Builder.TAG_SAVE_DRAFT_PROBLEM)
    }

    fun createProblem(title: String): Problem {
        val thumb = algorithm.getThumbnailImage()
        val keysInRow: ArrayList<List<Int>> = ArrayList()
        (0..algorithm.size.y - 1).forEach {
            keysInRow.add(algorithm.getKeys(algorithm.getCellsInRow(it) ?: return@forEach))
        }
        val keysInColumn: ArrayList<List<Int>> = ArrayList()
        (0..algorithm.size.x - 1).forEach {
            keysInColumn.add(algorithm.getKeys(algorithm.getCellsInColumn(it) ?: return@forEach))
        }

        return Problem(-1L, title, Problem.Companion.KeysCluster(*(keysInRow.toTypedArray())), Problem.Companion.KeysCluster(*(keysInColumn.toTypedArray())), thumb)
    }

    fun createDraftProblem(title: String): DraftProblem {
        val thumb = algorithm.getThumbnailImage()
        val keysInRow: ArrayList<List<Int>> = ArrayList()
        (0..algorithm.size.y - 1).forEach {
            keysInRow.add(algorithm.getKeys(algorithm.getCellsInRow(it) ?: return@forEach))
        }
        val keysInColumn: ArrayList<List<Int>> = ArrayList()
        (0..algorithm.size.x - 1).forEach {
            keysInColumn.add(algorithm.getKeys(algorithm.getCellsInColumn(it) ?: return@forEach))
        }

        return DraftProblem(-1L, title, Problem.Companion.KeysCluster(*(keysInRow.toTypedArray())), Problem.Companion.KeysCluster(*(keysInColumn.toTypedArray())), thumb, Cell.Companion.Catalog(algorithm.cells))
    }
}