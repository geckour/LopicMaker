package jp.co.seesaa.geckour.picrossmaker.activity

import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import jp.co.seesaa.geckour.picrossmaker.fragment.MainFragment
import jp.co.seesaa.geckour.picrossmaker.R
import jp.co.seesaa.geckour.picrossmaker.fragment.EditorFragment
import jp.co.seesaa.geckour.picrossmaker.util.CommonListener
import jp.co.seesaa.geckour.picrossmaker.util.MyAlertDialogFragment
import timber.log.Timber

class MainActivity : RxAppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)

        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener {
            view ->
            run {
                val fragment = MyAlertDialogFragment.Builder(object : MyAlertDialogFragment.IListener {
                    override fun onResultAlertDialog(dialogInterface: DialogInterface, requestCode: Int, resultCode: Int, result: Any?) {
                        when (resultCode) {
                            DialogInterface.BUTTON_POSITIVE -> when (requestCode) {
                                MyAlertDialogFragment.Builder.REQUEST_CODE_DEFINE_SIZE -> {
                                    if (result != null && result is Size) {
                                        val fragment = EditorFragment.newInstance(result, object: EditorFragment.IListener {
                                            override fun onCanvasSizeError(size: Size) {
                                                Snackbar
                                                        .make(findViewById(R.id.container), R.string.error_editor_fragment_invalid_size, Snackbar.LENGTH_SHORT)
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
                        dialogInterface.dismiss()
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

        if (savedInstanceState == null) {
            val fragment = MainFragment.newInstance()
            fragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
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

    @SuppressWarnings("StatementWithEmptyBody")
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        return CommonListener.onNavigationItemSelected(item, findViewById(R.id.drawer_layout) as DrawerLayout)
    }
}
