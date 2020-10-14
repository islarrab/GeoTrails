package org.isaac.geotrails.util

import org.greenrobot.eventbus.EventBus
import org.isaac.geotrails.eventbus.EventBusMSG
import timber.log.Timber
import java.io.*

object EGM96 {

    private const val EGM96_VALUE_INVALID = -100000.0

    // The grid extensions (in each of the 4 sides) of the real 721 x 1440 grid
    private const val BOUNDARY = 3
    private val EGM_GRID = Array(BOUNDARY + 1440 + BOUNDARY) {
        ShortArray(
            BOUNDARY + 721 + BOUNDARY
        )
    }

    private var isEgmFileCopying = false
    private var egmFilename: String? = null
    private var egmFileNameLocalCopy: String? = null

    var isEgmGridLoaded = false
        private set
    var isEGMGridLoading = false
        private set

    fun LoadGridFromFile(FileName: String?, FileNameLocalCopy: String?) {
        when {
            isEGMGridLoading -> {
                Timber.w("Grid is already loading")
                return
            }
            isEgmGridLoaded -> {
                Timber.i("Grid already loaded")
            }
            else -> {
                isEGMGridLoading = true
                egmFilename = FileName
                egmFileNameLocalCopy = FileNameLocalCopy
                Thread(LoadEGM96Grid()).start()
            }
        }
    }

