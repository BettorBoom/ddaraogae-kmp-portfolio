package dev.ddaraogae.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import dev.ddaraogae.data.BackendUserObserver
import dev.ddaraogae.data.createFirebaseBackend
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseSmokeTest {
    @Test fun officialSdkEnforcesUidAndReleasesAuthObserver() {
        // Cloud is opt-in and pinned to the new project. Never reuse the product Auth session.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cloudProject = InstrumentationRegistry.getArguments().getString("firebaseCloudProject")
        require(cloudProject == null || cloudProject == "ddaraogae-kmp")
        require(context.packageName == "dev.ddaraogae.app")
        val options = if (cloudProject != null) {
            requireNotNull(FirebaseOptions.fromResource(context)).also {
                require(it.projectId == cloudProject)
            }
        } else FirebaseOptions.Builder()
            .setProjectId("demo-ddaraogae-kmp")
            .setApplicationId("1:123456789:android:0123456789abcdef")
            .setApiKey("AIza" + "0".repeat(35)).build()
        val app = FirebaseApp.initializeApp(context, options, "uid-smoke")
        val auth = FirebaseAuth.getInstance(app).apply {
            if (cloudProject == null) useEmulator("10.0.2.2", 9099)
        }
        val db = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
            if (cloudProject == null) useEmulator("10.0.2.2", 8080)
        }
        fun signInTestUser(): String {
            // Synthetic credentials exist only in this test call; no real email or password artifact.
            val task = if (cloudProject == null) auth.signInAnonymously() else
                auth.createUserWithEmailAndPassword("m04-${UUID.randomUUID()}@example.invalid", UUID.randomUUID().toString())
            return requireNotNull(Tasks.await(task, 30, TimeUnit.SECONDS).user).uid
        }
        val states = LinkedBlockingQueue<String>()
        val calls = AtomicInteger()
        val backend = requireNotNull(createFirebaseBackend(context, app.name))
        val handle = backend.observeUser(object : BackendUserObserver {
            override fun onUser(userId: String?) { calls.incrementAndGet(); states.offer(userId ?: "signed-out") }
        })
        fun awaitState(uid: String) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
            while (System.nanoTime() < deadline) {
                if (states.poll(1, TimeUnit.SECONDS) == uid) return
            }
            fail("Auth observer did not publish expected state")
        }
        fun denied(action: () -> Unit) {
            try { action(); fail("Cross-UID operation unexpectedly succeeded") }
            catch (e: ExecutionException) {
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED,
                    (e.cause as FirebaseFirestoreException).code)
            }
        }
        try {
            auth.signOut()
            awaitState("signed-out")
            val alice = signInTestUser()
            awaitState(alice)
            val path = db.document("users/$alice")
            Tasks.await(path.set(mapOf("uid" to alice, "schemaVersion" to 1, "lifecycle" to "active")), 15, TimeUnit.SECONDS)
            assertEquals(alice, Tasks.await(path.get(Source.SERVER), 15, TimeUnit.SECONDS).getString("uid"))
            val firstCancel = Thread { handle.cancel() }
            val secondCancel = Thread { handle.cancel() }
            firstCancel.start(); secondCancel.start()
            firstCancel.join(5_000); secondCancel.join(5_000)
            assertFalse(firstCancel.isAlive); assertFalse(secondCancel.isAlive)
            handle.cancel()
            val stoppedAt = calls.get()
            // A second real listener is a callback barrier, not a fixed sleep.
            val barrier = backend.observeUser(object : BackendUserObserver {
                override fun onUser(userId: String?) { states.offer(userId ?: "signed-out") }
            })
            try {
                states.clear()
                auth.signOut(); awaitState("signed-out")
                denied { Tasks.await(path.get(Source.SERVER), 15, TimeUnit.SECONDS) }
                val bob = signInTestUser()
                assertNotEquals(alice, bob)
                awaitState(bob)
                denied { Tasks.await(path.get(Source.SERVER), 15, TimeUnit.SECONDS) }
                // Valid A document: rejection must come from B's ownership, not invalid fields.
                denied { Tasks.await(path.set(mapOf("uid" to alice, "schemaVersion" to 1, "lifecycle" to "active")), 15, TimeUnit.SECONDS) }
                assertEquals(stoppedAt, calls.get())
            } finally { barrier.cancel() }
        } finally {
            handle.cancel()
            auth.signOut()
            Tasks.await(db.terminate(), 15, TimeUnit.SECONDS)
            app.delete()
        }
    }
}
