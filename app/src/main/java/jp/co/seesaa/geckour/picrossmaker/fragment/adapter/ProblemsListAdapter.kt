package jp.co.seesaa.geckour.picrossmaker.fragment.adapter

import android.databinding.DataBindingUtil
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
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
        onChangeProblems()
    }

    fun addProblems(problems: List<Problem>) {
        val size = this.problems.size
        this.problems.addAll(problems)
        notifyItemRangeInserted(size - 1, problems.size)
        onChangeProblems()
    }

    fun insertProblem(position: Int, problem: Problem) {
        this.problems.add(position, problem)
        notifyItemInserted(position)
        onChangeProblems()
    }

    fun getProblemByIndex(position: Int): Problem? =
            if (-1 < position && position < problems.size) problems[position] else null

    fun removeProblemsByIndex(vararg positions: Int) {
        positions.forEach {
            problems.removeAt(it)
            notifyItemRemoved(it)
        }
        onChangeProblems()
    }

    fun removeProblemsByObject(vararg problems: Problem) {
        problems.forEach {
            this.problems.remove(it)
            val index = problems.indexOf(it)
            notifyItemRemoved(index)
        }
        onChangeProblems()
    }

    private fun onChangeProblems() {
        if (problems.size > 0) listener.onBind()
        else listener.onAllUnbind()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val binding: ItemProblemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent?.context), R.layout.item_problem, parent, false)

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pos = holder.adapterPosition
        val problem = problems[pos]
        holder.bindData(problem)
    }

    override fun getItemCount(): Int = problems.size

    inner class ViewHolder(private val binding: ItemProblemBinding): RecyclerView.ViewHolder(binding.root) {

        fun bindData(problem: Problem) {
            binding.thumb.setImageBitmap(problem.thumb)
            binding.title.text = problem.title
            binding.size.text = binding.root.context
                    .getString(R.string.problem_fragment_item_point,
                            problem.keysVertical.keys.size,
                            problem.keysHorizontal.keys.size)
            binding.tag.apply { text = context.getString(R.string.problem_fragment_text_tag_prefix, if (problem.tags.isEmpty()) "-" else problem.tags.joinToString(", ")) }
            binding.opt.apply {
                setOnClickListener {
                    PopupMenu(this.context, this).apply {
                        menuInflater.inflate(R.menu.popup_problem_opt, menu)
                        setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.menu_register_problem -> {
                                    listener.onRegister(problem)
                                    true
                                }
                                else -> false
                            }
                        }
                        show()
                    }
                }
            }
            binding.root.apply {
                setOnClickListener { listener.onClickProblemItem(problem) }
                setOnLongClickListener { listener.onLongClickProblemItem(problem) }
            }
        }
    }

    interface IListener {
        fun onClickProblemItem(problem: Problem)
        fun onLongClickProblemItem(problem: Problem): Boolean
        fun onRegister(problem: Problem)
        fun onBind()
        fun onAllUnbind()
    }
}