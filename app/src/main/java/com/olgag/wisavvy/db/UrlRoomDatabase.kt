package com.olgag.wisavvy.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.olgag.wisavvy.model.URLS

@Database(entities = [(URLS::class)], version = 1)
abstract class UrlRoomDatabase: RoomDatabase() {

    abstract fun urlDao(): UrlsDao

    companion object {

        private var INSTANCE: UrlRoomDatabase? = null

        fun getInstance(context: Context): UrlRoomDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        UrlRoomDatabase::class.java,
                        "product_database"
                    ).fallbackToDestructiveMigration()
                        .build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}