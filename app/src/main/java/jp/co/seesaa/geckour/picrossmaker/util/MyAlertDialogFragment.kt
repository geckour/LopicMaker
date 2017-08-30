package jp.co.seesaa.geckour.picrossmaker.util

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import com.trello.rxlifecycle2.components.RxDialogFragment
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.databinding.DialogDefineSizeBinding
import jp.co.seesaa.geckour.picrossmaker.databinding.DialogDefineTitleBinding
import timber.log.Timber

class MyAlertDialogFragment : RxDialogFragment() {

    enum class RequestCode {
        DEFINE_SIZE,
        SAVE_PROBLEM,
        SAVE_DRAFT_PROBLEM,
        UNKNOWN
    }

    companion object {
        private const val ARGS_TITLE = "title"
        private const val ARGS_MESSAGE = "message"
        private const val ARGS_RES_ID = "resId"
        private const val ARGS_REQUEST_CODE = "requestCode"
        private const val ARGS_CANCELABLE = "cancelable"

        fun newInstance(
                title: String = "",
                message: String = "",
                requestCode: RequestCode? = null,
                resId: Int? = null,
                cancelable: Boolean = true,
                targetFragment: Fragment? = null
        ): MyAlertDialogFragment = MyAlertDialogFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(ARGS_TITLE, title)
                        putString(ARGS_MESSAGE, message)
                        requestCode?.let { putSerializable(ARGS_REQUEST_CODE, it) }
                        putInt(ARGS_RES_ID, resId ?: -1)
                        putBoolean(ARGS_CANCELABLE, cancelable)
                    }
                    if (targetFragment != null) setTargetFragment(targetFragment, requestCode?.ordinal ?: 0)
                }

        fun getTag(requestCode: RequestCode): String =
                when (requestCode) {
                    RequestCode.DEFINE_SIZE -> "myAlertDialogFragmentDefineSize"
                    RequestCode.SAVE_DRAFT_PROBLEM -> "myAlertDialogFragmentSaveDraftProblem"
                    RequestCode.SAVE_PROBLEM -> "myAlertDialogFragmentSaveProblem"
                    else -> "myAlertDialogFragment"
                }

        fun showSnackbar(view: View, resId: Int) {
            Snackbar.make(view,
                    resId,
                    Snackbar.LENGTH_SHORT).show()
        }
    }
    private var sizeBinding: DialogDefineSizeBinding? = null
    private var titleBinding: DialogDefineTitleBinding? = null
    private var listener: IListener? = null

    interface IListener {
        fun onResultAlertDialog(dialogInterface: DialogInterface, requestCode: RequestCode, resultCode: Int, result: Any? = null)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        listener = targetFragment as? IListener ?: activity as? IListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState == null) {
            isCancelable = arguments.getBoolean(ARGS_CANCELABLE)
            val builder = AlertDialog.Builder(activity)
                    .setTitle(arguments.getString(ARGS_TITLE))
                    .setMessage(arguments.getString(ARGS_MESSAGE))
                    .setNegativeButton(
                            R.string.dialog_alert_cancel,
                            { dialog, _ -> dialog.dismiss() })
                    .setPositiveButton(
                            R.string.dialog_alert_confirm,
                            { dialog, which -> fireListener(dialog, which) })

            when (arguments.getInt(ARGS_RES_ID, -1)) {
                R.layout.dialog_define_size -> {
                    sizeBinding = DataBindingUtil.inflate(LayoutInflater.from(activity), R.layout.dialog_define_size, null, false)
                    sizeBinding?.let { builder.setView(it.root) }
                    return builder.create()
                }

                R.layout.dialog_define_title -> {
                    titleBinding = DataBindingUtil.inflate(LayoutInflater.from(activity), R.layout.dialog_define_title, null, false)
                    titleBinding?.let { builder.setView(it.root) }
                    return builder.create()
                }
            }
        }

        return super.onCreateDialog(savedInstanceState)
    }

    private fun fireListener(dialogInterface: DialogInterface, which: Int) {
        listener?.onResultAlertDialog(
                dialogInterface,
                getRequestCode(),
                which,
                when (getRequestCode()) {
                    RequestCode.DEFINE_SIZE -> {
                        getSize()
                    }
                    RequestCode.SAVE_DRAFT_PROBLEM, RequestCode.SAVE_PROBLEM -> {
                        getTitle()
                    }
                    else -> null
                }
        )
    }

    private fun getRequestCode(): RequestCode = if (arguments.containsKey(ARGS_REQUEST_CODE)) arguments.getSerializable(ARGS_REQUEST_CODE) as RequestCode else RequestCode.UNKNOWN

    private fun getSize(): Size {
        var width: Int
        var height: Int
        try {
            width = sizeBinding?.editTextSizeWidth?.text?.toString()?.toInt() ?: 0
            height = sizeBinding?.editTextSizeHeight?.text?.toString()?.toInt() ?: 0
        } catch (e: NumberFormatException) {
            width = 0
            height = 0
            Timber.e(e)
        }

        return Size(width, height)
    }

    private fun getTitle(): String? = titleBinding?.editTextProblemTitle?.text?.toString()
}