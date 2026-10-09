package com.multi2.app.di

import android.content.Context
import androidx.room.Room
import com.multi2.app.data.db.ClonedAppDao
import com.multi2.app.data.db.Multi2Database
import com.multi2.app.data.db.ProxyDao
import com.multi2.app.data.db.SpaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): Multi2Database =
        Room.databaseBuilder(context, Multi2Database::class.java, Multi2Database.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSpaceDao(db: Multi2Database): SpaceDao = db.spaceDao()
    @Provides fun provideClonedAppDao(db: Multi2Database): ClonedAppDao = db.clonedAppDao()
    @Provides fun provideProxyDao(db: Multi2Database): ProxyDao = db.proxyDao()
}
