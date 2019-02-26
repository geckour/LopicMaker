package com.geckour.lopicmaker.ui.main.edit

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.os.Bundle
import android.util.Size
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.geckour.lopicmaker.R
import com.geckour.lopicmaker.databinding.FragmentEditorBinding
import com.geckour.lopicmaker.ui.main.MainViewModel
import com.geckour.lopicmaker.util.Algorithm
import com.geckour.lopicmaker.util.MyAlertDialogFragment
import com.geckour.lopicmaker.util.observe
import timber.log.Timber

class EditorFragment : Fragment(), MyAlertDialogFragment.DialogListener {

    companion object {
        private const val KEY_CANVAS_SIZE = "keyCanvasSize"
        private const val KEY_PROBLEM_ID = "keyProblemId"
        private const val KEY_DRAFT = "keyDraft"
        fun newInstance(size: Size): EditorFragment? {
            if (size.width < 1 || size.height < 1) return null

            return EditorFragment().apply {
                arguments = Bundle().apply { putSize(KEY_CANVAS_SIZE, size) }
            }
        }

        fun newInstance(problemId: Long, isDraft: Boolean): EditorFragment? =
            if (problemId > -1) {
                EditorFragment().apply {
                    arguments = Bundle().apply {
                        putLong(KEY_PROBLEM_ID, problemId)
                        putBoolean(KEY_DRAFT, isDraft)
                    }
                }
            } else null
    }

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProviders.of(requireActivity())[MainViewModel::class.java]
    }
    private val viewModel: EditorViewModel by lazy {
        ViewModelProviders.of(this)[EditorViewModel::class.java]
    }


    private lateinit var binding: FragmentEditorBinding
    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeEvents()

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        arguments?.apply {
            viewModel.draft = getBoolean(KEY_DRAFT, true)
            val problemId = getLong(KEY_PROBLEM_ID, -1)
            val size = getSize(KEY_CANVAS_SIZE)?.let { Point(it.width, it.height) }
            viewModel.initCanvas(requireContext(), binding, problemId, size)
        }

        binding.canvas.setOnTouchListener { _, event ->
            viewModel.onTouchCanvas(binding, event)
        }
        binding.cover.setOnTouchListener { _, event ->
            return@setOnTouchListener when (mainViewModel.fabRightMode.value) {
                MainViewModel.FabRightMode.SCALE -> when (event.action) {
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_POINTER_UP,
                    MotionEvent.ACTION_CANCEL -> {
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
        mainViewModel.fabLeftMode.postValue(MainViewModel.FabLeftMode.UNDO)
    }

    override fun onPause() {
        super.onPause()

        mainViewModel.fabLeftVisible.postValue(false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menu.clear()
        inflater.inflate(R.menu.editor, menu)

        this.menu = menu

        viewModel.refreshSatisfactionState()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        return when (id) {
            R.id.action_save -> {
                viewModel.onSaveCanvas(activity, this)
                true
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onResultAlertDialog(
        dialogInterface: DialogInterface,
        requestCode: MyAlertDialogFragment.RequestCode,
        resultCode: Int,
        result: Any?
    ) {
        when (resultCode) {
            DialogInterface.BUTTON_POSITIVE -> {
                onPositive(requestCode, result)
            }

            DialogInterface.BUTTON_NEUTRAL -> {
                onNeutral(requestCode, result)
            }
        }
    }

    private fun observeEvents() {
        mainViewModel.fabLeftClicked.observe(this) {
            when (it) {
                MainViewModel.FabLeftMode.UNDO ->
                    viewModel.onUndoClicked(binding)
                MainViewModel.FabLeftMode.REDO ->
                    viewModel.onRedoClicked(binding)
                else -> Unit
            }
        }

        viewModel.problem.observe(this) {
            mainViewModel.toolbarTitleResId.postValue(
                if (it == null) R.string.action_bar_title_edit to emptyList()
                else R.string.action_bar_title_edit_with_title to listOf(it.title)
            )
        }

        viewModel.satisfactionState.observe(this) {
            it ?: return@observe

            this.menu?.apply {
                findItem(R.id.action_save)?.apply {
                    icon = requireActivity().getDrawable(
                        if (it == Algorithm.SatisfactionState.Satisfiable)
                            R.drawable.ic_save
                        else R.drawable.ic_bookmark
                    )?.apply {
                        setTint(Color.WHITE)
                    }
                }
            }
        }

        viewModel.fabLeftVisible.observe(this) {
            mainViewModel.fabLeftVisible.postValue(it)
        }

        viewModel.fabLeftMode.observe(this) {
            mainViewModel.fabLeftMode.postValue(it)
        }

        viewModel.snackbarStringResId.observe(this) {
            mainViewModel.snackBarStringResId.postValue(it)
        }

        viewModel.toolbarTitleResId.observe(this) {
            mainViewModel.toolbarTitleResId.postValue(it)
        }
    }

    private fun onPositive(requestCode: MyAlertDialogFragment.RequestCode, result: Any?) {
        when (requestCode) {
            // 問題を保存
            MyAlertDialogFragment.RequestCode.SAVE_PROBLEM -> {
                (result as? String)?.apply {
                    if (this.isNotEmpty()) {
                        Timber.d("result: $this")
                        viewModel.saveProblem(requireContext(), this, false)
                    }
                } ?: viewModel.snackbarStringResId.postValue(R.string.editor_fragment_error_invalid_title)
            }

            // 下書きを保存
            MyAlertDialogFragment.RequestCode.SAVE_DRAFT_PROBLEM -> {
                (result as? String)?.apply {
                    if (this.isNotEmpty()) {
                        Timber.d("result: $this")
                        viewModel.saveProblem(requireContext(), this, true)
                    }
                } ?: viewModel.snackbarStringResId.postValue(R.string.editor_fragment_error_invalid_title)
            }

            // 上書きして問題 / 下書きを保存
            MyAlertDialogFragment.RequestCode.CONFIRM_BEFORE_SAVE -> {
                (result as? String)?.apply {
                    if (this.isNotEmpty()) {
                        viewModel.overwriteProblem(requireContext(), this)
                    }
                } ?: viewModel.snackbarStringResId.postValue(R.string.editor_fragment_error_invalid_title)
            }

            else -> Unit
        }
    }

    private fun onNeutral(requestCode: MyAlertDialogFragment.RequestCode, result: Any?) =
        when (requestCode) {
            MyAlertDialogFragment.RequestCode.CONFIRM_BEFORE_SAVE ->
                viewModel.onSaveCanvas(activity, this, result as String?, false)
            else -> Unit
        }
}