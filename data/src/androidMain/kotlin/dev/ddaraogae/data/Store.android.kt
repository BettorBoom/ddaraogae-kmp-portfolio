package dev.ddaraogae.data

import android.content.Context
import androidx.room.Room
import dev.ddaraogae.domain.WalkSessionStore

fun openAndroidWalkStore(context: Context): WalkSessionStore = openStore(
    Room.databaseBuilder<WalkDatabase>(context.applicationContext, "walks.db")
)
