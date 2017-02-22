package jp.co.seesaa.geckour.picrossmaker.fragment

import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Size
import android.view.*
import com.trello.rxlifecycle2.components.RxFragment
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentMainBinding
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment
import timber.log.Timber

class MainFragment: RxFragment() {
    private var binding: FragmentMainBinding? = null

    companion object {
        fun newInstance(): MainFragment {
            val fragment = MainFragment()
            return fragment
        }

        val TAG = "mainFragment"
    }

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).supportActionBar?.setTitle(R.string.app_name)

        val fab = activity.findViewById(R.id.fab) as FloatingActionButton
        fab.visibility = View.VISIBLE
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        return binding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val fab = activity.findViewById(R.id.fab) as FloatingActionButton
        fab.setImageResource(R.drawable.ic_add_white_24px)
        fab.setOnClickListener {
            view ->
            run {
                val fragment = MyAlertDialogFragment.Builder(object : MyAlertDialogFragment.IListener {
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
                                                    .make(activity.findViewById(R.id.container), R.string.error_editor_fragment_invalid_size, Snackbar.LENGTH_SHORT)
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

        binding?.recyclerView?.layoutManager = LinearLayoutManager(activity)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

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
}