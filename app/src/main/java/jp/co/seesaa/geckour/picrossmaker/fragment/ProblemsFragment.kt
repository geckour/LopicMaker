package jp.co.seesaa.geckour.picrossmaker.fragment

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import com.trello.rxlifecycle2.components.RxFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.api.ApiClient
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentProblemsBinding
import jp.co.seesaa.geckour.picrossmaker.fragment.adapter.ProblemsListAdapter
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

    lateinit private var binding: FragmentProblemsBinding
    lateinit private var adapter: ProblemsListAdapter
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
        (mainActivity()?.toolbar?.layoutParams as? AppBarLayout.LayoutParams)?.scrollFlags =
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS

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

            val itemTouchHelper = getItemTouchHelper().apply { attachToRecyclerView(binding.recyclerView) }
            it.addItemDecoration(itemTouchHelper)
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = true
            fetchProblems()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.textIndicateEmpty.setText(R.string.problem_fragment_message_empty)

        (activity as MainActivity).binding.navView.menu.findItem(R.id.nav_problem).isChecked = true
    }

    override fun onResume() {
        super.onResume()

        mainActivity()?.apply {
            actionBar?.setTitle(R.string.action_bar_title_main)
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
                override fun onClickProblemItem(problem: Problem) {
                    val fragment = EditorFragment.newInstance(Pair(EditorFragment.ArgKeys.PROBLEM_ID, problem.id))
                    if (fragment != null) {
                        fragmentManager.beginTransaction()
                                .replace(R.id.container, fragment)
                                .addToBackStack(null)
                                .commit()
                    }
                }

                override fun onLongClickProblemItem(problem: Problem): Boolean  = true

                override fun onImport(problem: Problem) {}

                override fun onRegister(problem: Problem) {
                    ApiClient().registerProblem(problem.parse())
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .compose(bindToLifecycle())
                            .subscribe({ result ->
                                mainActivity()?.binding?.appBarMain?.contentMain?.container?.apply {
                                    showSnackbar(this, result.message)
                                }
                            }, { throwable ->
                                Timber.e(throwable)
                                mainActivity()?.binding?.appBarMain?.contentMain?.container?.apply {
                                    showSnackbar(this, R.string.problem_fragment_error_failure_register)
                                }
                            })
                }

                override fun onBind() {
                    ui(jobList) { binding.textIndicateEmpty.visibility = View.GONE }
                }

                override fun onAllUnbind() {
                    ui(jobList) { binding.textIndicateEmpty.visibility = View.VISIBLE }
                }
            }, true)

    private fun getItemTouchHelper(): ItemTouchHelper {
        return ItemTouchHelper(
                object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean = false

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                        val position = viewHolder?.adapterPosition
                        if (position != null) {
                            mainActivity()?.binding?.appBarMain?.contentMain?.container?.apply {
                                ui(jobList, { showSnackbar(activity.findViewById(R.id.container), R.string.problem_fragment_error_failure_delete) }) {
                                    val id = adapter.getProblemByIndex(position)?.id ?: -1

                                    if (id > -1) {
                                        async { OrmaProvider.db.selectFromProblem().idEq(id).firstOrNull() }.await()?.let { target ->
                                            val deleteCount = async { OrmaProvider.db.deleteFromProblem().idEq(id).execute() }.await()

                                            if (deleteCount > 0) {
                                                adapter.removeProblemsByIndex(position)
                                                showSnackbar(this@apply, R.string.problem_fragment_message_complete_delete, R.string.action_undo) {
                                                    ui(jobList) {
                                                        async {
                                                            OrmaProvider.db.selectFromProblem().idEq(id).lastOrNull() ?: apply {
                                                                OrmaProvider.db.insertIntoProblem(target)
                                                            }
                                                        }.await()
                                                        adapter.insertProblem(position, target)
                                                        showSnackbar(this@apply, R.string.problem_fragment_message_undo)
                                                    }
                                                }
                                            } else {
                                                ui(jobList) { showSnackbar(activity.findViewById(R.id.container), R.string.problem_fragment_error_failure_delete) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        )
    }

    private fun fetchProblems() {
        ui(jobList) {
            adapter.clearProblems()
            val problems = OrmaProvider.db.selectFromProblem().draftEq(false).orderBy("editedAt").toList()
            adapter.addProblems(problems)
        }
    }
}