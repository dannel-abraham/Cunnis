package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.EstrusRecord;
import java.util.List;

@Dao
public interface EstrusRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(EstrusRecord record);

    @Update
    void update(EstrusRecord record);

    @Delete
    void delete(EstrusRecord record);

    @Query("SELECT * FROM estrus_records WHERE rabbitId = :rabbitId ORDER BY estrusDate DESC")
    LiveData<List<EstrusRecord>> getEstrusRecordsByRabbit(long rabbitId);

    @Query("SELECT * FROM estrus_records WHERE rabbitId = :rabbitId ORDER BY estrusDate DESC")
    List<EstrusRecord> getEstrusRecordsByRabbitSync(long rabbitId);

    @Query("SELECT * FROM estrus_records WHERE rabbitId = :rabbitId ORDER BY estrusDate DESC LIMIT 1")
    EstrusRecord getLatestEstrus(long rabbitId);
}
