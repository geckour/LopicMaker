package com.geckour.lopicmaker.ui.main.solve

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.geckour.lopicmaker.R
import com.geckour.lopicmaker.databinding.FragmentEditorBinding
import com.geckour.lopicmaker.ui.main.MainViewModel
import com.geckour.lopicmaker.util.observe

class SolveFragment : Fragment() {

    companion object {
        private const val KEY_PROBLEM_ID = "keyProblemId"

        fun newInstance(problemId: Long): SolveFragment? =
            if (problemId > -1) {
                SolveFragment().apply {
                    arguments = Bundle().apply {
                        putLong(KEY_PROBLEM_ID, problemId)
                    }
                }
            } else null
    }

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProviders.of(requireActivity())[MainViewModel::class.java]
    }
    private val viewModel: SolveViewModel by lazy {
        ViewModelProviders.of(this)[SolveViewModel::class.java]
    }

    private lateinit var binding: FragmentEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeEvents()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentEditorBinding.inflate(inflater, container, false).apply { binding = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FIXME: 不正な問題IDを与えられた場合は前のFragmentに戻る・ダイアログを表示
        arguments?.getLong(KEY_PROBLEM_ID, -1)?.apply {
            viewModel.initCanvas(requireContext(), binding, this)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.problem.value?.title?.let {
            mainViewModel.toolbarTitleResId.postValue(R.string.action_bar_title_solve_with_title to listOf(it))
        }

        binding.canvas.setOnTouchListener { _, event ->
            viewModel.onTouchCanvas(requireContext(), binding, event, mainViewModel.fabLeftMode.value)
        }
        binding.cover.setOnTouchListener { _, event ->
            return@setOnTouchListener when (mainViewModel.fabRightMode.value) {
                MainViewModel.FabRightMode.SCALE ->
                    when (event.action) {
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            viewModel.pointPrev0.set(-1f, -1f)
                            viewModel.pointPrev1.set(-1f, -1f)
                            true
                        }

                        else -> {
                            val p0 = PointF(event.getX(0), event.getY(0))
                            if (event.pointerCount > 1) {
                                val p1 = PointF(event.getX(1), event.getY(1))
                                viewModel.onScale(binding, p0, p1)
                            } else viewModel.onDrag(binding, p0)
                        }
                    }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mainViewModel.fabRightMode.postValue(MainViewModel.FabRightMode.EDIT)
        mainViewModel.fabLeftMode.postValue(MainViewModel.FabLeftMode.FILL)
        mainViewModel.fabLeftVisible.postValue(true)
    }

    override fun onPause() {
        super.onPause()

        mainViewModel.fabLeftVisible.postValue(false)
    }

    private fun observeEvents() {
        viewModel.problem.observe(this) {
            it ?: return@observe

            mainViewModel.toolbarTitleResId.postValue(
                R.string.action_bar_title_solve_with_title to listOf(it.title)
            )
        }

        viewModel.snackbarStringResId.observe(this) {
            mainViewModel.snackBarStringResId.postValue(it)
        }
    }
}