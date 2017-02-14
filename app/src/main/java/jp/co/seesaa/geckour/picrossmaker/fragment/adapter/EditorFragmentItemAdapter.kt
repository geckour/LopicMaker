package jp.co.seesaa.geckour.picrossmaker.fragment.adapter

import android.databinding.DataBindingUtil
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.databinding.ItemCellBinding
import java.util.*

/**
 * Created by geckour on 2017/02/14.
 */
class EditorFragmentItemAdapter(val size: Size): RecyclerView.Adapter<EditorFragmentItemAdapter.ViewHolder>() {
    private val cells = ArrayList<Boolean>()

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val binding: ItemCellBinding = DataBindingUtil.bind(view)
    }

    fun add(state: Boolean) {
        cells.add(state)
    }

    fun addAll(list: ArrayList<Boolean>) {
        val size = cells.size
        cells.addAll(list)
        notifyItemRangeChanged(size - 1, list.size)
    }

    fun clear() {
        val size = cells.size
        cells.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun getPosition(column: Int, row: Int): Int = size.width * row + column

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.item_cell, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pos = holder.adapterPosition
        val cellState = cells[pos]

        holder.binding.cell.setBackgroundColor(if (cellState) Color.BLACK else Color.WHITE)
    }

    override fun getItemCount(): Int {
        return cells.size
    }
}