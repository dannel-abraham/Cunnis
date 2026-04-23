package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.PhotoRecord;
import java.util.List;

@Dao
public interface PhotoRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PhotoRecord record);

    @Update
    void update(PhotoRecord record);

    @Delete
    void delete(PhotoRecord record);

    @Query("DELETE FROM photo_records WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM photo_records WHERE rabbitId = :rabbitId ORDER BY photoDate DESC")
    LiveData<List<PhotoRecord>> getPhotosByRabbit(long rabbitId);

    @Query("SELECT * FROM photo_records WHERE rabbitId = :rabbitId ORDER BY photoDate DESC LIMIT 1")
    PhotoRecord getLatestPhoto(long rabbitId);

    @Query("SELECT * FROM photo_records WHERE rabbitId = :rabbitId ORDER BY photoDate ASC")
    List<PhotoRecord> getPhotosChronological(long rabbitId);
}
