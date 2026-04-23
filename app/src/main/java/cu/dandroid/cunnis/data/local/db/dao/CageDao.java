package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import java.util.List;

@Dao
public interface CageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Cage cage);

    @Update
    void update(Cage cage);

    @Delete
    void delete(Cage cage);

    @Query("DELETE FROM cages WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM cages ORDER BY cageNumber ASC")
    LiveData<List<Cage>> getAllCages();

    @Query("SELECT * FROM cages ORDER BY cageNumber ASC")
    List<Cage> getAllCagesSync();

    @Query("SELECT * FROM cages WHERE id = :id")
    LiveData<Cage> getCageById(int id);

    @Query("SELECT * FROM cages WHERE cageNumber = :number")
    LiveData<Cage> getCageByNumber(int number);

    @Query("SELECT COUNT(*) FROM cages")
    LiveData<Integer> getCageCount();

    @Query("SELECT * FROM cages WHERE id IN (:ids)")
    List<Cage> getCagesByIds(List<Integer> ids);
}
