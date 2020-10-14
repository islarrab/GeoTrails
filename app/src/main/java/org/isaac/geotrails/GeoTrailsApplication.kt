package org.isaac.geotrails

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.facebook.stetho.Stetho
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class GeoTrailsApplication : Application() {

    override fun onCreate() {
        AppCompatDelegate.setDefaultNightMode(
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getString("prefColorTheme", "2")?.toInt() ?: 0
        )
        super.onCreate()

        Stetho.initializeWithDefaults(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