    fun UnloadGrid() {
        isEgmGridLoaded = false
        isEGMGridLoading = false
        //listener.onEGMGridLoaded(isEGMGridLoaded);
        EventBus.getDefault().post(EventBusMSG.UPDATE_FIX)
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK)
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACKLIST)
    }

    fun getEGMCorrection(Latitude: Double, Longitude: Double): Double {
        // This function calculates and return the EGM96 altitude correction value of the input coordinates, in m;
        // Input coordinates are: -90 < Latitude < 90; -180 < Longitude < 360 (android range -180 < Longitude < 180);
        return if (isEgmGridLoaded) {
            val Lat = 90.0 - Latitude
            var Lon = Longitude
            if (Lon < 0) Lon += 360.0
            val ilon = (Lon / 0.25).toInt() + BOUNDARY
            val ilat = (Lat / 0.25).toInt() + BOUNDARY
            try {
                // Creating points for interpolation
                val hc11 = EGM_GRID[ilon][ilat]
                val hc12 = EGM_GRID[ilon][ilat + 1]
                val hc21 = EGM_GRID[ilon + 1][ilat]
                val hc22 = EGM_GRID[ilon + 1][ilat + 1]

                // Interpolation:
                // Latitude
                val hc1 = hc11 + (hc12 - hc11) * (Lat % 0.25) / 0.25
                val hc2 = hc21 + (hc22 - hc21) * (Lat % 0.25) / 0.25
                // Longitude
                //double hc = (hc1 + (hc2 - hc1) * (Lon % 0.25) / 0.25) / 100;
                //Timber.d("getEGMCorrection(" + Latitude + ", " + Longitude + ") = " + hc);
                (hc1 + (hc2 - hc1) * (Lon % 0.25) / 0.25) / 100
            } catch (e: ArrayIndexOutOfBoundsException) {
                EGM96_VALUE_INVALID
            }
        } else EGM96_VALUE_INVALID
    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }

    private fun deleteFile(filename: String?) {
        if (!filename.isNullOrBlank()) {
            val file = File(filename)
            if (file.exists()) file.delete()
        }
    }

    // The Thread that loads the grid in background ------------------------------------------------
    private class LoadEGM96Grid : Runnable {
        // Thread: Load EGM grid
        override fun run() {
            Thread.currentThread().priority = Thread.MIN_PRIORITY
            Timber.d("Start loading grid")
            var islocalcopypresent = false
            var issharedcopypresent = false
            val localfile = File(egmFileNameLocalCopy)
            if (localfile.exists() && localfile.length() == 2076480L) islocalcopypresent = true
            val sharedfile = File(egmFilename)
            if (sharedfile.exists() && sharedfile.length() == 2076480L) issharedcopypresent = true
            val file = File(if (islocalcopypresent) egmFileNameLocalCopy else egmFilename)
            if (islocalcopypresent || issharedcopypresent) {
                Timber.d("From file: ${file.absolutePath}")
                val fin: FileInputStream
                try {
                    fin = FileInputStream(file)
                } catch (e: FileNotFoundException) {
                    isEgmGridLoaded = false
                    isEGMGridLoading = false
                    Timber.d("FileNotFoundException")
                    //Toast.makeText(getApplicationContext(), "Oops", Toast.LENGTH_SHORT).show();
                    //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    return
                }
                val bin = BufferedInputStream(fin)
                val din = DataInputStream(bin)
                var i = 0
                var iLon = BOUNDARY
                var iLat = BOUNDARY
                val count = (file.length() / 2).toInt()
                while (i < count) {
                    try {
                        EGM_GRID[iLon][iLat] = din.readShort()
                        iLon++
                        if (iLon >= 1440 + BOUNDARY) {
                            iLat++
                            iLon = BOUNDARY
                        }
                    } catch (e: IOException) {
                        isEgmGridLoaded = false
                        isEGMGridLoading = false
                        Timber.d("IOException")
                        return
                        //Toast.makeText(getApplicationContext(), "Oops", Toast.LENGTH_SHORT).show();
                        //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    i++
                }
                if (BOUNDARY > 0) {
                    // Fill boundaries with correct data, in order to speed up retrieving for interpolation;
                    // fill left + right boundaries
                    //Timber.d("LR BOUNDARIES");
                    var ix = 0
                    while (ix < BOUNDARY) {
                        var iy = BOUNDARY
                        while (iy < BOUNDARY + 721) {
                            EGM_GRID[ix][iy] = EGM_GRID[ix + 1440][iy]
                            EGM_GRID[BOUNDARY + ix + 1440][iy] = EGM_GRID[BOUNDARY + ix][iy]
                            iy++
                        }
                        ix++
                    }
                    // fill top + bottom boundaries
                    //Timber.d("TOP DOWN BOUNDARIES");
                    var iy = 0
                    while (iy < BOUNDARY) {
                        ix = 0
                        while (ix < BOUNDARY + 1440 + BOUNDARY) {
                            if (ix > 720) {
                                EGM_GRID[ix][iy] = EGM_GRID[ix - 720][BOUNDARY + BOUNDARY - iy]
                                EGM_GRID[ix][BOUNDARY + iy + 721] =
                                    EGM_GRID[ix - 720][BOUNDARY + 721 - 2 - iy]
                            } else {
                                EGM_GRID[ix][iy] = EGM_GRID[ix + 720][BOUNDARY + BOUNDARY - iy]
                                EGM_GRID[ix][BOUNDARY + iy + 721] =
                                    EGM_GRID[ix + 720][BOUNDARY + 721 - 2 - iy]
                            }
                            ix++
                        }
                        iy++
                    }
                }
                isEGMGridLoading = false
                isEgmGridLoaded = true
                Timber.d("Grid Successfully Loaded: ${file.absolutePath}")
                //Toast.makeText(getApplicationContext(), "EGM96 correction grid loaded", Toast.LENGTH_SHORT).show();
                if (issharedcopypresent) {
                    if (!islocalcopypresent) Thread(CopyEGM96Grid()).start() else {
                        deleteFile(egmFilename) // Delete the EGM file from the shared folder
                        Timber.d("EGM File already present into FilesDir. File deleted from shared folder")
                    }
                }
            } else {
                isEGMGridLoading = false
                isEgmGridLoaded = false
                if (!file.exists()) {
                    Timber.d("File not found")
                }
                if (!file.canRead()) {
                    Timber.d("Cannot read file")
                }
                if (file.length() != 2076480L) {
                    Timber.d("File has invalid length: ${file.length()}")
                }
                //Toast.makeText(getApplicationContext(), "EGM96 correction not available", Toast.LENGTH_SHORT).show();
            }
            EventBus.getDefault().post(EventBusMSG.UPDATE_FIX)
            EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK)
            EventBus.getDefault().post(EventBusMSG.UPDATE_TRACKLIST)
            //listener.onEGMGridLoaded(isEGMGridLoaded);
        }
    }

    // The Thread that copies the EGM grid in FilesDir (in background) -----------------------------
    private class CopyEGM96Grid : Runnable {
        // Thread: Copy the EGM grid in FilesDir
        override fun run() {
            Thread.currentThread().priority = Thread.MIN_PRIORITY
            Timber.d("Copy EGM96 Grid into FilesDir")
            if (isEgmFileCopying) return
            isEgmFileCopying = true
            val sd_cpy = File(egmFileNameLocalCopy)
            if (sd_cpy.exists()) sd_cpy.delete()
            val sd_old = File(egmFilename)
            if (sd_old.exists()) {
                var `in`: InputStream? = null
                var out: OutputStream? = null
                try {
                    `in` = FileInputStream(egmFilename)
                    out = FileOutputStream(egmFileNameLocalCopy)
                    copyFile(`in`, out)
                    `in`.close()
                    `in` = null
                    out.flush()
                    out.close()
                    out = null
                    Timber.d("EGM File copy completed")
                    deleteFile(egmFilename) // Delete the EGM file from the shared folder
                    Timber.d("EGM File deleted from shared folder")
                } catch (e: Exception) {
                    Timber.d("Unable to make local copy of EGM file: ${e.message}")
                }
            }
            isEgmFileCopying = false
        }
    }
}
