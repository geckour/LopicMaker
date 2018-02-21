package jp.co.seesaa.geckour.picrossmaker.presentation.fragment

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Size
import android.view.*
import com.trello.rxlifecycle2.components.RxFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import jp.co.seesaa.geckour.picrossmaker.App
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.presentation.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.api.ApiClient
import jp.co.seesaa.geckour.picrossmaker.api.model.Result
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentProblemsBinding
import jp.co.seesaa.geckour.picrossmaker.presentation.fragment.adapter.ProblemsListAdapter
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider
import jp.co.seesaa.geckour.picrossmaker.model.Problem
import jp.co.seesaa.geckour.picrossmaker.util.*
import kotlinx.coroutines.experimental.Job
import timber.log.Timber

class ProblemsFragment: RxFragment() {

    companion object {
        val TAG: String = ProblemsFragment::class.java.simpleName

        fun newInstance(): ProblemsFragment = ProblemsFragment()
    }

    private lateinit var binding: FragmentProblemsBinding
    private lateinit var adapter: ProblemsListAdapter
    private val jobList: ArrayList<Job> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_problems, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mainActivity()?.apply {
            toolbar.viewTreeObserver.apply {
                removeOnDrawListener(onClearScrollFlags)
                addOnGlobalLayoutListener(onSetScrollFlags)
            }
        }

        adapter = getAdapter()
        fetchProblems()

        mainActivity()?.binding?.appBarMain?.fabRight
                ?.apply {
                    setImageResource(R.drawable.ic_add_white_24px)
                    setOnClickListener {
                        run {
                            val requestCode = MyAlertDialogFragment.RequestCode.DEFINE_SIZE
                            val fragment = MyAlertDialogFragment.newInstance(
                                    title = getString(R.string.dialog_alert_title_size_define),
                                    resId = R.layout.dialog_define_size,
                                    requestCode = requestCode,
                                    cancelable = true
                            )
                            fragment.show((activity as MainActivity).fragmentManager, MyAlertDialogFragment.getTag(requestCode))
                        }
                    }
                }

        binding.recyclerView.let {
            it.layoutManager = LinearLayoutManager(activity)
            it.adapter = this@ProblemsFragment.adapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = true
            fetchProblems()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.textIndicateEmpty.setText(R.string.problem_fragment_message_empty)
    }

    override fun onResume() {
        super.onResume()

        mainActivity()?.apply {
            supportActionBar?.setTitle(R.string.action_bar_title_main)
            binding.appBarMain?.fabRight?.show()
        }
    }

    override fun onPause() {
        super.onPause()

        jobList.apply {
            forEach { it.cancel() }
            clear()
        }

        (mainActivity()?.toolbar?.layoutParams as? AppBarLayout.LayoutParams)?.scrollFlags = 0
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

    private fun getAdapter(): ProblemsListAdapter =
            ProblemsListAdapter(object: ProblemsListAdapter.IListener {
                override fun onClickProblemItem(view: View, position: Int, problem: Problem, hasOpt: Boolean) {
                    if (hasOpt) {
                        PopupMenu(view.context, view).apply {
                            menuInflater.inflate(R.menu.popup_opt_solvable_problem, menu)
                            setOnMenuItemClickListener { item ->
                                when (item.itemId) {
                                    R.id.menu_register_problem -> {
                                        onRegister(problem)
                                        true
                                    }
                                    R.id.menu_solve_problem -> {
                                        onSolve(problem)
                                        true
                                    }
                                    R.id.menu_edit_problem -> {
                                        onEdit(problem)
                                        true
                                    }
                                    R.id.menu_delete_problem -> {
                                        onDelete(position, problem)
                                        true
                                    }
                                    else -> false
                                }
                            }
                            show()
                        }
                    }
                }

                override fun onLongClickProblemItem(problem: Problem): Boolean  = true

                override fun onRegister(problem: Problem) { // TODO: 通信エラー時もメッセージ表示
                    ApiClient().registerProblem(problem.parse())
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .compose(bindToLifecycle())
                            .subscribe({ result ->
                                mainActivity()?.binding?.appBarMain?.contentMain?.container?.apply {
                                    result.body()?.message?.data?.let { showSnackbar(this, getStrResIdFromStatusCode(it)) }
                                    result.errorBody()?.string()?.let {
                                        App.Companion.gson.fromJson<Result<Result.Data<String>>>(it)
                                    }?.let {showSnackbar(this, getStrResIdFromStatusCode(it.message.data)) }
                                }
                            }, { t -> Timber.e(t) })
                }

                override fun onDelete(position: Int, problem: Problem) = tryToDelete(position, problem)

                fun tryToDelete(position: Int, problem: Problem) {
                    mainActivity()?.binding?.appBarMain?.contentMain?.container?.apply {
                        ui(jobList, { showSnackbar(this, R.string.problem_fragment_error_failure_delete) }) {
                            if (problem.id > -1) execDelete(problem.id, position, this@apply)
                        }
                    }
                }

                suspend fun execDelete(id: Long, adapterPosition: Int, rootView: View) {
                    async { OrmaProvider.db.selectFromProblem().idEq(id).firstOrNull() }.await()?.let { target ->
                        val deleteCount = async { OrmaProvider.db.deleteFromProblem().idEq(id).execute() }.await()
                        onExecDelete(deleteCount, id, target, adapterPosition, rootView)
                    }
                }

                fun onExecDelete(count: Int, id: Long, target: Problem, adapterPosition: Int, rootView: View) {
                    if (count > 0) {
                        adapter.removeProblemsByIndex(adapterPosition)
                        showSnackbar(rootView, R.string.problem_fragment_message_complete_delete, R.string.action_undo, Snackbar.LENGTH_LONG) {
                            ui(jobList) {
                                async {
                                    OrmaProvider.db.selectFromProblem().idEq(id).lastOrNull() ?: apply {
                                        OrmaProvider.db.insertIntoProblem(target)
                                    }
                                }.await()
                                adapter.insertProblem(adapterPosition, target)
                                showSnackbar(rootView, R.string.problem_fragment_message_undo)
                            }
                        }
                    } else showSnackbar(activity.findViewById(R.id.container), R.string.problem_fragment_error_failure_delete)
                }

                fun onSolve(problem: Problem) {
                    val fragment = SolveFragment.newInstance(
                            Pair(SolveFragment.ArgKeys.PROBLEM_ID, problem.id),
                            Pair(SolveFragment.ArgKeys.CANVAS_SIZE, Size(problem.keysVertical.keys.size, problem.keysHorizontal.keys.size))
                    )
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, fragment, SolveFragment.TAG)
                            .addToBackStack(SolveFragment.TAG)
                            .commit()
                }

                fun onEdit(problem: Problem) {
                    val fragment = EditorFragment.newInstance(Pair(EditorFragment.ArgKeys.PROBLEM_ID, problem.id))
                    if (fragment != null) {
                        fragmentManager.beginTransaction()
                                .replace(R.id.container, fragment)
                                .addToBackStack(null)
                                .commit()
                    }
                }

                override fun onBind() {
                    ui(jobList) { binding.textIndicateEmpty.visibility = View.GONE }
                }

                override fun onAllUnbind() {
                    ui(jobList) { binding.textIndicateEmpty.visibility = View.VISIBLE }
                }
            }, true)

    private fun fetchProblems() {
        ui(jobList) {
            adapter.clearProblems()
            val problems = OrmaProvider.db.selectFromProblem().draftEq(false).orderBy("editedAt").toList()
            adapter.addProblems(problems)
        }
    }
}