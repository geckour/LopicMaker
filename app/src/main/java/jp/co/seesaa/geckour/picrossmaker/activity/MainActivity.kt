package jp.co.seesaa.geckour.picrossmaker.activity

import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import com.trello.rxlifecycle2.components.RxActivity
import jp.co.seesaa.geckour.picrossmaker.fragment.ProblemsFragment
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.databinding.ActivityMainBinding
import jp.co.seesaa.geckour.picrossmaker.fragment.DraftProblemsFragment
import jp.co.seesaa.geckour.picrossmaker.fragment.EditorFragment
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment.Companion.showSnackbar

class MainActivity : RxActivity(), NavigationView.OnNavigationItemSelectedListener, EditorFragment.IListener, MyAlertDialogFragment.IListener {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setActionBar(binding.appBarMain.toolbar)

        binding.navView.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            val fragment = ProblemsFragment.newInstance()
            fragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
        }
    }

    override fun onBackPressed() {
        val drawer = binding.drawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
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
                fragmentManager.beginTransaction().replace(R.id.container, fragment).addToBackStack(null).commit()
            }

            R.id.nav_draft -> {
                val fragment = DraftProblemsFragment.newInstance()
                fragmentManager.beginTransaction().replace(R.id.container, fragment).addToBackStack(null).commit()
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

            R.id.nav_setting -> {

            }

            R.id.nav_share -> {

            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onCanvasSizeError(size: Size) {
        showSnackbar(binding.appBarMain.contentMain.container, R.string.problem_fragment_error_invalid_size)
    }

    override fun onResultAlertDialog(dialogInterface: DialogInterface, requestCode: MyAlertDialogFragment.RequestCode, resultCode: Int, result: Any?) {
        when (resultCode) {
            DialogInterface.BUTTON_POSITIVE ->
                onPositive(requestCode, result)
        }
        dialogInterface.dismiss()
    }

    private fun onPositive(requestCode: MyAlertDialogFragment.RequestCode, result: Any?) {
        when (requestCode) {
            MyAlertDialogFragment.RequestCode.DEFINE_SIZE -> {
                (result as? Size)?.let {
                    val fragment = EditorFragment.newInstance(it, this@MainActivity)
                    if (fragment != null) {
                        fragmentManager.beginTransaction()
                                .replace(R.id.container, fragment)
                                .addToBackStack(null)
                                .commit()
                    }
                }
            }
            else -> {}
        }
    }
}
