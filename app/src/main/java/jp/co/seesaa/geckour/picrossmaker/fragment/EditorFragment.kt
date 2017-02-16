package jp.co.seesaa.geckour.picrossmaker.fragment

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.GridLayoutManager
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.trello.rxlifecycle2.components.RxFragment
import jp.co.seesaa.geckour.picrossmaker.Constant
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentEditorBinding
import jp.co.seesaa.geckour.picrossmaker.fragment.adapter.EditorFragmentItemAdapter
import jp.co.seesaa.geckour.picrossmaker.model.Cell

class EditorFragment(listener: IListener): RxFragment() {
    var listener: IListener? = null
    private var size: Size? = null
    private var binding: FragmentEditorBinding? = null
    private var adapter: EditorFragmentItemAdapter? = null

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
        this.size = arguments.getSize(Constant.ARGS_FRAGMENT_CANVAS_SIZE)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_editor, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fab = activity.findViewById(R.id.fab) as FloatingActionButton
        fab.visibility = View.GONE

        (activity as MainActivity).supportActionBar?.setTitle(R.string.action_bar_title_edit)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding?.recyclerView?.layoutManager = GridLayoutManager(activity, (size?.width ?: 0) + getNumBlankArea())

        adapter = EditorFragmentItemAdapter(size ?: Size(0, 0), getNumBlankArea())
        binding?.recyclerView?.adapter = adapter
        onRefresh()
    }

    fun getNumBlankArea(): Int {
        if (size == null) return 0
        if (size?.width ?: 0 > 3 || size?.height ?: 0 > 3) return 3
        return if (size?.width ?: 0 < size?.height ?: 0) size?.height ?: 0 else size?.width ?: 0
    }

    fun onRefresh() {
        adapter?.clear()
        val numBlankArea = getNumBlankArea()
        val size = ((this.size?.width ?: 0) + numBlankArea) * ((this.size?.height ?: 0) + numBlankArea)
        for (i in 0..size - 1) adapter?.add(Cell())
    }
}