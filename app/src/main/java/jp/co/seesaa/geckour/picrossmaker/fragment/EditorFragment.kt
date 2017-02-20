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
    private var size: Size? = null
    private var binding: FragmentEditorBinding? = null
    private val pointPrev0 = PointF(-1f, -1f)
    private val pointPrev1 = PointF(-1f, -1f)
    private val cells: ArrayList<Cell> = ArrayList()

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
        const val MODE_EDIT: Boolean = true
        const val MODE_ZOOM: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.size = arguments.getSize(Constant.ARGS_FRAGMENT_CANVAS_SIZE)
        for (i in 0..(size?.width ?: 0) - 1) (0..(size?.height ?: 0) - 1).mapTo(cells) { Cell(Point(i, it)) }
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

                        else ->
                            if (event.pointerCount > 1) {
                                val p0 = PointF(event.getX(0), event.getY(0))
                                val p1 = PointF(event.getX(1), event.getY(1))
                                onScale(p0, p1)
                            } else true
                    }
                } else false
            }
        }
    }

    fun onScale (pointCurrent0: PointF, pointCurrent1: PointF): Boolean {
        if (pointPrev0.x < 0f) pointPrev0.set(pointCurrent0)
        if (pointPrev1.x < 0f) pointPrev1.set(pointCurrent1)
        val scale = Algorithm.getScale(Algorithm.getPointDiff(pointPrev0, pointPrev1).length(), Algorithm.getPointDiff(pointCurrent0, pointCurrent1).length())
        val pointMidPrev = Algorithm.getPointMid(pointPrev0, pointPrev1)
        val pointMidCurrent = Algorithm.getPointMid(pointCurrent0, pointCurrent1)
        val diff = Algorithm.getPointDiff(pointMidPrev, pointMidCurrent)
        binding?.canvas?.translationX = binding?.canvas?.translationX?.plus(diff.x) ?: 0f
        binding?.canvas?.translationY = binding?.canvas?.translationY?.plus(diff.y) ?: 0f
        binding?.canvas?.scaleX = binding?.canvas?.scaleX?.times(scale) ?: 1f
        binding?.canvas?.scaleY = binding?.canvas?.scaleY?.times(scale) ?: 1f
        pointPrev0.set(pointCurrent0)
        pointPrev1.set(pointCurrent1)
        return true
    }

    fun onTouchCanvas(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                pointPrev0.set(-1f, -1f)
                pointPrev1.set(-1f, -1f)
            }

            else -> {
                val pointCurrent = PointF(event.x, event.y)
                val coordCurrent = Algorithm.getCoordinate(
                        binding?.canvas,
                        pointCurrent,
                        size ?: Size(0, 0)) ?: return true
                val coordPrev = Algorithm.getCoordinate(
                        binding?.canvas,
                        pointPrev0,
                        size ?: Size(0, 0)) ?: Point(-1, -1)
                if (!coordCurrent.equals(coordPrev.x, coordPrev.y)) {
                    val cell = Algorithm.getCellByCoordinate(cells, coordCurrent) ?: return true

                    if (event.action == MotionEvent.ACTION_MOVE) {
                        val cellPrev = Algorithm.getCellByCoordinate(cells, coordPrev) ?: return true
                        cell.state = cellPrev.state
                    } else {
                        cell.state = !cell.state
                    }

                    val bitmap = Algorithm.onEditCanvasImage((binding?.canvas?.drawable as BitmapDrawable).bitmap, size ?: Size(0, 0), cells, cell)
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
        binding?.canvas?.setImageBitmap(Algorithm.createCanvasImage(binding?.canvas, size ?: Size(0, 0)))
    }
}