package jp.co.seesaa.geckour.picrossmaker.fragment

import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.util.Size
import android.view.*
import com.trello.rxlifecycle2.components.RxFragment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentProblemsBinding
import jp.co.seesaa.geckour.picrossmaker.fragment.adapter.ProblemsListAdapter
import jp.co.seesaa.geckour.picrossmaker.model.OrmaProvider
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment
import timber.log.Timber

class ProblemsFragment: RxFragment() {
    lateinit private var binding: FragmentProblemsBinding
    lateinit private var adapter: ProblemsListAdapter

    companion object {
        fun newInstance(): ProblemsFragment {
            val fragment = ProblemsFragment()
            return fragment
        }
    }

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).supportActionBar?.setTitle(R.string.app_name)

        val fab = activity.findViewById(R.id.fab) as FloatingActionButton
        fab.visibility = View.VISIBLE
    }

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
        fetchProblems()

        val fab = activity.findViewById(R.id.fab) as FloatingActionButton
        fab.setImageResource(R.drawable.ic_add_white_24px)
        fab.setOnClickListener {
            view ->
            run {
                val fragment = MyAlertDialogFragment.Builder(object: MyAlertDialogFragment.IListener {
                    override fun onResultAlertDialog(dialogInterface: DialogInterface, requestCode: Int, resultCode: Int, result: Any?) {
                        when (resultCode) {
                            DialogInterface.BUTTON_POSITIVE ->
                                onPositive(requestCode, result)
                        }
                        dialogInterface.dismiss()
                    }

                    fun onPositive(requestCode: Int, result: Any?) {
                        when (requestCode) {
                            MyAlertDialogFragment.Builder.REQUEST_CODE_DEFINE_SIZE -> {
                                if (result != null && result is Size) {
                                    val fragment = EditorFragment.newInstance(result, object : EditorFragment.IListener {
                                        override fun onCanvasSizeError(size: Size) {
                                            Snackbar
                                                    .make(activity.findViewById(R.id.cover), R.string.problem_fragment_error_invalid_size, Snackbar.LENGTH_SHORT)
                                                    .show()
                                        }
                                    })
                                    if (fragment != null) {
                                        fragmentManager.beginTransaction()
                                                .replace(R.id.container, fragment)
                                                .addToBackStack(null)
                                                .commit()
                                    }
                                }
                            }
                        }
                    }
                }, this)
                        .setTitle(getString(R.string.dialog_alert_title_size_define))
                        .setLayout(R.layout.dialog_define_size)
                        .setRequestCode(MyAlertDialogFragment.Builder.REQUEST_CODE_DEFINE_SIZE)
                        .setCancelable(true)
                        .commit()
                fragment.show(fragmentManager, MyAlertDialogFragment.Builder.TAG_DEFINE_SIZE)
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = adapter

        val itemTouchHelper = getItemTouchHelper()
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.addItemDecoration(itemTouchHelper)

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = true
            fetchProblems()
            binding.swipeRefresh.isRefreshing = false
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
        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    fun getAdapter(): ProblemsListAdapter {
        return ProblemsListAdapter(object: ProblemsListAdapter.IListener {
            override fun onClickProblemItem(position: Int) {

            }

            override fun onLongClickProblemItem(position: Int): Boolean {
                return true
            }
        })
    }

    fun getItemTouchHelper(): ItemTouchHelper {
        return ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                val position = viewHolder?.adapterPosition
                if (position != null) {
                    OrmaProvider.db.deleteFromProblem()
                            .idEq(adapter.getProblemByIndex(position)?.id ?: -1)
                            .executeAsSingle()
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .compose(this@ProblemsFragment.bindToLifecycle<Int>())
                            .subscribe({ deleteNum -> run {
                                if (deleteNum > 0) {
                                    Snackbar.make(activity.findViewById(R.id.container),
                                            R.string.problem_fragment_message_complete_delete,
                                            Snackbar.LENGTH_SHORT).show()
                                    adapter.removeProblemsByIndex(position)
                                } else {
                                    Log.d("onSwiped", "id: $deleteNum")
                                    Snackbar.make(activity.findViewById(R.id.container),
                                            R.string.problem_fragment_error_failure_delete,
                                            Snackbar.LENGTH_SHORT).show()
                                }
                            } }, { throwable -> run {
                                throwable.printStackTrace()
                                Snackbar.make(activity.findViewById(R.id.container),
                                        R.string.problem_fragment_error_failure_delete,
                                        Snackbar.LENGTH_SHORT).show()
                            } })
                }
            }
        })
    }

    fun fetchProblems() {
        adapter.clearProblems()
        val problems = OrmaProvider.db.selectFromProblem().orderBy("editedAt").toList()
        adapter.addProblems(problems)
    }
}