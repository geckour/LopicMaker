package jp.co.seesaa.geckour.picrossmaker.fragment

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toolbar
import com.trello.rxlifecycle2.components.RxFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.api.ApiClient
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentProblemsBinding
import jp.co.seesaa.geckour.picrossmaker.databinding.ToolbarSearchBinding
import jp.co.seesaa.geckour.picrossmaker.fragment.adapter.ProblemsListAdapter
import jp.co.seesaa.geckour.picrossmaker.model.Problem
import jp.co.seesaa.geckour.picrossmaker.util.mainActivity
import jp.co.seesaa.geckour.picrossmaker.util.parse
import timber.log.Timber

class SearchFragment: RxFragment() {

    companion object {
        val tag: String = SearchFragment::class.java.simpleName

        fun createInstance(): SearchFragment = SearchFragment()
    }

    private lateinit var binding: FragmentProblemsBinding
    private lateinit var toolbarBinding: ToolbarSearchBinding
    private lateinit var adapter: ProblemsListAdapter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_problems, container, false)

        mainActivity()?.setActionBar(null)
        mainActivity()?.binding?.appBarMain?.appBar?.apply {
            removeAllViews()
            toolbarBinding = DataBindingUtil.inflate(inflater, R.layout.toolbar_search, this, false)
            addView(toolbarBinding.root)
            toolbarBinding.buttonSearch.setOnClickListener {
                adapter.clearProblems()
                search(toolbarBinding.queryTitle.text.toString(), toolbarBinding.queryGenre.text.toString())
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .compose(bindToLifecycle())
                        .subscribe({
                            Timber.d("$it")
                            adapter.addProblems(it.message.data.problems.map { it.parse() })
                        }, { t ->
                            adapter.clearProblems()
                            this@SearchFragment.binding.textIndicateEmpty.apply {
                                text = getString(R.string.problem_fragment_message_empty_search)
                                visibility = View.VISIBLE
                            }
                            t.printStackTrace()
                        })
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textIndicateEmpty.visibility = View.GONE
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = getAdapter()
        (activity as MainActivity).binding.appBarMain?.fab?.hide()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = this@SearchFragment.adapter
        }
    }

    override fun onPause() {
        super.onPause()

        mainActivity()?.apply {
            binding.appBarMain?.appBar?.apply {
                val toolbar = LayoutInflater.from(activity).inflate(R.layout.toolbar_main, null) as Toolbar
                removeAllViews()
                addView(toolbar)
                setActionBar(toolbar)
            }
        }
    }

    private fun search(title: String?, genre: String?) =
            ApiClient().search(title?.let { if (it.isNotBlank()) it else null }, genre?.let { if (it.isNotBlank()) it else null })

    private fun getAdapter(): ProblemsListAdapter =
            ProblemsListAdapter(object: ProblemsListAdapter.IListener {
                override fun onClickProblemItem(problem: Problem) {} // TODO: ローカルに保存するためのダイアログ表示

                override fun onLongClickProblemItem(problem: Problem): Boolean = false

                override fun onBind() {
                    binding.textIndicateEmpty.visibility = View.GONE
                }

                override fun onAllUnbind() {
                    binding.textIndicateEmpty.visibility = View.VISIBLE
                }
            })
}