package jp.co.seesaa.geckour.picrossmaker.util

import android.app.*
import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.databinding.DialogDefineSizeBinding
import timber.log.Timber

class MyAlertDialogFragment(val listener: IListener) : DialogFragment() {
    private var binding: DialogDefineSizeBinding? = null

    interface IListener {
        fun onResultAlertDialog(dialogInterface: DialogInterface, requestCode: Int, resultCode: Int, result: Any? = null)
    }

    class Builder(val listener: IListener, parent: Any) {
        private var parentFragment: Fragment? = null
        private var parentActivity: Activity? = null

        companion object {
            val ARGS_TITLE = "title"
            val ARGS_MESSAGE = "message"
            val ARGS_RES_ID = "resId"
            val ARGS_REQUEST_CODE = "requestCode"
            val ARGS_CANCELABLE = "cancelable"
            val TAG_DEFINE_SIZE = "defineSize"
            val REQUEST_CODE_DEFINE_SIZE = 1
        }

        init {
            require(parent is Activity || parent is Fragment)

            if (parent is Activity) parentActivity = parent
            if (parent is Fragment) parentFragment = parent
        }

        private var title: String = ""
        private var message: String = ""
        private var resId: Int? = null
        private var requestCode: Int = -1
        private var cancelable: Boolean = true

        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        fun setLayout(resId: Int): Builder {
            this.resId = resId
            return this
        }

        fun setRequestCode(requestCode: Int): Builder {
            this.requestCode = requestCode
            return this
        }

        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        fun commit(): MyAlertDialogFragment {
            val args = Bundle()
            args.putString(ARGS_TITLE, title)
            args.putString(ARGS_MESSAGE, message)
            args.putInt(ARGS_RES_ID, resId ?: -1)
            args.putBoolean(ARGS_CANCELABLE, cancelable)

            val fragment = MyAlertDialogFragment(listener)
            if (parentFragment != null) {
                fragment.setTargetFragment(parentFragment, requestCode)
            } else {
                args.putInt(ARGS_REQUEST_CODE, requestCode)
            }

            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        when (getRequestCode()) {
            Builder.REQUEST_CODE_DEFINE_SIZE -> {
                binding = DataBindingUtil.inflate(inflater, R.layout.dialog_define_size, container, false)
                if (binding != null) return binding!!.root
                if (inflater != null) return inflater.inflate(R.layout.dialog_define_size, container)
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val resId = arguments.getInt(Builder.ARGS_RES_ID)
        if (savedInstanceState == null) {
            isCancelable = arguments.getBoolean(Builder.ARGS_CANCELABLE)
            val builder = AlertDialog.Builder(activity)
                    .setTitle(arguments.getString(Builder.ARGS_TITLE))
                    .setMessage(arguments.getString(Builder.ARGS_MESSAGE))
                    .setView(resId)
                    .setNegativeButton(
                            R.string.dialog_alert_cancel,
                            { dialogInterface, which -> this.dismiss() })
                    .setPositiveButton(
                            R.string.dialog_alert_confirm,
                            { dialogInterface, which -> fireListener(dialogInterface, which) })

            return builder.create()
        }

        return super.onCreateDialog(savedInstanceState)
    }

    private fun fireListener(dialogInterface: DialogInterface, which: Int) {
        when (getRequestCode()) {
            Builder.REQUEST_CODE_DEFINE_SIZE -> {
                listener.onResultAlertDialog(dialogInterface, getRequestCode(), which, getSize())
            }
            else -> listener.onResultAlertDialog(dialogInterface, getRequestCode(), which)
        }
    }

    private fun getRequestCode(): Int = if (arguments.containsKey(Builder.ARGS_REQUEST_CODE)) arguments.getInt(Builder.ARGS_REQUEST_CODE, -1) else targetRequestCode

    private fun getSize(): Size {
        var width: Int
        var height: Int
        try {
            width = (dialog.findViewById(R.id.edit_text_size_width) as EditText).text.toString().toInt()
            height = (dialog.findViewById(R.id.edit_text_size_height) as EditText).text.toString().toInt()
        } catch (e: NumberFormatException) {
            width = 0
            height = 0
            Timber.e(e)
        }

        return Size(width, height)
    }
}