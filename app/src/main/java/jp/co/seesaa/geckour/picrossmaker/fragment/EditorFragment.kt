package jp.co.seesaa.geckour.picrossmaker.fragment

import android.databinding.DataBindingUtil
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.util.Log
import android.util.Size
import android.view.*
import com.trello.rxlifecycle2.components.RxFragment
import jp.co.seesaa.geckour.picrossmaker.Constant
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentEditorBinding
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import jp.co.seesaa.geckour.picrossmaker.util.Algorithm
import timber.log.Timber
import java.util.*

class EditorFragment(listener: IListener): RxFragment() {
    var listener: IListener? = null
    private var size = Point(0, 0)
    private var binding: FragmentEditorBinding? = null
    private val pointPrev0 = PointF(-1f, -1f)
    private val pointPrev1 = PointF(-1f, -1f)
    private var isSolvable = true
    private var algorythm = Algorithm(size)

    interface IListener {
        fun onCanvasSizeError(size: Size)
    }

    init {
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

        val TAG = "editorFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val size = arguments.getSize(Constant.ARGS_FRAGMENT_CANVAS_SIZE)
        this.size.set(size.width, size.height)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_editor, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onRefresh()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as MainActivity).supportActionBar?.setTitle(R.string.action_bar_title_edit)

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

        binding?.canvas?.setOnTouchListener { view, event -> onTouchCanvas(event) }
        binding?.cover?.setOnTouchListener { view, event ->
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
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

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
                if (isSolvable) {
                    onSaveCanvas()
                }
                true
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    fun onScale (pointCurrent0: PointF, pointCurrent1: PointF): Boolean {
        if (pointPrev0.x < 0f) pointPrev0.set(pointCurrent0)
        if (pointPrev1.x < 0f) pointPrev1.set(pointCurrent1)
        val scale = algorythm.getScale(algorythm.getPointDiff(pointPrev0, pointPrev1).length(), algorythm.getPointDiff(pointCurrent0, pointCurrent1).length())
        val pointMidPrev = algorythm.getPointMid(pointPrev0, pointPrev1)
        val pointMidCurrent = algorythm.getPointMid(pointCurrent0, pointCurrent1)
        val diff = algorythm.getPointDiff(pointMidPrev, pointMidCurrent)
        binding?.canvas?.translationX = binding?.canvas?.translationX?.plus(diff.x) ?: 0f
        binding?.canvas?.translationY = binding?.canvas?.translationY?.plus(diff.y) ?: 0f
        binding?.canvas?.scaleX = binding?.canvas?.scaleX?.times(scale) ?: 1f
        binding?.canvas?.scaleY = binding?.canvas?.scaleY?.times(scale) ?: 1f
        pointPrev0.set(pointCurrent0)
        pointPrev1.set(pointCurrent1)
        return true
    }

    fun onDrag(pointCurrent0: PointF): Boolean {
        if (pointPrev0.x < 0f) pointPrev0.set(pointCurrent0)
        val diff = algorythm.getPointDiff(pointPrev0, pointCurrent0)
        binding?.canvas?.translationX = binding?.canvas?.translationX?.plus(diff.x) ?: 0f
        binding?.canvas?.translationY = binding?.canvas?.translationY?.plus(diff.y) ?: 0f
        pointPrev0.set(pointCurrent0)
        return true
    }

    fun onTouchCanvas(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                pointPrev0.set(-1f, -1f)
                pointPrev1.set(-1f, -1f)
                val keysHorizontal: ArrayList<List<Int>> = (0..size.y - 1)
                        .mapTo(ArrayList()) {
                            val cellsInRow = algorythm.getCellsInRow(it) ?: return true
                            algorythm.getKeys(cellsInRow)
                        }
                val keysVertical: ArrayList<List<Int>> = (0..size.x - 1)
                        .mapTo(ArrayList()) {
                            val cellsInColumn = algorythm.getCellsInColumn(it) ?: return true
                            algorythm.getKeys(cellsInColumn)
                        }

                isSolvable = algorythm.isSolvable(keysHorizontal, keysVertical)
                Log.d("onMotionActionUp", "isSolvable: $isSolvable")
            }

            else -> {
                if (event.action == MotionEvent.ACTION_MOVE) isSolvable = false

                val pointCurrent = PointF(event.x, event.y)
                val coordCurrent = algorythm.getCoordinateFromTouchPoint(
                        binding?.canvas,
                        pointCurrent) ?: return true
                val coordPrev = algorythm.getCoordinateFromTouchPoint(
                        binding?.canvas,
                        pointPrev0) ?: Point(-1, -1)
                if (!coordCurrent.equals(coordPrev.x, coordPrev.y)) {
                    val cell = algorythm.getCellByCoordinate(coordCurrent) ?: return true

                    if (event.action == MotionEvent.ACTION_MOVE) {
                        val cellPrev = algorythm.getCellByCoordinate(coordPrev) ?: return true
                        cell.state = cellPrev.state
                    } else {
                        cell.state = !cell.getState()
                    }

                    val bitmap = algorythm.onEditCanvasImage((binding?.canvas?.drawable as BitmapDrawable).bitmap, cell, true)
                    binding?.canvas?.setImageBitmap(bitmap)
                }
                pointPrev0.set(pointCurrent)
            }
        }



        return true
    }

    fun getMode(): Boolean {
        return (activity.findViewById(R.id.fab) as FloatingActionButton).tag as Boolean
    }

    fun onRefresh() {
        this.algorythm = Algorithm(size)
        binding?.canvas?.setImageBitmap(algorythm.createCanvasImage())
    }

    fun onSaveCanvas() {

    }
}