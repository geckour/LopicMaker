package jp.co.seesaa.geckour.picrossmaker.fragment

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import com.trello.rxlifecycle2.components.RxFragment
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.async
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentProblemsBinding
import jp.co.seesaa.geckour.picrossmaker.fragment.adapter.DraftProblemsListAdapter
import jp.co.seesaa.geckour.picrossmaker.model.DraftProblem
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider
import jp.co.seesaa.geckour.picrossmaker.ui
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment.Companion.showSnackbar
import kotlinx.coroutines.experimental.Job

class DraftProblemsFragment: RxFragment() {

    companion object {
        fun newInstance(): DraftProblemsFragment = DraftProblemsFragment()
    }

    lateinit private var binding: FragmentProblemsBinding
    lateinit private var adapter: DraftProblemsListAdapter
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

        adapter = getAdapter()
        fetchDraftProblems()

        (activity as MainActivity).binding.appBarMain.fab
                .apply {
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

        (activity as MainActivity).actionBar?.setTitle(R.string.action_bar_title_draft)

        (activity as MainActivity).binding.appBarMain.fab.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        jobList.apply {
            forEach { it.cancel() }
            clear()
        }
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

    private fun getAdapter(): DraftProblemsListAdapter =
            DraftProblemsListAdapter(
                    object: DraftProblemsListAdapter.IListener {
                        override fun onClickDraftProblemItem(draftProblem: DraftProblem) {
                            val fragment = EditorFragment.newInstance(draftProblem.id, EditorFragment.ArgKeys.DRAFT_ID)
                            if (fragment != null) {
                                fragmentManager.beginTransaction()
                                        .replace(R.id.container, fragment)
                                        .addToBackStack(null)
                                        .commit()
                            }
                        }

                        override fun onLongClickDraftProblemItem(draftProblem: DraftProblem): Boolean = true

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
                    val id = adapter.getDraftProblemByIndex(position)?.id ?: -1
                    ui(jobList, { showSnackbar(activity.findViewById(R.id.container), R.string.problem_fragment_error_failure_delete) }) {
                        val deleteNum = async {
                            OrmaProvider.db.deleteFromDraftProblem()
                                    .idEq(id)
                                    .execute()
                        }.await()

                        if (deleteNum > 0) {
                            showSnackbar(activity.findViewById(R.id.container),
                                    R.string.problem_fragment_message_complete_delete)
                            adapter.removeDraftProblemsByIndex(position)
                        } else {
                            showSnackbar(activity.findViewById(R.id.container),
                                    R.string.problem_fragment_error_failure_delete)
                        }
                    }
                }
            }
        })
    }

    private fun fetchDraftProblems() {
        ui(jobList) {
            async { adapter.clearDraftProblems() }.await()
            val draftProblems = async { OrmaProvider.db.selectFromDraftProblem().orderBy("editedAt").toList() }.await()
            adapter.addDraftProblems(draftProblems)
        }
    }
}