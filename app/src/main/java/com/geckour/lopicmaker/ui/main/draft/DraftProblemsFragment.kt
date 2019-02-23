package com.geckour.lopicmaker.ui.main.draft

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.geckour.lopicmaker.R
import com.geckour.lopicmaker.data.model.Problem
import com.geckour.lopicmaker.databinding.FragmentProblemsBinding
import com.geckour.lopicmaker.ui.main.MainViewModel
import com.geckour.lopicmaker.util.observe

class DraftProblemsFragment : Fragment() {

    companion object {
        fun newInstance(): DraftProblemsFragment = DraftProblemsFragment()
    }

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProviders.of(requireActivity())[MainViewModel::class.java]
    }
    private val viewModel: DraftProblemsViewModel by lazy {
        ViewModelProviders.of(this)[DraftProblemsViewModel::class.java]
    }
    private lateinit var binding: FragmentProblemsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProblemsBinding.inflate(layoutInflater, container, false)
        observeEvents()

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mainViewModel.fabRightMode.postValue(MainViewModel.FabRightMode.ADD)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewModel.adapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchDraftProblems(requireContext(), binding)
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.fetchDraftProblems(requireContext(), binding)

        mainViewModel.toolbarTitleResId.postValue(R.string.action_bar_title_draft to emptyList())
        mainViewModel.fabRightVisible.postValue(true)
        mainViewModel.fabRightMode.postValue(MainViewModel.FabRightMode.ADD)
        mainViewModel.fabLeftVisible.postValue(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        menu?.clear()
        inflater?.inflate(R.menu.main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item)
    }

    private fun observeEvents() {
        viewModel.selectedBinding.observe(this) {
            it ?: return@observe

            PopupMenu(it.root.context, it.root).apply {
                menuInflater.inflate(R.menu.popup_opt_draft_problem, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_edit_problem -> {
                            onEdit(it.data)
                            true
                        }
                        R.id.menu_delete_problem -> {
                            onDelete(it.data)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }

        viewModel.problems.observe(this) {
            binding.textIndicateEmpty.visibility =
                if (it?.size ?: 0 > 0) View.GONE else View.VISIBLE
        }
    }

    private fun onEdit(problem: Problem?) {
        problem ?: return

        mainViewModel.toEditDraftProblemId.postValue(problem.id)
    }

    private fun onDelete(problem: Problem?) {
        problem ?: return

        mainViewModel.deleteProblem(requireContext(), problem) {
            if (it) {
                viewModel.adapter.removeProblemsByObject(problem)
                mainViewModel.snackBarStringResId
                    .postValue(R.string.problem_fragment_message_complete_delete)
            } else {
                mainViewModel.snackBarStringResId
                    .postValue(R.string.problem_fragment_error_failure_delete)
            }
        }
    }
}