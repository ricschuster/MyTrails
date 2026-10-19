package ca.mytrails.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

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

    @Query("UPDATE track SET state = :state, endTimeMillis = :endTimeMillis WHERE id = :id")
    suspend fun finish(
        id: Long,
        state: String,
        endTimeMillis: Long,
    )

    @Query("SELECT COUNT(*) FROM point WHERE trackId = :id")
    suspend fun pointCount(id: Long): Int

    @Query("DELETE FROM track WHERE id = :id")
    suspend fun deleteTrack(id: Long)

    /** Tracks left open by a kill or reboot: keep their points, mark them interrupted. */
    @Query("UPDATE track SET state = 'interrupted' WHERE state IN ('recording', 'paused')")
    suspend fun markOpenTracksInterrupted()
}

@Database(entities = [TrackEntity::class, PointEntity::class], version = 1)
abstract class TrackDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile private var instance: TrackDatabase? = null

        fun get(context: Context): TrackDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, TrackDatabase::class.java, "tracks.db")
                    .build()
                    .also { instance = it }
            }
    }
}
