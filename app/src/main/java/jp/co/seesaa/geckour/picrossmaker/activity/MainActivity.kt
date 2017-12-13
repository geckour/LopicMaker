package jp.co.seesaa.geckour.picrossmaker.activity

import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.NavigationView
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toolbar
import com.trello.rxlifecycle2.components.RxActivity
import jp.co.seesaa.geckour.picrossmaker.fragment.ProblemsFragment
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.databinding.ActivityMainBinding
import jp.co.seesaa.geckour.picrossmaker.fragment.DraftProblemsFragment
import jp.co.seesaa.geckour.picrossmaker.fragment.EditorFragment
import jp.co.seesaa.geckour.picrossmaker.fragment.SearchFragment
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment
import jp.co.seesaa.geckour.picrossmaker.util.showSnackbar

class MainActivity : RxActivity(), NavigationView.OnNavigationItemSelectedListener, EditorFragment.IListener, MyAlertDialogFragment.IListener {

    lateinit var binding: ActivityMainBinding
    lateinit var toolbar: Toolbar
    val onClearScrollFlags by lazy { { (toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0 } }
    val onSetScrollFlags by lazy { {
        (toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
    } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.appBarMain?.appBar?.apply {
            toolbar = (LayoutInflater.from(context).inflate(R.layout.toolbar_main, null) as Toolbar).apply {
                viewTreeObserver.addOnGlobalLayoutListener(onClearScrollFlags)
            }
            removeAllViews()
            addView(toolbar)
            setActionBar(toolbar)
        }
        binding.appBarMain?.fabLeft?.hide()

        binding.navView.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            val fragment = ProblemsFragment.newInstance()
            fragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
        }
    }

    override fun onBackPressed() {
        binding.drawerLayout.apply {
            if (isDrawerOpen(Gravity.START)) closeDrawer(Gravity.START)
            else super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    @SuppressWarnings("StatementWithEmptyBody")
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId
        when (id) {
            R.id.nav_problem -> {
                val fragment = ProblemsFragment.newInstance()
                fragmentManager.beginTransaction().replace(R.id.container, fragment).addToBackStack(ProblemsFragment.TAG).commit()
            }

            R.id.nav_draft -> {
                val fragment = DraftProblemsFragment.newInstance()
                fragmentManager.beginTransaction().replace(R.id.container, fragment).addToBackStack(DraftProblemsFragment.TAG).commit()
            }

            R.id.nav_editor -> {
                val requestCode = MyAlertDialogFragment.RequestCode.DEFINE_SIZE
                val fragment = MyAlertDialogFragment.newInstance(
                        title = getString(R.string.dialog_alert_title_size_define),
                        resId = R.layout.dialog_define_size,
                        requestCode = requestCode,
                        cancelable = true
                )
                fragment.show(fragmentManager, MyAlertDialogFragment.getTag(requestCode))
            }

            R.id.nav_setting -> {}

            R.id.nav_search -> {
                val fragment = SearchFragment.createInstance()
                fragmentManager.beginTransaction().replace(R.id.container, fragment).addToBackStack(SearchFragment.tag).commit()
            }
        }

        binding.drawerLayout.closeDrawer(Gravity.START)
        return true
    }

    override fun onCanvasSizeError(size: Size) {
        binding.appBarMain?.contentMain?.container?.let { showSnackbar(it, R.string.problem_fragment_error_invalid_size) }
    }

    override fun onResultAlertDialog(dialogInterface: DialogInterface, requestCode: MyAlertDialogFragment.RequestCode, resultCode: Int, result: Any?) {
        when (resultCode) {
            DialogInterface.BUTTON_POSITIVE -> onPositive(requestCode, result)
        }
        dialogInterface.dismiss()
    }

    private fun onPositive(requestCode: MyAlertDialogFragment.RequestCode, result: Any?) {
        when (requestCode) {
            MyAlertDialogFragment.RequestCode.DEFINE_SIZE -> {
                (result as? Size)?.let {
                    val fragment = EditorFragment.newInstance(it, this@MainActivity)
                    if (fragment != null) fragmentManager.beginTransaction().replace(R.id.container, fragment).addToBackStack(EditorFragment.TAG).commit()
                }
            }
            else -> {}
        }
    }
}
