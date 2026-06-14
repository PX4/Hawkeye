package com.px4.hawkeye.feature.replay.data

import com.px4.hawkeye.feature.replay.data.db.LibraryEntryEntity
import com.px4.hawkeye.feature.replay.data.db.ReplayLibraryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [ReplayLibraryDao] that mirrors the real `ORDER BY imported_at_millis DESC`. */
class FakeReplayLibraryDao : ReplayLibraryDao {
    private val rows = MutableStateFlow<List<LibraryEntryEntity>>(emptyList())
    var insertShouldThrow: Throwable? = null

    fun seed(vararg entities: LibraryEntryEntity) { rows.value = entities.toList() }

    override fun observeAll(): Flow<List<LibraryEntryEntity>> =
        rows.map { list -> list.sortedByDescending { it.importedAtMillis } }

    override suspend fun getById(id: String): LibraryEntryEntity? = rows.value.find { it.id == id }

    override suspend fun insert(entity: LibraryEntryEntity) {
        insertShouldThrow?.let { throw it }
        rows.value = rows.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun deleteById(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteByIds(ids: List<String>) {
        rows.value = rows.value.filterNot { it.id in ids }
    }
}
