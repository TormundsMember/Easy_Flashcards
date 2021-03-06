package io.github.tormundsmember.easyflashcards.ui

import android.app.Application
import androidx.room.Room
import io.github.tormundsmember.easyflashcards.BuildConfig
import io.github.tormundsmember.easyflashcards.data.Database
import io.github.tormundsmember.easyflashcards.data.RoomDb
import io.github.tormundsmember.easyflashcards.data.UserData
import io.mockk.mockk

class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Dependencies.init(object : MainModule {
            private val roomDb: RoomDb by lazy {
                Room.databaseBuilder(applicationContext, RoomDb::class.java, BuildConfig.db_name_test)
                    .allowMainThreadQueries()
                    .build()

            }
            override val database: Database by lazy { roomDb.getDao() }
            override val userData: UserData by lazy { mockk<UserData>(relaxed=true) }
        })
    }

}