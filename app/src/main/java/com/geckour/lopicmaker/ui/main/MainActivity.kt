package com.geckour.lopicmaker.ui.main

import android.content.DialogInterface
import android.os.Bundle
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.geckour.lopicmaker.R
import com.geckour.lopicmaker.databinding.ActivityMainBinding
import com.geckour.lopicmaker.ui.CrashlyticsEnabledActivity
import com.geckour.lopicmaker.ui.main.draft.DraftProblemsFragment
import com.geckour.lopicmaker.ui.main.edit.EditorFragment
import com.geckour.lopicmaker.ui.main.problem.ProblemsFragment
import com.geckour.lopicmaker.ui.main.solve.SolveFragment
import com.geckour.lopicmaker.util.MyAlertDialogFragment
import com.geckour.lopicmaker.util.observe
import com.geckour.lopicmaker.util.showSnackbar

class MainActivity :
    CrashlyticsEnabledActivity(),
    MyAlertDialogFragment.DialogListener {

    private val viewModel: MainViewModel by lazy {
        ViewModelProviders.of(this)[MainViewModel::class.java]
    }
    lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private val onNavigationItemSelected: (MenuItem) -> Boolean = {
        val id = it.itemId
        when (id) {
            R.id.nav_problem -> {
                val fragment = ProblemsFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit()
            }

            R.id.nav_draft -> {
                val fragment = DraftProblemsFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit()
            }

            R.id.nav_editor -> {
                val requestCode = MyAlertDialogFragment.RequestCode.DEFINE_SIZE
                val fragment = MyAlertDialogFragment.newInstance(
                    title = getString(R.string.dialog_alert_title_size_define),
                    resId = R.layout.dialog_define_size,
                    requestCode = requestCode,
                    cancelable = true
                )
                fragment.show(supportFragmentManager, MyAlertDialogFragment.getTag(requestCode))
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeEvents()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setSupportActionBar(binding.contentMain.toolbar as Toolbar)

        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.contentMain.toolbar as Toolbar,
            R.string.accessibility_desc_drawer_open,
            R.string.accessibility_desc_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)

        binding.navView.apply {
            setNavigationItemSelectedListener(onNavigationItemSelected)
            setCheckedItem(R.id.nav_problem)
        }

        binding.contentMain.fabLeft.setOnClickListener {
            viewModel.fabLeftClicked.call()
        }

        if (supportFragmentManager.fragments.isEmpty()) {
            val fragment = ProblemsFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        drawerToggle.syncState()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onResultAlertDialog(
        dialogInterface: DialogInterface,
        requestCode: MyAlertDialogFragment.RequestCode,
        resultCode: Int,
        result: Any?
    ) {
        when (resultCode) {
            DialogInterface.BUTTON_POSITIVE -> onPositive(requestCode, result)
        }
        dialogInterface.dismiss()
    }

    private fun observeEvents() {
        viewModel.toolbarTitleResId.observe(this) {
            it ?: return@observe

            supportActionBar?.title =
                if (it.second.isNotEmpty())
                    getString(it.first, *it.second.toTypedArray())
                else getString(it.first)
        }

        viewModel.fabRightMode.observe(this) { mode ->
            mode ?: return@observe

            binding.contentMain.fabRight.apply {
                setImageResource(
                    when (mode) {
                        MainViewModel.FabRightMode.ADD -> R.drawable.ic_add
                        MainViewModel.FabRightMode.EDIT -> R.drawable.ic_edit
                        MainViewModel.FabRightMode.SCALE -> R.drawable.ic_crop_free
                    }
                )
                setOnClickListener {
                    when (mode) {
                        MainViewModel.FabRightMode.ADD -> {
                            val requestCode = MyAlertDialogFragment.RequestCode.DEFINE_SIZE
                            val fragment = MyAlertDialogFragment.newInstance(
                                title = getString(R.string.dialog_alert_title_size_define),
                                resId = R.layout.dialog_define_size,
                                requestCode = requestCode,
                                cancelable = true
                            )
                            fragment.show(supportFragmentManager, MyAlertDialogFragment.getTag(requestCode))
                        }
                        MainViewModel.FabRightMode.EDIT -> {
                            viewModel.fabRightMode.postValue(MainViewModel.FabRightMode.SCALE)
                        }
                        MainViewModel.FabRightMode.SCALE -> {
                            viewModel.fabRightMode.postValue(MainViewModel.FabRightMode.EDIT)
                        }
                    }
                }
            }
        }

        viewModel.fabRightVisible.observe(this) {
            it ?: return@observe

            binding.contentMain.fabRight.apply {
                if (it) show()
                else hide()
            }
        }

        viewModel.fabLeftMode.observe(this) { mode ->
            mode ?: return@observe

            binding.contentMain.fabLeft.apply {
                setImageResource(
                    when (mode) {
                        MainViewModel.FabLeftMode.UNDO -> R.drawable.ic_undo
                        MainViewModel.FabLeftMode.FILL -> R.drawable.ic_fill
                        MainViewModel.FabLeftMode.MARK_NOT_FILL -> R.drawable.ic_close
                    }
                )
                setOnClickListener {
                    when (mode) {
                        MainViewModel.FabLeftMode.UNDO -> {
                            viewModel.fabLeftClicked.call()
                        }
                        MainViewModel.FabLeftMode.FILL -> {
                            viewModel.fabLeftMode.postValue(MainViewModel.FabLeftMode.MARK_NOT_FILL)
                        }
                        MainViewModel.FabLeftMode.MARK_NOT_FILL -> {
                            viewModel.fabLeftMode.postValue(MainViewModel.FabLeftMode.FILL)
                        }
                    }
                }
            }
        }

        viewModel.fabLeftVisible.observe(this) {
            it ?: return@observe

            binding.contentMain.fabLeft.apply {
                if (it) show()
                else hide()
            }
        }

        viewModel.snackBarStringResId.observe(this) {
            it ?: return@observe

            showSnackbar(binding.root, it)
        }

        viewModel.toSolveProblemId.observe(this) {
            it ?: return@observe

            val fragment = SolveFragment.newInstance(it)
            if (fragment != null) {
                supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.container, fragment)
                    ?.addToBackStack(null)
                    ?.commit()
            }
        }

        viewModel.toEditProblemId.observe(this) {
            it ?: return@observe

            val fragment = EditorFragment.newInstance(it, false)
            if (fragment != null) {
                supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.container, fragment)
                    ?.addToBackStack(null)
                    ?.commit()
            }
        }

        viewModel.toEditDraftProblemId.observe(this) {
            it ?: return@observe

            val fragment = EditorFragment.newInstance(it, true)
            if (fragment != null) {
                supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.container, fragment)
                    ?.addToBackStack(null)
                    ?.commit()
            }
        }
    }

    private fun onPositive(requestCode: MyAlertDialogFragment.RequestCode, result: Any?) {
        when (requestCode) {
            MyAlertDialogFragment.RequestCode.DEFINE_SIZE -> {
                (result as? Size)?.apply {
                    val fragment = EditorFragment.newInstance(this)
                    if (fragment != null) {
                        supportFragmentManager.beginTransaction().replace(
                            R.id.container,
                            fragment
                        ).addToBackStack(null).commit()
                    } else {
                        showSnackbar(
                            binding.root,
                            R.string.problem_fragment_error_invalid_size
                        )
                    }
                }
            }
            else -> Unit
        }
    }
}
