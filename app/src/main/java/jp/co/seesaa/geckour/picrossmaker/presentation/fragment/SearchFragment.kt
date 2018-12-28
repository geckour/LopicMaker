package jp.co.seesaa.geckour.picrossmaker.presentation.fragment

import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.schedulers.Schedulers
import jp.co.seesaa.geckour.picrossmaker.App
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.R.id.thumb
import jp.co.seesaa.geckour.picrossmaker.R.id.toolbar
import jp.co.seesaa.geckour.picrossmaker.api.ApiClient
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentProblemsBinding
import jp.co.seesaa.geckour.picrossmaker.databinding.ToolbarSearchBinding
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider
import jp.co.seesaa.geckour.picrossmaker.model.Problem
import jp.co.seesaa.geckour.picrossmaker.presentation.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.presentation.fragment.adapter.ProblemsListAdapter
import jp.co.seesaa.geckour.picrossmaker.util.*
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class SearchFragment : Fragment(), MyAlertDialogFragment.IListener {

    companion object {
        val tag: String = SearchFragment::class.java.simpleName

        fun createInstance(): SearchFragment = SearchFragment()
    }

    private lateinit var binding: FragmentProblemsBinding
    private lateinit var toolbarBinding: ToolbarSearchBinding
    private lateinit var adapter: ProblemsListAdapter

    private val jobList: ArrayList<Job> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        inflater?.let {
            DataBindingUtil.inflate<FragmentProblemsBinding>(
                    it,
                    R.layout.fragment_problems,
                    container,
                    false
            ).apply { binding = this }
        }

        mainActivity?.setActionBar(null)
        mainActivity?.binding?.appBarMain?.appBar?.apply {
            removeAllViews()
            inflater?.let {
                DataBindingUtil.inflate<ToolbarSearchBinding>(
                        it,
                        R.layout.toolbar_search,
                        this,
                        true
                ).apply { toolbarBinding = this }
            }
            (toolbarBinding.root.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
            toolbarBinding.buttonSearch.setOnClickListener {
                adapter.clearProblems()
                search(toolbarBinding.queryTitle.text.toString(), toolbarBinding.queryGenre.text.split(Regex("\\s")))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .compose(bindToLifecycle())
                        .subscribe({
                            if (!it.isSuccessful) {
                                adapter.clearProblems()
                                this@SearchFragment.binding.textIndicateEmpty.apply {
                                    text = getString(R.string.problem_fragment_message_empty_search)
                                    visibility = View.VISIBLE
                                }
                                return@subscribe
                            }
                            ui(jobList) {
                                it.body()?.message?.data?.problems?.map {
                                    val algorithm = Algorithm(it.keysHorizontal, it.keysVertical)
                                    val cells = algorithm.getSolution()
                                    it.parse(algorithm, cells).apply { thumb = null }
                                }?.apply { adapter.addProblems(this) }
                            }
                        }, { t -> Timber.e(t) })
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textIndicateEmpty.visibility = View.GONE
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = getAdapter()

        binding.recyclerView.let {
            it.layoutManager = LinearLayoutManager(activity)
            it.adapter = this@SearchFragment.adapter
        }
    }

    override fun onResume() {
        super.onResume()

        mainActivity?.binding?.appBarMain?.fabRight?.hide()
    }

    override fun onPause() {
        super.onPause()

        mainActivity?.apply {
            binding.appBarMain?.appBar?.apply {
                removeAllViews()
                addView(toolbar)
                setSupportActionBar(toolbar)
            }
        }
    }

    override fun onResultAlertDialog(dialogInterface: DialogInterface, requestCode: MyAlertDialogFragment.RequestCode, resultCode: Int, result: Any?) {
        when (requestCode) {
            MyAlertDialogFragment.RequestCode.IMPORT_PROBLEM -> {
                val problem: Problem? =
                        (result as? String)?.let {
                            try {
                                App.gson.fromJson(it)
                            } catch (e: Exception) {
                                Timber.e(e)
                                null
                            }
                        }

                when (resultCode) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        onImport(problem)
                    }
                }
            }

            else -> {
            }
        }
    }

    private fun search(title: String?, tags: List<String>?) =
            ApiClient().search(title?.let { if (it.isNotBlank()) it else null }, tags?.filter { it.isNotBlank() }?.let { if (it.isEmpty()) null else it })

    private fun onImport(problem: Problem?) {
        if (problem != null) {
            ui(jobList) {
                OrmaProvider.db.insertIntoProblem(problem)
                mainActivity?.binding?.appBarMain?.contentMain?.container?.apply {
                    showSnackbar(this, R.string.problem_fragment_message_complete_import)
                }
            }
        } else {
            mainActivity?.binding?.appBarMain?.contentMain?.container?.apply {
                showSnackbar(this, R.string.problem_fragment_message_failure_import)
            }
        }
    }

    private fun getAdapter(): ProblemsListAdapter =
            ProblemsListAdapter(
                    object : ProblemsListAdapter.IListener {
                        override fun onClickProblemItem(view: View, position: Int, problem: Problem, hasOpt: Boolean) {
                            MyAlertDialogFragment.newInstance(
                                    title = getString(R.string.dialog_alert_title_import),
                                    message = getString(R.string.dialog_alert_message_import),
                                    optional = App.gson.toJson(problem),
                                    requestCode = MyAlertDialogFragment.RequestCode.IMPORT_PROBLEM,
                                    targetFragment = this@SearchFragment
                            ).show(mainActivity?.supportFragmentManager, MyAlertDialogFragment.getTag(MyAlertDialogFragment.RequestCode.IMPORT_PROBLEM))
                        }

                        override fun onLongClickProblemItem(problem: Problem): Boolean = false

                        override fun onRegister(problem: Problem) {}

                        override fun onDelete(position: Int, problem: Problem) {}

                        override fun onBind() {
                            binding.textIndicateEmpty.visibility = View.GONE
                        }

                        override fun onAllUnbind() {
                            binding.textIndicateEmpty.visibility = View.VISIBLE
                        }
                    }, false
            )

    private val mainActivity get() = requireActivity() as? MainActivity
}