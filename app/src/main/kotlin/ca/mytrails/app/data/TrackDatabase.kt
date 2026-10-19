package ca.mytrails.app.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.migration.Migration
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** Track lifecycle states, stored as strings so the schema stays readable. */
object TrackState {
    const val RECORDING = "recording"
    const val PAUSED = "paused"
    const val COMPLETED = "completed"
    const val INTERRUPTED = "interrupted"
}

@Entity(tableName = "track")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val state: String,
    @ColumnInfo(defaultValue = "0") val distanceMeters: Double = 0.0,
    /** Time spent recording, excluding pauses. */
    @ColumnInfo(defaultValue = "0") val durationMillis: Long = 0,
)

@Entity(
    tableName = "point",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("trackId")],
)
data class PointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val segment: Int,
    val latitude: Double,
    val longitude: Double,
    val timeMillis: Long,
    val altitude: Double?,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float?,
)

@Dao
interface TrackDao {
    @Insert
    suspend fun insertTrack(track: TrackEntity): Long

    @Insert
    suspend fun insertPoint(point: PointEntity)

    @Query("UPDATE track SET state = :state WHERE id = :id")
    suspend fun setState(
        id: Long,
        state: String,
    )

    @Query(
        "UPDATE track SET state = :state, endTimeMillis = :endTimeMillis, " +
            "distanceMeters = :distanceMeters, durationMillis = :durationMillis WHERE id = :id",
    )
    suspend fun finish(
        id: Long,
        state: String,
        endTimeMillis: Long,
        distanceMeters: Double,
        durationMillis: Long,
    )

    @Query("UPDATE track SET distanceMeters = :distanceMeters, durationMillis = :durationMillis WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        distanceMeters: Double,
        durationMillis: Long,
    )

    @Query("UPDATE track SET name = :name WHERE id = :id")
    suspend fun rename(
        id: Long,
        name: String,
    )

    @Query("SELECT * FROM track ORDER BY startTimeMillis DESC")
    fun observeTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM track WHERE id = :id")
    suspend fun getTrack(id: Long): TrackEntity?

    @Query("SELECT * FROM point WHERE trackId = :id ORDER BY id")
    suspend fun getPoints(id: Long): List<PointEntity>

    @Query("SELECT COUNT(*) FROM point WHERE trackId = :id")
    suspend fun pointCount(id: Long): Int

    @Query("DELETE FROM track WHERE id = :id")
    suspend fun deleteTrack(id: Long)

    /** Tracks left open by a kill or reboot: keep their points, mark them interrupted. */
    @Query(
        "UPDATE track SET state = 'interrupted', endTimeMillis = " +
            "(SELECT MAX(timeMillis) FROM point WHERE trackId = track.id) " +
            "WHERE state IN ('recording', 'paused')",
    )
    suspend fun markOpenTracksInterrupted()
}

@Database(entities = [TrackEntity::class, PointEntity::class], version = 2)
abstract class TrackDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE track ADD COLUMN distanceMeters REAL NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE track ADD COLUMN durationMillis INTEGER NOT NULL DEFAULT 0")
                }
            }

        @Volatile private var instance: TrackDatabase? = null

        fun get(context: Context): TrackDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, TrackDatabase::class.java, "tracks.db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
