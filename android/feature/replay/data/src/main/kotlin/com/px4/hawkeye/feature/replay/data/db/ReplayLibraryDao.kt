package com.px4.hawkeye.feature.replay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplayLibraryDao {

    @Query("SELECT * FROM library_entries ORDER BY imported_at_millis DESC")
    fun observeAll(): Flow<List<LibraryEntryEntity>>

    @Query("SELECT * FROM library_entries WHERE id = :id")
    suspend fun getById(id: String): LibraryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LibraryEntryEntity)

    @Query("DELETE FROM library_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM library_entries WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
