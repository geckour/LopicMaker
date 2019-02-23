package com.geckour.lopicmaker.ui.main.problem

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geckour.lopicmaker.data.DB
import com.geckour.lopicmaker.data.model.Problem
import com.geckour.lopicmaker.databinding.FragmentProblemsBinding
import com.geckour.lopicmaker.databinding.ItemProblemBinding
import com.geckour.lopicmaker.util.SingleLiveEvent
import kotlinx.coroutines.launch

class ProblemsViewModel : ViewModel() {

    internal val problems = SingleLiveEvent<List<Problem>>()

    internal val selectedBinding = SingleLiveEvent<ItemProblemBinding>()

    internal val adapter = ProblemsListAdapter(this)

    internal fun fetchProblems(context: Context, binding: FragmentProblemsBinding) {
        viewModelScope.launch {
            binding.swipeRefresh.isRefreshing = true
            val problems = DB.getInstance(context).problemDao().getAll().filter { it.draft.not() }
            adapter.setProblems(problems)
            binding.swipeRefresh.isRefreshing = false
        }
    }
}