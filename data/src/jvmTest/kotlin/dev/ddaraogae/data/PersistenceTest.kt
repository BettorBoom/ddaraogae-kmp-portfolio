package dev.ddaraogae.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.ddaraogae.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.*

class PersistenceTest {
    @Test fun dogSnapshotHistoryAndRouteSurviveReopen() = runTest {
        val file = Files.createTempFile("ddaraogae-route", ".db")
        try {
            var store = openJvmWalkStore(file.toString())
            store.saveDog(DogProfile("a", "보리"))
            store.start(WalkSession("first", "a", 1_000, WalkStatus.Recording,
                dogName = "보리", lastResumedAtMillis = 1_000))
            store.appendPoint("a", "first", 0, LocationSample(37.0, 127.0, 900, 5.0))
            assertTrue(store.observePoints("a", "first").first().isEmpty())
            store.appendPoint("a", "first", 0, LocationSample(37.0, 127.0, 2_000, 5.0))
            store.appendPoint("a", "first", 0, LocationSample(37.0001, 127.0, 4_000, 5.0))
            store.appendPoint("a", "first", 0, LocationSample(38.0, 127.0, 5_000, 5.0))
            assertEquals(2, store.observePoints("a", "first").first().size)
            store.pause("a", "first", 5_000)
            store.resume("a", "first", 10_000)
            store.appendPoint("a", "first", 0, LocationSample(37.1, 127.0, 11_000, 5.0))
            store.appendPoint("a", "first", 1, LocationSample(37.1, 127.0, 9_000, 5.0))
            assertEquals(2, store.observePoints("a", "first").first().size)
            store.appendPoint("a", "first", 1, LocationSample(37.1, 127.0, 11_000, 5.0))
            store.finish("a", "first", 12_000)
            store.finish("a", "first", 12_001)
            store.saveDog(DogProfile("a", "보리 새 이름"))
            store.start(WalkSession("second", "a", 20_000, WalkStatus.Recording,
                dogName = "보리 새 이름", lastResumedAtMillis = 20_000))
            store.close()
            store = openJvmWalkStore(file.toString())
            assertEquals(listOf("second", "first"), store.observeHistory("a").first().map { it.id })
            val first = store.observeRecord("a", "first").first()!!
            assertEquals("보리", first.dogName)
            assertEquals(6_000L, first.activeDurationMillis)
            assertTrue(first.distanceMeters in 10.0..12.0)
            assertEquals(listOf(0, 0, 1), store.observePoints("a", "first").first().map { it.segment })
            assertNull(store.observeRecord("b", "first").first())
            assertTrue(store.observePoints("b", "first").first().isEmpty())
            store.close()
        } finally { Files.deleteIfExists(file) }
    }

    @Test fun migratesVersionTwoWithoutDeletingWalk() = runTest {
        val file = Files.createTempFile("ddaraogae-v2", ".db")
        try {
            BundledSQLiteDriver().open(file.toString()).use { connection ->
                listOf(
                    "CREATE TABLE walks (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, startedAtMillis INTEGER NOT NULL, status TEXT NOT NULL, activeToken INTEGER, endedAtMillis INTEGER)",
                    "CREATE UNIQUE INDEX index_walks_userId_activeToken ON walks(userId, activeToken)",
                    "INSERT INTO walks VALUES ('kept', 'a', 100, 'Recording', 1, NULL)",
                    "PRAGMA user_version = 2"
                ).forEach { sql -> connection.prepare(sql).use { it.step() } }
            }
            val store = openJvmWalkStore(file.toString())
            assertEquals(WalkStatus.Interrupted, store.observeRecord("a", "kept").first()?.status)
            assertNull(store.observeRecord("a", "kept").first()?.dogName)
            store.finish("a", "kept", 200)
            assertEquals(WalkStatus.Completed, store.observeRecord("a", "kept").first()?.status)
            store.close()
        } finally { Files.deleteIfExists(file) }
    }

