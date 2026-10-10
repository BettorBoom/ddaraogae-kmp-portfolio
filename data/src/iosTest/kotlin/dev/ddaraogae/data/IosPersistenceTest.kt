package dev.ddaraogae.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.ddaraogae.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.*

class IosPersistenceTest {
    @Test fun nativeDatabasePreservesAndIsolatesUsers() = runTest {
        val path = NSTemporaryDirectory() + NSUUID().UUIDString + ".db"
        try {
            var store = openIosWalkStore(path)
            store.start(WalkSession("one", "a", 100, WalkStatus.Recording))
            assertNull(store.observe("b").first())
            assertFailsWith<IllegalStateException> { store.finish("b", "one", 200) }
            assertFailsWith<IllegalStateException> { store.start(WalkSession("racing", "a", 101, WalkStatus.Recording)) }
            store.close()
            store = openIosWalkStore(path)
            assertEquals("one", store.observe("a").first()?.id)
            store.finish("a", "one", 200)
            assertEquals(200L, store.observe("a").first()?.endedAtMillis)
            // Wall clocks can move backwards between walks; active still wins.
            store.start(WalkSession("next", "a", 50, WalkStatus.Recording))
            assertEquals("next", store.observe("a").first()?.id)
            store.close()
        } finally { removeDatabase(path) }
    }

    @Test fun nativeVersionOneMigrationKeepsRecord() = runTest {
        val path = NSTemporaryDirectory() + NSUUID().UUIDString + ".db"
        try {
            BundledSQLiteDriver().open(path).use { connection ->
                listOf(
                    "CREATE TABLE walks (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, startedAtMillis INTEGER NOT NULL, status TEXT NOT NULL, activeToken INTEGER)",
                    "CREATE UNIQUE INDEX index_walks_userId_activeToken ON walks(userId, activeToken)",
                    "INSERT INTO walks VALUES ('kept', 'a', 100, 'Recording', 1)",
                    "PRAGMA user_version = 1"
                ).forEach { sql -> connection.prepare(sql).use { it.step() } }
            }
            val store = openIosWalkStore(path)
            assertEquals("kept", store.observe("a").first()?.id)
            store.finish("a", "kept", 200)
            assertEquals(WalkStatus.Completed, store.observe("a").first()?.status)
            store.close()
        } finally { removeDatabase(path) }
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun removeDatabase(path: String) {
    listOf("", "-wal", "-shm").forEach { NSFileManager.defaultManager.removeItemAtPath(path + it, null) }
}
