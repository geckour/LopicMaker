package jp.co.seesaa.geckour.picrossmaker.fragment.adapter

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.databinding.ItemProblemBinding
import jp.co.seesaa.geckour.picrossmaker.model.DraftProblem
import java.util.*

class DraftProblemsListAdapter(val listener: DraftProblemsListAdapter.IListener): RecyclerView.Adapter<DraftProblemsListAdapter.ViewHolder>() {
    private val draftProblems: ArrayList<DraftProblem> = ArrayList()

    fun clearDraftProblems() {
        val size = draftProblems.size
        draftProblems.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun addDraftProblems(draftProblems: List<DraftProblem>) {
        val size = this.draftProblems.size
        this.draftProblems.addAll(draftProblems)
        notifyItemRangeInserted(size - 1, draftProblems.size)
    }

    fun getProblemByIndex(position: Int): DraftProblem? {
        return if (-1 < position && position < draftProblems.size) draftProblems[position] else null
    }

    fun removeDraftProblemsByIndex(vararg positions: Int) {
        positions.forEach {
            draftProblems.removeAt(it)
            notifyItemRemoved(it)
        }
    }

    fun removeDraftProblemsByObject(vararg draftProblems: DraftProblem) {
        draftProblems.forEach {
            this.draftProblems.remove(it)
            val index = draftProblems.indexOf(it)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.item_problem, parent, false)
        val holder = ViewHolder(view)
        val position = holder.adapterPosition

        holder.itemView.setOnClickListener { view -> listener.onClickDraftProblemItem(position) }
        holder.itemView.setOnLongClickListener { view -> listener.onLongClickDraftProblemItem(position) }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pos = holder.adapterPosition
        val problem = draftProblems[pos]
        holder.bindData(problem)
    }

    override fun getItemCount(): Int {
        return draftProblems.size
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val binding: ItemProblemBinding = DataBindingUtil.bind(view)

        fun bindData(draftProblem: DraftProblem) {
            binding.thumb.setImageBitmap(draftProblem.thumb)
            binding.title.text = draftProblem.title
            binding.point.text = binding.root.context
                    .getString(R.string.problem_fragment_item_point,
                            draftProblem.keysHorizontal.keys.size,
                            draftProblem.keysVertical.keys.size)
        }
    }

    interface IListener {
        fun onClickDraftProblemItem(position: Int)
        fun onLongClickDraftProblemItem(position: Int): Boolean
    }
}