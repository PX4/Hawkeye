package com.px4.hawkeye.feature.replay.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LibraryEntryEntity::class], version = 1, exportSchema = true)
abstract class ReplayDatabase : RoomDatabase() {
    abstract fun libraryDao(): ReplayLibraryDao
}
