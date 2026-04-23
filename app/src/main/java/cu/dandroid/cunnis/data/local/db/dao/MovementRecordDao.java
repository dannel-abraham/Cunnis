package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.MovementRecord;
import java.util.List;

@Dao
public interface MovementRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MovementRecord record);

    @Update
    void update(MovementRecord record);

    @Delete
    void delete(MovementRecord record);

    @Query("SELECT * FROM movement_records WHERE rabbitId = :rabbitId ORDER BY movementDate DESC")
    LiveData<List<MovementRecord>> getMovementsByRabbit(long rabbitId);

    @Query("SELECT * FROM movement_records WHERE rabbitId = :rabbitId ORDER BY movementDate DESC")
    List<MovementRecord> getMovementsByRabbitSync(long rabbitId);
}
