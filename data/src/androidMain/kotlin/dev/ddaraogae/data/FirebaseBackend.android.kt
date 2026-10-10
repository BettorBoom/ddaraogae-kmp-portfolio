package dev.ddaraogae.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

fun createFirebaseBackend(context: Context, appName: String? = null): NativeBackend? {
    val app = if (appName == null) FirebaseApp.initializeApp(context.applicationContext)
        else FirebaseApp.getInstance(appName)
    return app?.let { FirebaseBackend(FirebaseAuth.getInstance(it)) }
}

private class FirebaseBackend(private val auth: FirebaseAuth) : NativeBackend {
    override fun observeUser(observer: BackendUserObserver): CancelHandle {
        val lock = Any()
        var closed = false
        val listener = FirebaseAuth.AuthStateListener {
            synchronized(lock) { if (!closed) observer.onUser(it.currentUser?.uid) }
        }
        auth.addAuthStateListener(listener)
        return object : CancelHandle {
            override fun cancel() {
                val remove = synchronized(lock) {
                    if (closed) false else { closed = true; true }
                }
                // Removal alone does not stop an already queued callback. ADR-007.
                if (remove) auth.removeAuthStateListener(listener)
            }
        }
    }
}
