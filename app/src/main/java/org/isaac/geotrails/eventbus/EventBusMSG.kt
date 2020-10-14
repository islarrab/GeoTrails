package org.isaac.geotrails.eventbus

object EventBusMSG {
    const val APP_RESUME: Short = 1 // Sent to components on app resume
    const val APP_PAUSE: Short = 2 // Sent to components on app pause
    const val NEW_TRACK: Short = 3 // Request to create a new track
    const val UPDATE_FIX: Short = 4 // Notify that a new fix is available
    const val UPDATE_TRACK: Short = 5 // Notify that the current track stats are updated
    const val UPDATE_TRACKLIST: Short = 6 // Notify that the tracklist is changed
    const val UPDATE_SETTINGS: Short = 7 // Tell that settings are changed
    const val REQUEST_ADD_PLACEMARK: Short = 8 // The user ask to add a placemark
    const val ADD_PLACEMARK: Short = 9 // The placemark is available
    const val APPLY_SETTINGS: Short = 10 // The new settings must be applied
    const val TOAST_TRACK_EXPORTED: Short =
        11 // The exporter has finished to export the track, shows toast
    const val TOAST_STORAGE_PERMISSION_REQUIRED: Short = 12 // The Storage permission is required
    const val UPDATE_JOB_PROGRESS: Short = 13 // Update the progress of the current Job
    const val NOTIFY_TRACKS_DELETED: Short = 14 // Notify that some tracks are deleted
    const val UPDATE_ACTIONBAR: Short = 15 // Notify that the actionbar must be updated
    const val REFRESH_TRACKLIST: Short = 16 // Refresh the tracklist, without update it from DB
    const val TRACKLIST_DESELECT: Short =
        24 // The user deselect (into the tracklist) the track with a given id
    const val TRACKLIST_SELECT: Short =
        25 // The user select (into the tracklist) the track with a given id
    const val INTENT_SEND: Short = 26 // Request to share
    const val TOAST_UNABLE_TO_WRITE_THE_FILE: Short =
        27 // Exporter fails to export the Track (given id)
    const val ACTION_BULK_DELETE_TRACKS: Short = 40 // Delete the selected tracks
    const val ACTION_BULK_EXPORT_TRACKS: Short = 41 // Export the selected tracks
    const val ACTION_BULK_VIEW_TRACKS: Short = 42 // View the selected tracks
    const val ACTION_BULK_SHARE_TRACKS: Short = 43 // Share the selected tracks
}