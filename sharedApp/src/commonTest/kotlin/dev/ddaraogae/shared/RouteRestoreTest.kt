package dev.ddaraogae.shared

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.PolymorphicSerializer

class RouteRestoreTest {
    @Test fun restoresTypedBackStackWithWalkIdOnNativeToo() {
        val stack = NavBackStack<NavKey>(Home, Walk, Finish("walk-123"), Stamps, StampDetail(3), PetForm(editing = true))
        // Exercise the serializer used by rememberNavBackStack. A reified
        // List<NavKey> lookup silently relies on JVM reflection and fails on iOS.
        val serializer = NavBackStackSerializer(PolymorphicSerializer(NavKey::class))
        val saved = encodeToSavedState(serializer, stack, configuration = routeConfiguration)
        val restored = decodeFromSavedState(serializer, saved, configuration = routeConfiguration)
        assertEquals(stack.toList(), restored.toList())
    }
}
