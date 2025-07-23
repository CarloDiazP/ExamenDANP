package com.unsa.examendanp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.unsa.examendanp.data.local.database.dao.ContactDao
import com.unsa.examendanp.data.local.database.entities.ContactEntity
// TypeConverters for Date
import androidx.room.TypeConverter
import java.util.Date

@Database(
    entities = [ContactEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}