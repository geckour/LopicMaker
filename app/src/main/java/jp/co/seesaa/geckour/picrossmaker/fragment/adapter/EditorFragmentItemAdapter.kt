package jp.co.seesaa.geckour.picrossmaker.fragment.adapter

import android.databinding.DataBindingUtil
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.databinding.ItemCellBlankBinding
import jp.co.seesaa.geckour.picrossmaker.databinding.ItemCellNormalBinding
import jp.co.seesaa.geckour.picrossmaker.databinding.ItemCellNumberBinding
import jp.co.seesaa.geckour.picrossmaker.model.Cell
import java.util.*

/**
 * Created by geckour on 2017/02/14.
 */
class EditorFragmentItemAdapter(val size: Size, var numBlankArea: Int): RecyclerView.Adapter<EditorFragmentItemAdapter.ViewHolder>() {
    private val cells = ArrayList<Cell>()

    companion object {
        val VIEW_TYPE_NORMAL = 0
        val VIEW_TYPE_NUMBER = 1
        val VIEW_TYPE_BLANK = 2
    }

    inner class ViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder(view) {
        val bindingNormal: ItemCellNormalBinding? = if (viewType == VIEW_TYPE_NORMAL) DataBindingUtil.bind(view) else null
        val bindingNumber: ItemCellNumberBinding? = if (viewType == VIEW_TYPE_NUMBER) DataBindingUtil.bind(view) else null
        val bindingBlank: ItemCellBlankBinding? = if (viewType == VIEW_TYPE_BLANK) DataBindingUtil.bind(view) else null
    }

    fun add(cell: Cell) {
        cells.add(cell)
    }

    fun addAll(list: ArrayList<Cell>) {
        val size = cells.size
        cells.addAll(list)
        notifyItemRangeChanged(size - 1, list.size)
    }

    fun clear() {
        val size = cells.size
        cells.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun getPositionFromSize(size: Size): Int = (this.size.width + numBlankArea) * (size.height + numBlankArea) + (size.width + numBlankArea)

    fun getSizeFromPosition(position: Int): Size = Size(position % (this.size.width + numBlankArea), position / (this.size.width + numBlankArea))

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = when (viewType) {
            VIEW_TYPE_NUMBER -> LayoutInflater.from(parent?.context).inflate(R.layout.item_cell_number, parent, false)
            VIEW_TYPE_BLANK -> LayoutInflater.from(parent?.context).inflate(R.layout.item_cell_blank, parent, false)
            else -> LayoutInflater.from(parent?.context).inflate(R.layout.item_cell_normal, parent, false)
        }
        return ViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pos = holder.adapterPosition
        val cell = cells[pos]

        if (getItemViewType(pos) == VIEW_TYPE_NORMAL) {
            holder.bindingNormal?.cell?.setOnClickListener {
                cell.state = !cell.state
                notifyItemChanged(pos)
            }
            holder.bindingNormal?.cell?.setBackgroundColor(if (cell.state) Color.BLACK else Color.WHITE)
        }
        if (getItemViewType(pos) == VIEW_TYPE_NUMBER) {
            val coordinate = getSizeFromPosition(pos)
            if ((coordinate.height > numBlankArea - 1 && coordinate.width == numBlankArea - 1) ||
                    (coordinate.width > numBlankArea - 1 && coordinate.height == numBlankArea - 1)) {
                cell.str = "0"
            }
            holder.bindingNumber?.number?.text = cell.str
        }
    }

    override fun getItemCount(): Int {
        return cells.size
    }

    override fun getItemViewType(position: Int): Int {
        val coordinate = getSizeFromPosition(position)
        if (coordinate.height < numBlankArea && coordinate.width < numBlankArea) return VIEW_TYPE_BLANK
        if ((coordinate.height > numBlankArea - 1 && coordinate.width < numBlankArea) ||
                (coordinate.width > numBlankArea - 1 && coordinate.height < numBlankArea)) return VIEW_TYPE_NUMBER
        return VIEW_TYPE_NORMAL
    }
}