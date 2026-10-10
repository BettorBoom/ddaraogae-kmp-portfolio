package dev.ddaraogae.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.ddaraogae.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "dogs")
internal data class DogRow(@PrimaryKey val userId: String, val name: String) {
    fun toProfile() = DogProfile(userId, name)
}

@Entity(tableName = "walks", indices = [Index(value = ["userId", "activeToken"], unique = true)])
internal data class WalkRow(
    @PrimaryKey val id: String,
    val userId: String,
    val startedAtMillis: Long,
    val status: String,
    val activeToken: Int?,
    val endedAtMillis: Long? = null,
    val dogName: String? = null,
    @ColumnInfo(defaultValue = "0.0") val distanceMeters: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val activeDurationMillis: Long = 0,
    val lastResumedAtMillis: Long? = null,
    @ColumnInfo(defaultValue = "0") val segment: Int = 0,
    @ColumnInfo(defaultValue = "0") val segmentStartedAtMillis: Long = 0,
) {
    fun toSession() = WalkSession(id, userId, startedAtMillis, WalkStatus.valueOf(status),
        endedAtMillis, dogName, distanceMeters, activeDurationMillis, lastResumedAtMillis, segment,
        segmentStartedAtMillis)
}

@Entity(tableName = "walk_points", primaryKeys = ["walkId", "sequence"],
    indices = [Index(value = ["userId", "walkId"])])
internal data class PointRow(val walkId: String, val sequence: Int, val userId: String,
    val segment: Int, val latitude: Double, val longitude: Double,
    val recordedAtMillis: Long, val accuracyMeters: Double) {
    fun toPoint() = WalkPoint(sequence, segment,
        LocationSample(latitude, longitude, recordedAtMillis, accuracyMeters))
}

@Dao
internal interface WalkDao {
    @Query("SELECT * FROM walks WHERE userId = :userId ORDER BY activeToken DESC, startedAtMillis DESC, id DESC LIMIT 1")
    fun observeLatest(userId: String): Flow<WalkRow?>

    @Query("SELECT * FROM walks WHERE userId = :userId ORDER BY startedAtMillis DESC, id DESC")
    fun observeHistory(userId: String): Flow<List<WalkRow>>

    @Query("SELECT * FROM walks WHERE id = :walkId AND userId = :userId LIMIT 1")
    fun observeRecord(userId: String, walkId: String): Flow<WalkRow?>

    @Query("SELECT * FROM walks WHERE id = :walkId AND userId = :userId LIMIT 1")
    suspend fun record(userId: String, walkId: String): WalkRow?

    @Query("SELECT * FROM dogs WHERE userId = :userId LIMIT 1")
    fun observeDog(userId: String): Flow<DogRow?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDog(dog: DogRow)

    @Query("SELECT * FROM walk_points WHERE userId = :userId AND walkId = :walkId ORDER BY sequence ASC")
    fun observePoints(userId: String, walkId: String): Flow<List<PointRow>>

    @Query("SELECT * FROM walk_points WHERE userId = :userId AND walkId = :walkId ORDER BY sequence DESC LIMIT 1")
    suspend fun lastPoint(userId: String, walkId: String): PointRow?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWalk(row: WalkRow): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPoint(row: PointRow)

    @Query("UPDATE walks SET distanceMeters = distanceMeters + :additional WHERE id = :walkId AND userId = :userId AND status = 'Recording' AND segment = :segment")
    suspend fun addDistance(userId: String, walkId: String, segment: Int, additional: Double): Int

    @Query("UPDATE walks SET activeDurationMillis = activeDurationMillis + CASE WHEN :now > lastResumedAtMillis THEN :now - lastResumedAtMillis ELSE 0 END, lastResumedAtMillis = CASE WHEN :now > lastResumedAtMillis THEN :now ELSE lastResumedAtMillis END WHERE id = :walkId AND userId = :userId AND status = 'Recording'")
    suspend fun checkpoint(userId: String, walkId: String, now: Long): Int

    @Query("UPDATE walks SET activeDurationMillis = activeDurationMillis + CASE WHEN :now > lastResumedAtMillis THEN :now - lastResumedAtMillis ELSE 0 END, lastResumedAtMillis = NULL, status = 'Interrupted' WHERE id = :walkId AND userId = :userId AND status = 'Recording'")
    suspend fun pause(userId: String, walkId: String, now: Long): Int

    @Query("UPDATE walks SET lastResumedAtMillis = :now, segmentStartedAtMillis = :now, status = 'Recording', segment = segment + 1 WHERE id = :walkId AND userId = :userId AND status = 'Interrupted'")
    suspend fun resume(userId: String, walkId: String, now: Long): Int

    @Query("UPDATE walks SET status = 'Completed', activeToken = NULL, endedAtMillis = :endedAt, activeDurationMillis = activeDurationMillis + CASE WHEN status = 'Recording' AND :endedAt > lastResumedAtMillis THEN :endedAt - lastResumedAtMillis ELSE 0 END, lastResumedAtMillis = NULL WHERE id = :walkId AND userId = :userId AND status IN ('Recording', 'Interrupted') AND startedAtMillis <= :endedAt")
    suspend fun finish(userId: String, walkId: String, endedAt: Long): Int

    @Transaction
    suspend fun finishOnce(userId: String, walkId: String, endedAt: Long) {
        if (finish(userId, walkId, endedAt) == 1) return
        check(record(userId, walkId)?.status == WalkStatus.Completed.name) { "종료할 산책을 확인해 주세요." }
    }

