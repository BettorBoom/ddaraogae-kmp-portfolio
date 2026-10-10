package dev.ddaraogae.data

import androidx.room.Room
import dev.ddaraogae.domain.WalkSessionStore

fun openJvmWalkStore(path: String): WalkSessionStore = openStore(
    Room.databaseBuilder<WalkDatabase>(name = path)
)
