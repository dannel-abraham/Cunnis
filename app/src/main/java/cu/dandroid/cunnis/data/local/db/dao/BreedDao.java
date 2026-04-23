package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.Breed;
import java.util.List;

@Dao
public interface BreedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Breed breed);

    @Query("SELECT DISTINCT name FROM breeds ORDER BY usageCount DESC, name ASC")
    LiveData<List<String>> getAllBreedNames();

    @Query("SELECT DISTINCT name FROM breeds ORDER BY usageCount DESC, name ASC")
    List<String> getAllBreedNamesSync();

    @Query("SELECT * FROM breeds WHERE name = :name LIMIT 1")
    Breed getBreedByName(String name);

    @Query("UPDATE breeds SET lastUsed = :timestamp, usageCount = usageCount + 1 WHERE name = :name")
    void incrementUsage(String name, long timestamp);

    @Query("SELECT name FROM breeds WHERE name LIKE '%' || :query || '%' ORDER BY usageCount DESC LIMIT 10")
    List<String> searchBreeds(String query);
}