    @Transaction
    suspend fun append(userId: String, walkId: String, segment: Int, sample: LocationSample) {
        val walk = record(userId, walkId) ?: return
        // A late callback from a previous segment must never extend a resumed route.
        if (walk.status != WalkStatus.Recording.name || walk.segment != segment || sample.accuracyMeters > 50.0 ||
            sample.recordedAtMillis < walk.segmentStartedAtMillis) return
        val previous = lastPoint(userId, walkId)
        if (previous != null && sample.recordedAtMillis <= previous.recordedAtMillis) return
        val added = if (previous?.segment == segment) distanceMeters(previous.toPoint().sample, sample) else 0.0
        if (previous?.segment == segment && added * 1000.0 / (sample.recordedAtMillis - previous.recordedAtMillis) > 12.0) return
        insertPoint(PointRow(walkId, (previous?.sequence ?: -1) + 1, userId,
            segment, sample.latitude, sample.longitude, sample.recordedAtMillis, sample.accuracyMeters))
        check(addDistance(userId, walkId, segment, added) == 1)
    }
}

@Database(entities = [DogRow::class, WalkRow::class, PointRow::class], version = 4)
@ConstructedBy(WalkDatabaseConstructor::class)
internal abstract class WalkDatabase : RoomDatabase() { abstract fun walks(): WalkDao }

@Suppress("KotlinNoActualForExpect")
internal expect object WalkDatabaseConstructor : RoomDatabaseConstructor<WalkDatabase> {
    override fun initialize(): WalkDatabase
}

internal val migration1To2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE walks ADD COLUMN endedAtMillis INTEGER").use { it.step() }
    }
}

internal val migration2To3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        listOf(
            "ALTER TABLE walks ADD COLUMN dogName TEXT",
            "ALTER TABLE walks ADD COLUMN distanceMeters REAL NOT NULL DEFAULT 0.0",
            "ALTER TABLE walks ADD COLUMN activeDurationMillis INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE walks ADD COLUMN lastResumedAtMillis INTEGER",
            "ALTER TABLE walks ADD COLUMN segment INTEGER NOT NULL DEFAULT 0",
            "CREATE TABLE IF NOT EXISTS dogs (userId TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS walk_points (walkId TEXT NOT NULL, sequence INTEGER NOT NULL, userId TEXT NOT NULL, segment INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, recordedAtMillis INTEGER NOT NULL, accuracyMeters REAL NOT NULL, PRIMARY KEY(walkId, sequence))",
            "CREATE INDEX IF NOT EXISTS index_walk_points_userId_walkId ON walk_points (userId, walkId)",
            // An active v2 walk has no persisted timer checkpoint. Restore it as interrupted.
            "UPDATE walks SET status = 'Interrupted' WHERE status = 'Recording'"
        ).forEach { sql -> connection.prepare(sql).use { it.step() } }
    }
}

internal val migration3To4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE walks ADD COLUMN segmentStartedAtMillis INTEGER NOT NULL DEFAULT 0").use { it.step() }
        // v3 did not retain the segment start after a timer checkpoint. Use
        // the latest known boundary; dropping an old callback is safer than
        // crediting movement from before the current segment.
        connection.prepare("UPDATE walks SET segmentStartedAtMillis = COALESCE(lastResumedAtMillis, startedAtMillis)").use { it.step() }
    }
}

internal fun openStore(builder: RoomDatabase.Builder<WalkDatabase>): WalkSessionStore =
    RoomWalkStore(builder.setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(migration1To2, migration2To3, migration3To4).build())

private class RoomWalkStore(private val database: WalkDatabase) : WalkSessionStore {
    private val dao = database.walks()
    override fun observe(userId: String) = dao.observeLatest(userId).map { it?.toSession() }
    override fun observeHistory(userId: String) = dao.observeHistory(userId).map { rows -> rows.map { it.toSession() } }
    override fun observeRecord(userId: String, walkId: String) = dao.observeRecord(userId, walkId).map { it?.toSession() }
    override fun observePoints(userId: String, walkId: String) = dao.observePoints(userId, walkId).map { rows -> rows.map { it.toPoint() } }
    override fun observeDog(userId: String) = dao.observeDog(userId).map { it?.toProfile() }
    override suspend fun saveDog(profile: DogProfile) = dao.saveDog(DogRow(profile.userId, profile.name))
    override suspend fun start(session: WalkSession) {
        require(session.status == WalkStatus.Recording && session.endedAtMillis == null)
        // SQLite permits multiple NULL values, but only one activeToken=1 per user.
        check(dao.insertWalk(WalkRow(session.id, session.userId, session.startedAtMillis,
            session.status.name, 1, dogName = session.dogName,
            lastResumedAtMillis = session.lastResumedAtMillis,
            segmentStartedAtMillis = session.startedAtMillis)) != -1L) { "이미 진행 중인 산책이 있어요." }
    }
    override suspend fun finish(userId: String, walkId: String, endedAtMillis: Long) =
        dao.finishOnce(userId, walkId, endedAtMillis)
    override suspend fun pause(userId: String, walkId: String, nowMillis: Long) { dao.pause(userId, walkId, nowMillis) }
    override suspend fun resume(userId: String, walkId: String, nowMillis: Long) {
        check(dao.resume(userId, walkId, nowMillis) == 1) { "이어갈 산책을 확인해 주세요." }
    }
    override suspend fun checkpoint(userId: String, walkId: String, nowMillis: Long) {
        dao.checkpoint(userId, walkId, nowMillis)
    }
    override suspend fun appendPoint(userId: String, walkId: String, segment: Int, sample: LocationSample) =
        dao.append(userId, walkId, segment, sample)
    override fun close() = database.close()
}
