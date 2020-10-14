package org.isaac.geotrails.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Type converters to allow Room to reference complex data types.
 */
class Converters {
    @TypeConverter
    fun zonedDateTimeToMillis(dateTime: ZonedDateTime): Long = dateTime.toInstant().toEpochMilli()

    @TypeConverter
    fun millisToZonedDateTime(value: Long): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault())
}