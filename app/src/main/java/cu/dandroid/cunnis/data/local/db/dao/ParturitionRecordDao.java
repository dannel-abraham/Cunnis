package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.ParturitionRecord;
import java.util.List;

@Dao
public interface ParturitionRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ParturitionRecord record);

    @Update
    void update(ParturitionRecord record);

    @Delete
    void delete(ParturitionRecord record);

    @Query("SELECT * FROM parturition_records WHERE doeId = :doeId ORDER BY parturitionDate DESC")
    LiveData<List<ParturitionRecord>> getParturitionsByDoe(long doeId);

    @Query("SELECT * FROM parturition_records WHERE doeId = :doeId ORDER BY parturitionDate DESC")
    List<ParturitionRecord> getParturitionsByDoeSync(long doeId);

    @Query("SELECT * FROM parturition_records WHERE buckId = :buckId ORDER BY parturitionDate DESC")
    LiveData<List<ParturitionRecord>> getParturitionsByBuck(long buckId);

    @Query("SELECT * FROM parturition_records WHERE buckId = :buckId ORDER BY parturitionDate DESC")
    List<ParturitionRecord> getParturitionsByBuckSync(long buckId);

    @Query("SELECT * FROM parturition_records ORDER BY parturitionDate DESC")
    LiveData<List<ParturitionRecord>> getAllParturitions();

    @Query("SELECT * FROM parturition_records ORDER BY parturitionDate DESC")
    List<ParturitionRecord> getAllParturitionsSync();

    @Query("SELECT * FROM parturition_records WHERE matingRecordId = :matingId LIMIT 1")
    ParturitionRecord getByMatingRecordId(long matingId);

    @Query("SELECT SUM(bornAlive) FROM parturition_records")
    int getTotalBornAlive();

    @Query("SELECT SUM(bornDead) FROM parturition_records")
    int getTotalBornDead();

    @Query("SELECT COUNT(*) FROM parturition_records")
    int getTotalParturitions();
}
