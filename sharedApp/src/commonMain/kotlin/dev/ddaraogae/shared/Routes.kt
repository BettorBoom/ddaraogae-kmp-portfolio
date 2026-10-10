package dev.ddaraogae.shared

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
internal data object Home : NavKey
@Serializable
internal data object Walk : NavKey
@Serializable
internal data object Dog : NavKey
@Serializable
internal data object History : NavKey
@Serializable
internal data class Record(val walkId: String) : NavKey

// Native platforms need explicit registration, since reflection is unavailable.
// Route restoration carries an ID, not a stale DB entity.
internal val routeConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Home::class, Home.serializer())
            subclass(Walk::class, Walk.serializer())
            subclass(Dog::class, Dog.serializer())
            subclass(History::class, History.serializer())
            subclass(Record::class, Record.serializer())
        }
    }
}
