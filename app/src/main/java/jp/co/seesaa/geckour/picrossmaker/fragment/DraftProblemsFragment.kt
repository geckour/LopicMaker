package jp.co.seesaa.geckour.picrossmaker.fragment

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import com.trello.rxlifecycle2.components.RxFragment
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.util.async
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentProblemsBinding
import jp.co.seesaa.geckour.picrossmaker.fragment.adapter.ProblemsListAdapter
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider
import jp.co.seesaa.geckour.picrossmaker.model.Problem
import jp.co.seesaa.geckour.picrossmaker.util.ui
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment
import jp.co.seesaa.geckour.picrossmaker.util.ViewUtil.showSnackbar
import jp.co.seesaa.geckour.picrossmaker.util.mainActivity
import kotlinx.coroutines.experimental.Job

class DraftProblemsFragment: RxFragment() {

    companion object {
        val TAG: String = DraftProblemsFragment::class.java.simpleName

        fun newInstance(): DraftProblemsFragment = DraftProblemsFragment()
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
                removeOnDrawListener(layoutListenerForClear)
                addOnGlobalLayoutListener(layoutListenerForSet)
            }
        }

        adapter = getAdapter()
        fetchDraftProblems()

        mainActivity()?.binding?.appBarMain?.fab
                ?.apply {
                    setImageResource(R.drawable.ic_add_white_24px)
                    setOnClickListener {
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

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = adapter

        val itemTouchHelper = getItemTouchHelper()
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.addItemDecoration(itemTouchHelper)

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = true
            fetchDraftProblems()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.textIndicateEmpty.setText(R.string.problem_fragment_message_empty_draft)

        (activity as MainActivity).binding.navView.menu.findItem(R.id.nav_draft).isChecked = true
    }

    override fun onResume() {
        super.onResume()

        mainActivity()?.apply {
            actionBar?.setTitle(R.string.action_bar_title_draft)
            binding.appBarMain?.fab?.show()
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
            ProblemsListAdapter(
                    object: ProblemsListAdapter.IListener {
                        override fun onClickProblemItem(problem: Problem) {
                            val fragment = EditorFragment.newInstance(Pair(EditorFragment.ArgKeys.PROBLEM_ID, problem.id))
                            if (fragment != null) {
                                fragmentManager.beginTransaction()
                                        .replace(R.id.container, fragment)
                                        .addToBackStack(null)
                                        .commit()
                            }
                        }

                        override fun onLongClickProblemItem(problem: Problem): Boolean = true

                        override fun onRegister(problem: Problem) {}

                        override fun onBind() {
                            binding.textIndicateEmpty.visibility = View.GONE
                        }

                        override fun onAllUnbind() {
                            binding.textIndicateEmpty.visibility = View.VISIBLE
                        }
                    })

    private fun getItemTouchHelper(): ItemTouchHelper {
        return ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
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
        })
    }

    private fun fetchDraftProblems() {
        ui(jobList) {
            async { adapter.clearProblems() }.await()
            val draftProblems = async { OrmaProvider.db.selectFromProblem().draftEq(true).orderBy("editedAt").toList() }.await()
            adapter.addProblems(draftProblems)
        }
    }
}