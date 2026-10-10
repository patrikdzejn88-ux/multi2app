package com.multi2.app.data

import com.multi2.app.data.db.ClonedAppDao
import com.multi2.app.data.db.ClonedAppEntity
import com.multi2.app.data.db.SpaceDao
import com.multi2.app.data.db.SpaceEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpaceRepository @Inject constructor(
    private val spaceDao: SpaceDao,
    private val clonedAppDao: ClonedAppDao,
) {
    val spaces: Flow<List<SpaceEntity>> = spaceDao.observeAll()

    /** Home shows only public spaces; private space lives behind the lock. */
    val publicSpaces: Flow<List<SpaceEntity>> = spaceDao.observePublic()

    /** First-run: create "Virtual Space-1" like the original does. */
    suspend fun ensureDefaultSpace() {
        if (spaceDao.count() == 0) {
            spaceDao.insert(SpaceEntity(title = "Virtual Space-1", sortOrder = 0))
        }
    }

    suspend fun createSpace(title: String, sortOrder: Int): Long =
        spaceDao.insert(SpaceEntity(title = title, sortOrder = sortOrder))

    /** Private space is a singleton — returns its id, creating it on first use. */
    suspend fun ensurePrivateSpace(): Int {
        val existing = spaceDao.privateSpace()
        if (existing != null) return existing.id
        return spaceDao.insert(
            SpaceEntity(title = "Private Space", isPrivate = true, sortOrder = -1),
        ).toInt()
    }

    suspend fun renameSpace(spaceId: Int, newTitle: String) {
        spaceDao.byId(spaceId)?.let { spaceDao.update(it.copy(title = newTitle)) }
    }

    suspend fun deleteSpaceById(spaceId: Int) {
        spaceDao.byId(spaceId)?.let {
            spaceDao.deleteAppsOfSpace(it.id)
            spaceDao.delete(it)
        }
    }

    fun appsOfSpace(spaceId: Int): Flow<List<ClonedAppEntity>> =
        clonedAppDao.observeOfSpace(spaceId)

    suspend fun appsOfSpaceOnce(spaceId: Int): List<ClonedAppEntity> =
        clonedAppDao.getOfSpace(spaceId)

    fun app(pkg: String, spaceId: Int): Flow<ClonedAppEntity?> =
        clonedAppDao.observeOne(pkg, spaceId)

    suspend fun getAppOnce(pkg: String, spaceId: Int): ClonedAppEntity? =
        clonedAppDao.getOne(pkg, spaceId)

    suspend fun getRunningApps(): List<ClonedAppEntity> = clonedAppDao.getRunning()

    suspend fun assignSlot(pkg: String, spaceId: Int, slot: Int) {
        clonedAppDao.getOne(pkg, spaceId)?.let { clonedAppDao.update(it.copy(slot = slot)) }
    }

    suspend fun releaseSlot(pkg: String, spaceId: Int) {
        clonedAppDao.getOne(pkg, spaceId)?.let { clonedAppDao.update(it.copy(slot = -1)) }
    }

    fun appCountOfSpace(spaceId: Int): Flow<Int> =
        clonedAppDao.observeCountOfSpace(spaceId)

    /** Record a clone request; the engine performs the actual install in M2. */
    suspend fun enqueueClone(pkg: String, label: String, spaceIds: List<Int>) {
        spaceIds.forEach { id ->
            clonedAppDao.upsert(ClonedAppEntity(pkg = pkg, spaceId = id, label = label))
        }
    }

    suspend fun updateApp(app: ClonedAppEntity) = clonedAppDao.update(app)

    suspend fun removeApp(pkg: String, spaceId: Int) = clonedAppDao.delete(pkg, spaceId)
}
