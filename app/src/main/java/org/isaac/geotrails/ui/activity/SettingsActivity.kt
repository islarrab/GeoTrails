package org.isaac.geotrails.ui.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import org.isaac.geotrails.R
import org.isaac.geotrails.ui.fragment.FragmentSettings
import timber.log.Timber

class SettingsActivity : AppCompatActivity() {
    private var toolbar: Toolbar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getInt("prefColorTheme", 2)
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        toolbar = findViewById(R.id.id_toolbar2)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.menu_settings)
        val wvf = FragmentSettings()
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.id_preferences, wvf)
        ft.commit()
    }

    public override fun onResume() {
        super.onResume()
        Timber.v("onResume()")
    }

    public override fun onPause() {
        Timber.v("onPause()")
        super.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}