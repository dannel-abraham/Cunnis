package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.FeedingRecord;
import java.util.List;

@Dao
public interface FeedingRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FeedingRecord record);

    @Update
    void update(FeedingRecord record);

    @Delete
    void delete(FeedingRecord record);

    @Query("SELECT * FROM feeding_records WHERE cageId = :cageId ORDER BY feedingDate DESC")
    LiveData<List<FeedingRecord>> getFeedingByCage(int cageId);

    @Query("SELECT * FROM feeding_records WHERE cageId = :cageId ORDER BY feedingDate DESC")
    List<FeedingRecord> getFeedingByCageSync(int cageId);

    @Query("SELECT * FROM feeding_records ORDER BY feedingDate DESC")
    LiveData<List<FeedingRecord>> getAllFeedingRecords();

    @Query("SELECT * FROM feeding_records ORDER BY feedingDate DESC")
    List<FeedingRecord> getAllFeedingRecordsSync();

    @Query("SELECT * FROM feeding_records WHERE cageId = :cageId AND feedingDate BETWEEN :startDate AND :endDate ORDER BY feedingDate DESC")
    List<FeedingRecord> getFeedingByCageAndDateRange(int cageId, long startDate, long endDate);
}
