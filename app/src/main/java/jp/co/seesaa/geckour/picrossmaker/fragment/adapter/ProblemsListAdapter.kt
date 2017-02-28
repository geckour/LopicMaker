package jp.co.seesaa.geckour.picrossmaker.fragment.adapter

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.databinding.ItemProblemBinding
import jp.co.seesaa.geckour.picrossmaker.model.Problem
import java.util.*

class ProblemsListAdapter(val listener: IListener): RecyclerView.Adapter<ProblemsListAdapter.ViewHolder>() {
    private val problems: ArrayList<Problem> = ArrayList()

    fun clearProblems() {
        val size = problems.size
        problems.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun addProblems(problems: List<Problem>) {
        val size = this.problems.size
        this.problems.addAll(problems)
        notifyItemRangeInserted(size - 1, problems.size)
    }

    fun getProblemByIndex(position: Int): Problem? {
        return if (-1 < position && position < problems.size) problems[position] else null
    }

    fun removeProblemsByIndex(vararg positions: Int) {
        positions.forEach {
            problems.removeAt(it)
            notifyItemRemoved(it)
        }
    }

    fun removeProblemsByObject(vararg problems: Problem) {
        problems.forEach {
            this.problems.remove(it)
            val index = problems.indexOf(it)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.item_problem, parent, false)
        val holder = ViewHolder(view)

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pos = holder.adapterPosition
        val problem = problems[pos]
        holder.bindData(problem)

        holder.itemView.setOnClickListener { view -> listener.onClickProblemItem(pos) }
        holder.itemView.setOnLongClickListener { view -> listener.onLongClickProblemItem(pos) }
    }

    override fun getItemCount(): Int {
        return problems.size
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val binding: ItemProblemBinding = DataBindingUtil.bind(view)

        fun bindData(problem: Problem) {
            binding.thumb.setImageBitmap(problem.thumb)
            binding.title.text = problem.title
            binding.point.text = binding.root.context
                    .getString(R.string.problem_fragment_item_point,
                            problem.keysHorizontal.keys.size,
                            problem.keysVertical.keys.size)
        }
    }

    interface IListener {
        fun onClickProblemItem(position: Int)
        fun onLongClickProblemItem(position: Int): Boolean
    }
}