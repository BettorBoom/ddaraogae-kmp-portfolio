package dev.ddaraogae.shared

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

// Home, Walk and MyPage are the original bottom tabs; the rest are pushed screens without the bar.
@Serializable
internal data object Home : NavKey
@Serializable
internal data object Walk : NavKey
@Serializable
internal data object MyPage : NavKey
@Serializable
internal data object History : NavKey
@Serializable
internal data object Stamps : NavKey
@Serializable
internal data class StampDetail(val number: Int) : NavKey
@Serializable
internal data class Finish(val walkId: String) : NavKey
@Serializable
internal data object PetList : NavKey
@Serializable
internal data object PetDetail : NavKey
@Serializable
internal data class PetForm(val editing: Boolean) : NavKey
@Serializable
internal data object Alarm : NavKey

// Native platforms need explicit registration, since reflection is unavailable.
// Route restoration carries an ID, not a stale DB entity.
internal val routeConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Home::class, Home.serializer())
            subclass(Walk::class, Walk.serializer())
            subclass(MyPage::class, MyPage.serializer())
            subclass(History::class, History.serializer())
            subclass(Stamps::class, Stamps.serializer())
            subclass(StampDetail::class, StampDetail.serializer())
            subclass(Finish::class, Finish.serializer())
            subclass(PetList::class, PetList.serializer())
            subclass(PetDetail::class, PetDetail.serializer())
            subclass(PetForm::class, PetForm.serializer())
            subclass(Alarm::class, Alarm.serializer())
        }
    }
}
