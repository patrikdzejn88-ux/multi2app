package com.multi2.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {
    @Query("SELECT * FROM spaces ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<SpaceEntity>>

    @Query("SELECT COUNT(*) FROM spaces")
    suspend fun count(): Int

    @Query("SELECT * FROM spaces WHERE id = :id")
    suspend fun byId(id: Int): SpaceEntity?

    @Query("SELECT * FROM spaces WHERE isPrivate = 1 LIMIT 1")
    suspend fun privateSpace(): SpaceEntity?

    @Query("SELECT * FROM spaces WHERE isPrivate = 0 ORDER BY sortOrder, id")
    fun observePublic(): Flow<List<SpaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(space: SpaceEntity): Long

    @Update
    suspend fun update(space: SpaceEntity)

    @Delete
    suspend fun delete(space: SpaceEntity)

    @Query("DELETE FROM cloned_apps WHERE spaceId = :spaceId")
    suspend fun deleteAppsOfSpace(spaceId: Int)
}

@Dao
interface ClonedAppDao {
    @Query("SELECT * FROM cloned_apps WHERE spaceId = :spaceId ORDER BY pinned DESC, label")
    fun observeOfSpace(spaceId: Int): Flow<List<ClonedAppEntity>>

    @Query("SELECT * FROM cloned_apps WHERE spaceId = :spaceId ORDER BY pinned DESC, label")
    suspend fun getOfSpace(spaceId: Int): List<ClonedAppEntity>

    @Query("SELECT * FROM cloned_apps WHERE pkg = :pkg AND spaceId = :spaceId")
    fun observeOne(pkg: String, spaceId: Int): Flow<ClonedAppEntity?>

    @Query("SELECT * FROM cloned_apps WHERE pkg = :pkg AND spaceId = :spaceId")
    suspend fun getOne(pkg: String, spaceId: Int): ClonedAppEntity?

    @Query("SELECT * FROM cloned_apps WHERE slot >= 0")
    suspend fun getRunning(): List<ClonedAppEntity>

    @Query("SELECT COUNT(*) FROM cloned_apps WHERE spaceId = :spaceId")
    fun observeCountOfSpace(spaceId: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: ClonedAppEntity)

    @Update
    suspend fun update(app: ClonedAppEntity)

    @Query("DELETE FROM cloned_apps WHERE pkg = :pkg AND spaceId = :spaceId")
    suspend fun delete(pkg: String, spaceId: Int)
}

@Dao
interface ProxyDao {
    @Query("SELECT * FROM proxies ORDER BY id")
    fun observeAll(): Flow<List<ProxyEntity>>

    @Query("SELECT * FROM proxies WHERE id = :id")
    suspend fun byId(id: Long): ProxyEntity?

    @Query("SELECT * FROM proxies WHERE (spaceId = :spaceId OR spaceId IS NULL) AND enabled = 1 ORDER BY spaceId DESC LIMIT 1")
    suspend fun activeForSpace(spaceId: Int): ProxyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(proxy: ProxyEntity): Long

    @Delete
    suspend fun delete(proxy: ProxyEntity)
}
