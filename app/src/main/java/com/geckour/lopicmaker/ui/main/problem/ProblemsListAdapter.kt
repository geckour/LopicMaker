package com.geckour.lopicmaker.ui.main.problem

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.geckour.lopicmaker.R
import com.geckour.lopicmaker.data.model.Problem
import com.geckour.lopicmaker.databinding.ItemProblemBinding
import com.geckour.lopicmaker.util.SingleLiveEvent

class ProblemsListAdapter(
    private val viewModel: ProblemsViewModel
) : RecyclerView.Adapter<ProblemsListAdapter.ViewHolder>() {

    internal fun setProblems(problems: List<Problem>) {
        viewModel.problems.value?.apply {
            val ids = this.map { it.id }
            val update: List<Problem> = problems.filter { ids.contains(it.id) }
            val add = problems - update
            viewModel.problems.value = problems
            update.forEach { notifyItemChanged(getIndexByProblem(it)) }
            add.forEach { notifyItemInserted(getIndexByProblem(it)) }
        } ?: run {
            viewModel.problems.value = problems
            notifyItemRangeInserted(0, problems.size)
        }
    }

    private fun getIndexByProblem(problem: Problem): Int =
        viewModel.problems.value?.indexOf(problem) ?: -1

    internal fun removeProblemsByObject(vararg problems: Problem) {
        problems.forEach {
            val index = viewModel.problems.get().indexOf(it)
            val toPost = viewModel.problems.get()
                .toMutableList().apply { remove(it) }
            viewModel.problems.postValue(toPost)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemProblemBinding =
            DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_problem, parent, false)

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val problem = viewModel.problems.get()[holder.adapterPosition]
        holder.bindData(problem)
    }

    override fun getItemCount(): Int = viewModel.problems.get().size

    inner class ViewHolder(private val binding: ItemProblemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindData(problem: Problem) {
            binding.data = problem
            binding.root.apply {
                setOnClickListener {
                    viewModel.selectedBinding.postValue(binding)
                }
            }
        }
    }

    private inline fun <reified T> SingleLiveEvent<List<T>>.get() = this.value ?: emptyList()
}