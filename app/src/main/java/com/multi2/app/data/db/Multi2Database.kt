package com.multi2.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SpaceEntity::class, ClonedAppEntity::class, ProxyEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class Multi2Database : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao
    abstract fun clonedAppDao(): ClonedAppDao
    abstract fun proxyDao(): ProxyDao

    companion object {
        const val NAME = "multi2.db"
    }
}
