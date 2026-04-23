package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.WeightRecord;
import java.util.List;

@Dao
public interface WeightRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(WeightRecord record);

    @Update
    void update(WeightRecord record);

    @Delete
    void delete(WeightRecord record);

    @Query("DELETE FROM weight_records WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM weight_records WHERE rabbitId = :rabbitId ORDER BY recordDate DESC")
    LiveData<List<WeightRecord>> getWeightRecordsByRabbit(long rabbitId);

    @Query("SELECT * FROM weight_records WHERE rabbitId = :rabbitId ORDER BY recordDate DESC")
    List<WeightRecord> getWeightRecordsByRabbitSync(long rabbitId);

    @Query("SELECT * FROM weight_records WHERE rabbitId = :rabbitId ORDER BY recordDate ASC")
    List<WeightRecord> getWeightRecordsByRabbitChronological(long rabbitId);

    @Query("SELECT * FROM weight_records WHERE rabbitId = :rabbitId ORDER BY recordDate DESC LIMIT 1")
    WeightRecord getLatestWeight(long rabbitId);

    @Query("SELECT AVG(weight) FROM weight_records WHERE rabbitId = :rabbitId")
    double getAverageWeight(long rabbitId);

    @Query("SELECT MAX(weight) FROM weight_records WHERE rabbitId = :rabbitId")
    double getMaxWeight(long rabbitId);
}
