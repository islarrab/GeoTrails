package org.isaac.geotrails.util

data class ExportingTask(
    var id: Long = 0,
    var numberOfPoints_Total: Long = 0,
    var numberOfPoints_Processed: Long = 0,
    var status: Short = STATUS_PENDING,
    var name: String = ""
) {

    companion object {
        const val STATUS_PENDING: Short = 0 // Task not yet started
        const val STATUS_RUNNING: Short = 1 // Task is running
        const val STATUS_ENDED_SUCCESS: Short = 2 // Task ended with success
        const val STATUS_ENDED_FAILED: Short = 3 // Task failed to export
    }
}