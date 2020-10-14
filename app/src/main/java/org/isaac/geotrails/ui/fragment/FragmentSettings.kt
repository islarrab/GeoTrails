package org.isaac.geotrails.ui.fragment

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import org.greenrobot.eventbus.EventBus
import org.isaac.geotrails.R
import org.isaac.geotrails.eventbus.EventBusMSG
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class FragmentSettings() : PreferenceFragmentCompat() {
    var prefListener: OnSharedPreferenceChangeListener? = null
    var altcor // manual offset
            = 0.0
    var altcorm // Manual offset in m
            = 0.0
    var Downloaded = false

    private var mProgressDialog: ProgressDialog? = null

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.app_preferences)
        var tsd = File(Environment.getExternalStorageDirectory().toString() + "/GPSLogger")
        var isGPSLoggerFolder = true
        if (!tsd.exists()) {
            isGPSLoggerFolder = tsd.mkdir()
        }
        tsd = File(Environment.getExternalStorageDirectory().toString() + "/GPSLogger/AppData")
        if (!tsd.exists()) {
            isGPSLoggerFolder = tsd.mkdir()
        }
        Log.w(
            "myApp",
            "[#] FragmentSettings.java - " + (if (isGPSLoggerFolder) "Folder /GPSLogger/AppData OK" else "Unable to create folder /GPSLogger/AppData")
        )
        prefs = PreferenceManager.getDefaultSharedPreferences(context)

        // Chech if EGM96 file is downloaded and complete;
        val sd = File(requireActivity().applicationContext.filesDir.toString() + "/WW15MGH.DAC")
        val sd_old = File(
            Environment.getExternalStorageDirectory().toString() + "/GPSLogger/AppData/WW15MGH.DAC"
        )
        if ((sd.exists() && (sd.length() == 2076480L)) || (sd_old.exists() && (sd_old.length() == 2076480L))) {
            Downloaded = true
        } else {
            val settings = PreferenceManager.getDefaultSharedPreferences(
                context
            )
            val editor1 = settings.edit()
            editor1.putBoolean("prefEGM96AltitudeCorrection", false)
            editor1.apply()
            val EGM96 =
                super.findPreference<Preference>("prefEGM96AltitudeCorrection") as SwitchPreferenceCompat?
            EGM96!!.isChecked = false
        }

        // Instantiate Progress dialog
        mProgressDialog = ProgressDialog(activity)
        mProgressDialog!!.isIndeterminate = true
        mProgressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        mProgressDialog!!.setCancelable(true)
        mProgressDialog!!.setMessage(getString(R.string.pref_EGM96AltitudeCorrection_download_progress))
        prefListener = object : OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(
                sharedPreferences: SharedPreferences,
                key: String
            ) {
                if ((key == "prefUM")) {
                    altcorm =
                        prefs.getString("prefAltitudeCorrection", "0.0")?.toDouble() ?: 0.0
                    altcor =
                        if ((prefs.getString("prefUM", "0") == "0")) altcorm else altcorm * M_TO_FT
                    val editor = prefs.edit()
                    editor.putString("prefAltitudeCorrectionRaw", altcor.toString())
                    editor.apply()
                    val pAltitudeCorrection =
                        findPreference<Preference>("prefAltitudeCorrectionRaw") as EditTextPreference?
                    pAltitudeCorrection!!.text = prefs.getString("prefAltitudeCorrectionRaw", "0")
                }
                if ((key == "prefAltitudeCorrectionRaw")) {
                    try {
                        val d = sharedPreferences.getString("prefAltitudeCorrectionRaw", "0")!!
                            .toDouble()
                        altcor = d
                    } catch (nfe: NumberFormatException) {
                        altcor = 0.0
                        val Alt =
                            findPreference<Preference>("prefAltitudeCorrectionRaw") as EditTextPreference?
                        Alt!!.text = "0"
                    }
                    altcorm =
                        if ((prefs.getString("prefUM", "0") == "0")) altcor else altcor / M_TO_FT
                    val editor = prefs.edit()
                    editor.putString("prefAltitudeCorrection", altcorm.toString())
                    editor.apply()
                }
                if ((key == "prefEGM96AltitudeCorrection")) {
                    if (sharedPreferences.getBoolean(key, false)) {
                        if (!Downloaded) {
                            // execute this when the downloader must be fired
                            val downloadTask = DownloadTask(activity)
                            downloadTask.execute("http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm96/binary/WW15MGH.DAC")
                            mProgressDialog!!.setOnCancelListener(DialogInterface.OnCancelListener {
                                downloadTask.cancel(
                                    true
                                )
                            })
                            PrefEGM96SetToFalse()
                        }
                    }
                }
                if ((key == "prefColorTheme")) {
                    val settings = PreferenceManager.getDefaultSharedPreferences(
                        context
                    )
                    val editor1 = settings.edit()
                    editor1.putString(key, sharedPreferences.getString(key, "2"))
                    editor1.apply()
                    AppCompatDelegate.setDefaultNightMode(
                        PreferenceManager.getDefaultSharedPreferences(context)
                            .getInt("prefColorTheme", 2)
                    )
                    activity!!.window.setWindowAnimations(R.style.MyCrossfadeAnimation_Window)
                    activity!!.recreate()
                }
                SetupPreferences()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Remove dividers between preferences
        setDivider(ColorDrawable(Color.TRANSPARENT))
        setDividerHeight(0)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        //Log.w("myApp", "[#] FragmentSettings.java - onResume");
        SetupPreferences()
    }

    override fun onPause() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        Log.w("myApp", "[#] FragmentSettings.java - onPause")
        EventBus.getDefault().post(EventBusMSG.UPDATE_SETTINGS)
        super.onPause()
    }

    override fun onCreatePreferences(bundle: Bundle, s: String) {
        Log.w("myApp", "[#] FragmentSettings.java - onCreatePreferences")
    }

    fun SetupPreferences() {
        val pUM = findPreference<Preference>("prefUM") as ListPreference?
        val pUMSpeed = findPreference<Preference>("prefUMSpeed") as ListPreference?
        val pGPSDistance = findPreference<Preference>("prefGPSdistance") as ListPreference?
        val pGPSUpdateFrequency =
            findPreference<Preference>("prefGPSupdatefrequency") as ListPreference?
        val pKMLAltitudeMode = findPreference<Preference>("prefKMLAltitudeMode") as ListPreference?
        val pGPXVersion = findPreference<Preference>("prefGPXVersion") as ListPreference?
        val pShowTrackStatsType =
            findPreference<Preference>("prefShowTrackStatsType") as ListPreference?
        val pShowDirections = findPreference<Preference>("prefShowDirections") as ListPreference?
        val pViewTracksWith = findPreference<Preference>("prefViewTracksWith") as ListPreference?
        val pColorTheme = findPreference<Preference>("prefColorTheme") as ListPreference?
        val pAltitudeCorrection =
            findPreference<Preference>("prefAltitudeCorrectionRaw") as EditTextPreference?
        altcorm = prefs.getString("prefAltitudeCorrection", "0.0")?.toDouble() ?: 0.0
        altcor = if ((prefs.getString("prefUM", "0") == "0")) altcorm else altcorm * M_TO_FT
        if ((prefs.getString("prefUM", "0") == "0")) {       // Metric
            pUMSpeed?.setEntries(R.array.UMSpeed_Metric)
            pGPSDistance?.setEntries(R.array.GPSDistance_Metric)
            pAltitudeCorrection?.summary =
                if (altcor != 0.0) getString(R.string.pref_AltitudeCorrection_summary_offset).toString() + " = " + java.lang.Double.valueOf(
                    Math.round(altcor * 1000.0) / 1000.0
                )
                    .toString() + " m" else getString(R.string.pref_AltitudeCorrection_summary_not_defined)
        }
        if ((prefs.getString("prefUM", "0") == "8")) {       // Imperial
            pUMSpeed?.setEntries(R.array.UMSpeed_Imperial)
            pGPSDistance?.setEntries(R.array.GPSDistance_Imperial)
            pAltitudeCorrection?.summary =
                if (altcor != 0.0) getString(R.string.pref_AltitudeCorrection_summary_offset).toString() + " = " + java.lang.Double.valueOf(
                    Math.round(altcor * 1000.0) / 1000.0
                )
                    .toString() + " ft" else getString(R.string.pref_AltitudeCorrection_summary_not_defined)
        }
        if ((prefs.getString("prefUM", "0") == "16")) {       // Aerial / Nautical
            pUMSpeed?.setEntries(R.array.UMSpeed_AerialNautical)
            pGPSDistance?.setEntries(R.array.GPSDistance_Imperial)
            pAltitudeCorrection?.summary =
                if (altcor != 0.0) getString(R.string.pref_AltitudeCorrection_summary_offset).toString() + " = " + java.lang.Double.valueOf(
                    Math.round(altcor * 1000.0) / 1000.0
                )
                    .toString() + " ft" else getString(R.string.pref_AltitudeCorrection_summary_not_defined)
        }
        Log.w(
            "myApp",
            "[#] FragmentSettings.java - prefAltitudeCorrectionRaw = " + prefs.getString(
                "prefAltitudeCorrectionRaw",
                "0"
            )
        )
        Log.w(
            "myApp",
            "[#] FragmentSettings.java - prefAltitudeCorrection = " + prefs.getString(
                "prefAltitudeCorrection",
                "0"
            )
        )

        // Set all summaries
        pColorTheme!!.summary = pColorTheme.entry
        pUMSpeed!!.summary = pUMSpeed.entry
        pUM!!.summary = pUM.entry
        pGPSDistance!!.summary = pGPSDistance.entry
        pGPSUpdateFrequency!!.summary = pGPSUpdateFrequency.entry
        pKMLAltitudeMode!!.summary = pKMLAltitudeMode.entry
        pGPXVersion!!.summary = pGPXVersion.entry
        pShowTrackStatsType!!.summary = pShowTrackStatsType.entry
        pShowDirections!!.summary = pShowDirections.entry
        pViewTracksWith!!.summary = pViewTracksWith.entry
    }

    fun PrefEGM96SetToFalse() {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = settings.edit()
        editor.putBoolean("prefEGM96AltitudeCorrection", false)
        editor.apply()
        val useEgm96 =
            findPreference<Preference>("prefEGM96AltitudeCorrection") as SwitchPreferenceCompat?
        useEgm96?.isChecked = false
    }

    fun PrefEGM96SetToTrue() {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = settings.edit()
        editor.putBoolean("prefEGM96AltitudeCorrection", true)
        editor.apply()
        val useEgm96 =
            super.findPreference<Preference>("prefEGM96AltitudeCorrection") as SwitchPreferenceCompat?
        useEgm96?.isChecked = true
    }

    // ----------------------------------------------------------------.----- EGM96 - Download file
    // usually, subclasses of AsyncTask are declared inside the activity class.
    // that way, you can easily modify the UI thread from here
    private inner class DownloadTask     //private PowerManager.WakeLock mWakeLock;
        (private val context: Context?) : AsyncTask<String?, Int?, String?>() {
        // Disables the SSL certificate checking for new instances of {@link HttpsURLConnection} This has been created to
        // usually aid testing on a local box, not for use on production. On this case it is OK
        // Code found on https://gist.github.com/tobiasrohloff/72e32bc4e215522c4bcc
        private fun disableSSLCertificateChecking() {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }

                @Throws(CertificateException::class)
                override fun checkClientTrusted(arg0: Array<X509Certificate>, arg1: String) {
                    // Not implemented
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(arg0: Array<X509Certificate>, arg1: String) {
                    // Not implemented
                }
            })
            try {
                val sc = SSLContext.getInstance("TLS")
                sc.init(null, trustAllCerts, SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            } catch (e: KeyManagementException) {
                e.printStackTrace()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
        }

        override fun doInBackground(vararg sUrl: String?): String? {
            var redirect = false
            var HTTPSUrl = ""
            var input: InputStream? = null
            var output: OutputStream? = null
            var connection: HttpURLConnection? = null
            try {
                val url = URL(sUrl[0])
                connection = url.openConnection() as HttpURLConnection
                connection!!.instanceFollowRedirects = true
                connection.connect()

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    if (((connection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP)
                                || (connection.responseCode == HttpURLConnection.HTTP_MOVED_PERM)
                                || (connection.responseCode == HttpURLConnection.HTTP_SEE_OTHER))
                    ) {
                        // REDIRECTED !!
                        HTTPSUrl = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (HTTPSUrl.startsWith("https")) {
                            redirect = true
                            Log.w(
                                "myApp",
                                "[#] FragmentSettings.java - Download of EGM Grid redirected to $HTTPSUrl"
                            )
                        }
                    } else return ("Server returned HTTP " + connection.responseCode
                            + " " + connection.responseMessage)
                }
                if (!redirect) {
                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    val fileLength = connection.contentLength

                    // download the file
                    input = connection.inputStream
                    output =
                        FileOutputStream(activity!!.applicationContext.filesDir.toString() + "/WW15MGH.DAC")
                    val data = ByteArray(4096)
                    var total: Long = 0
                    var count: Int
                    while ((input.read(data).also { count = it }) != -1) {
                        // allow canceling with back button
                        if (isCancelled) {
                            input.close()
                            return null
                        }
                        total += count.toLong()
                        // publishing the progress....
                        if (fileLength > 0) // only if total length is known
                            publishProgress((total * 2028 / fileLength).toInt())
                        output.write(data, 0, count)
                    }
                }
            } catch (e: Exception) {
                return e.toString()
            } finally {
                try {
                    output?.close()
                    input?.close()
                } catch (ignored: IOException) {
                }
                connection?.disconnect()
            }
            if (!redirect) return null else {
                // REDIRECTION. Try with HTTPS:
                var connection_https: HttpsURLConnection? = null
                try {
                    val url = URL(HTTPSUrl)
                    connection_https = url.openConnection() as HttpsURLConnection
                    connection_https!!.instanceFollowRedirects = true
                    disableSSLCertificateChecking()
                    connection_https = url.openConnection() as HttpsURLConnection
                    connection_https.connect()

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connection_https.responseCode != HttpURLConnection.HTTP_OK) {
                        return ("Server returned HTTP " + connection_https.responseCode
                                + " " + connection_https.responseMessage)
                    }

                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    val fileLength = connection_https.contentLength

                    // download the file
                    input = connection_https.inputStream
                    output =
                        FileOutputStream(activity!!.applicationContext.filesDir.toString() + "/WW15MGH.DAC")
                    val data = ByteArray(4096)
                    var total: Long = 0
                    var count: Int
                    while ((input.read(data).also { count = it }) != -1) {
                        // allow canceling with back button
                        if (isCancelled) {
                            input.close()
                            return null
                        }
                        total += count.toLong()
                        // publishing the progress....
                        if (fileLength > 0) // only if total length is known
                            publishProgress((total * 2028 / fileLength).toInt())
                        output.write(data, 0, count)
                    }
                } catch (e: Exception) {
                    return e.toString()
                } finally {
                    try {
                        output?.close()
                        input?.close()
                    } catch (ignored: IOException) {
                    }
                    connection_https?.disconnect()
                }
                return null
            }
        }

        override fun onPreExecute() {
            super.onPreExecute()
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            //PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
            //        getClass().getName());
            //mWakeLock.acquire();
            mProgressDialog!!.show()
        }

        override fun onProgressUpdate(vararg progress: Int?) {
            super.onProgressUpdate(*progress)
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog!!.isIndeterminate = false
            mProgressDialog!!.max = 2028
            mProgressDialog!!.progress = progress.get(0) ?: 0
        }

        override fun onPostExecute(result: String?) {
            if (activity != null) {
                //mWakeLock.release();
                mProgressDialog!!.dismiss()
                if (result != null) Toast.makeText(
                    context,
                    getString(R.string.toast_download_error) + ": " + result,
                    Toast.LENGTH_LONG
                ).show() else {
                    val sd =
                        File(activity!!.applicationContext.filesDir.toString() + "/WW15MGH.DAC")
                    val sd_old = File(
                        Environment.getExternalStorageDirectory()
                            .toString() + "/GPSLogger/AppData/WW15MGH.DAC"
                    )
                    if ((sd.exists() && (sd.length() == 2076480L)) || (sd_old.exists() && (sd_old.length() == 2076480L))) {
                        Downloaded = true
                        Toast.makeText(
                            context,
                            getString(R.string.toast_download_completed),
                            Toast.LENGTH_SHORT
                        ).show()
                        PrefEGM96SetToTrue()

                        // Ask to switch to Absolute Altitude Mode if not already active.
                        /*
                        ListPreference pKMLAltitudeMode = (ListPreference) findPreference("prefKMLAltitudeMode");
                        if (!(pKMLAltitudeMode.getValue().equals("0"))) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.StyledDialog));
                            builder.setMessage(getResources().getString(R.string.pref_message_switch_to_absolute_altitude_mode));
                            builder.setIcon(android.R.drawable.ic_menu_info_details);
                            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                                    SharedPreferences.Editor editor1 = settings.edit();
                                    editor1.putString("prefKMLAltitudeMode", "0");
                                    editor1.apply();
                                    ListPreference pKMLAltitudeMode = (ListPreference) findPreference("prefKMLAltitudeMode");
                                    pKMLAltitudeMode.setValue("0");
                                    pKMLAltitudeMode.setSummary(R.string.pref_KML_altitude_mode_absolute);
                                }
                            });
                            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                        */
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.toast_download_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    companion object {
        private val M_TO_FT = 3.280839895f
    }
}