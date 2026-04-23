package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.MatingRecord;
import java.util.List;

@Dao
public interface MatingRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MatingRecord record);

    @Update
    void update(MatingRecord record);

    @Delete
    void delete(MatingRecord record);

    @Query("SELECT * FROM mating_records WHERE doeId = :doeId ORDER BY matingDate DESC")
    LiveData<List<MatingRecord>> getMatingRecordsByDoe(long doeId);

    @Query("SELECT * FROM mating_records WHERE buckId = :buckId ORDER BY matingDate DESC")
    LiveData<List<MatingRecord>> getMatingRecordsByBuck(long buckId);

    @Query("SELECT * FROM mating_records WHERE doeId = :doeId ORDER BY matingDate DESC")
    List<MatingRecord> getMatingRecordsByDoeSync(long doeId);

    @Query("SELECT * FROM mating_records WHERE buckId = :buckId ORDER BY matingDate DESC")
    List<MatingRecord> getMatingRecordsByBuckSync(long buckId);

    @Query("SELECT * FROM mating_records ORDER BY matingDate DESC")
    LiveData<List<MatingRecord>> getAllMatingRecords();

    @Query("SELECT * FROM mating_records ORDER BY matingDate DESC")
    List<MatingRecord> getAllMatingRecordsSync();
}
