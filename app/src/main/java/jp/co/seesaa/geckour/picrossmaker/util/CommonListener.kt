package jp.co.seesaa.geckour.picrossmaker.util

import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.view.MenuItem
import jp.co.seesaa.geckour.picrossmaker.R

/**
 * Created by geckour on 2017/02/14.
 */
class CommonListener {
    companion object {
        fun onNavigationItemSelected(item: MenuItem, drawer: DrawerLayout): Boolean {
            val id = item.itemId
            when (id) {
                R.id.nav_camera -> {

                }
                R.id.nav_gallery -> {

                }
                R.id.nav_slideshow -> {

                }
                R.id.nav_manage -> {

                }
                R.id.nav_share -> {

                }
                R.id.nav_send -> {

                }
            }

            drawer.closeDrawer(GravityCompat.START)
            return true
        }
    }
}