    @Test fun versionThreeMigrationUsesLastCheckpointAsConservativeLocationBoundary() = runTest {
        val file = Files.createTempFile("ddaraogae-v3", ".db")
        try {
            BundledSQLiteDriver().open(file.toString()).use { connection ->
                listOf(
                    "CREATE TABLE dogs (userId TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)",
                    "CREATE TABLE walks (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, startedAtMillis INTEGER NOT NULL, status TEXT NOT NULL, activeToken INTEGER, endedAtMillis INTEGER, dogName TEXT, distanceMeters REAL NOT NULL DEFAULT 0.0, activeDurationMillis INTEGER NOT NULL DEFAULT 0, lastResumedAtMillis INTEGER, segment INTEGER NOT NULL DEFAULT 0)",
                    "CREATE UNIQUE INDEX index_walks_userId_activeToken ON walks(userId, activeToken)",
                    "CREATE TABLE walk_points (walkId TEXT NOT NULL, sequence INTEGER NOT NULL, userId TEXT NOT NULL, segment INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, recordedAtMillis INTEGER NOT NULL, accuracyMeters REAL NOT NULL, PRIMARY KEY(walkId, sequence))",
                    "CREATE INDEX index_walk_points_userId_walkId ON walk_points(userId, walkId)",
                    "INSERT INTO walks VALUES ('kept', 'a', 100, 'Recording', 1, NULL, NULL, 0.0, 20, 120, 0)",
                    "PRAGMA user_version = 3"
                ).forEach { sql -> connection.prepare(sql).use { it.step() } }
            }
            val store = openJvmWalkStore(file.toString())
            store.appendPoint("a", "kept", 0, LocationSample(37.0, 127.0, 110, 5.0))
            assertTrue(store.observePoints("a", "kept").first().isEmpty())
            store.appendPoint("a", "kept", 0, LocationSample(37.0, 127.0, 130, 5.0))
            assertEquals(1, store.observePoints("a", "kept").first().size)
            assertEquals(120, store.observeRecord("a", "kept").first()?.segmentStartedAtMillis)
            store.close()
        } finally { Files.deleteIfExists(file) }
    }

    @Test fun preservesRecordsAndEnforcesUserScope() = runTest {
        val file = Files.createTempFile("ddaraogae", ".db")
        try {
            var store = openJvmWalkStore(file.toString())
            store.start(WalkSession("walk-a", "user-a", 100, WalkStatus.Recording))
            assertFailsWith<IllegalStateException> {
                store.start(WalkSession("walk-b", "user-a", 101, WalkStatus.Recording))
            }
            assertNull(store.observe("user-b").first())
            assertFailsWith<IllegalStateException> { store.finish("user-b", "walk-a", 200) }
            store.close()
            store = openJvmWalkStore(file.toString())
            assertEquals("walk-a", store.observe("user-a").first()!!.id)
            store.finish("user-a", "walk-a", 200)
            assertEquals(WalkStatus.Completed, store.observe("user-a").first()!!.status)
            store.start(WalkSession("walk-next", "user-a", 300, WalkStatus.Recording))
            store.close()
        } finally { Files.deleteIfExists(file) }
    }

    @Test fun migratesVersionOneWithoutDeletingWalk() = runTest {
        val file = Files.createTempFile("ddaraogae-v1", ".db")
        try {
            BundledSQLiteDriver().open(file.toString()).use { connection ->
                listOf(
                    "CREATE TABLE walks (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, startedAtMillis INTEGER NOT NULL, status TEXT NOT NULL, activeToken INTEGER)",
                    "CREATE UNIQUE INDEX index_walks_userId_activeToken ON walks(userId, activeToken)",
                    "INSERT INTO walks VALUES ('kept', 'user-a', 100, 'Recording', 1)",
                    "PRAGMA user_version = 1"
                ).forEach { sql -> connection.prepare(sql).use { it.step() } }
            }
            val store = openJvmWalkStore(file.toString())
            assertEquals("kept", store.observe("user-a").first()!!.id)
            store.finish("user-a", "kept", 200)
            assertEquals(200L, store.observe("user-a").first()!!.endedAtMillis)
            store.close()
        } finally { Files.deleteIfExists(file) }
    }
}
