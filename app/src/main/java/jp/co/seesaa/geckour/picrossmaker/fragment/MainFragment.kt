package jp.co.seesaa.geckour.picrossmaker.fragment

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.trello.rxlifecycle2.components.RxFragment
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.activity.MainActivity
import jp.co.seesaa.geckour.picrossmaker.databinding.FragmentMainBinding
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

        binding?.recyclerView?.layoutManager = LinearLayoutManager(activity)
    }
}