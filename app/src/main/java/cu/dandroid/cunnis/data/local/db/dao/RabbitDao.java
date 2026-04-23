package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.model.Gender;
import java.util.List;

@Dao
public interface RabbitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Rabbit rabbit);

    @Update
    void update(Rabbit rabbit);

    @Delete
    void delete(Rabbit rabbit);

    @Query("DELETE FROM rabbits WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM rabbits ORDER BY createdAt DESC")
    LiveData<List<Rabbit>> getAllRabbits();

    @Query("SELECT * FROM rabbits ORDER BY createdAt DESC")
    List<Rabbit> getAllRabbitsSync();

    @Query("SELECT * FROM rabbits WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    LiveData<List<Rabbit>> getActiveRabbits();

    @Query("SELECT * FROM rabbits WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    List<Rabbit> getActiveRabbitsSync();

    @Query("SELECT * FROM rabbits WHERE id = :id")
    LiveData<Rabbit> getRabbitById(long id);

    @Query("SELECT * FROM rabbits WHERE id = :id")
    Rabbit getRabbitByIdSync(long id);

    @Query("SELECT * FROM rabbits WHERE identifier = :identifier LIMIT 1")
    Rabbit getRabbitByIdentifierSync(String identifier);

    @Query("SELECT * FROM rabbits WHERE cageId = :cageId AND status = 'ACTIVE' ORDER BY name ASC")
    LiveData<List<Rabbit>> getRabbitsByCage(int cageId);

    @Query("SELECT * FROM rabbits WHERE cageId = :cageId AND status = 'ACTIVE' ORDER BY name ASC")
    List<Rabbit> getRabbitsByCageSync(int cageId);

    @Query("SELECT * FROM rabbits WHERE isCemental = 1 LIMIT 1")
    LiveData<Rabbit> getCemental();

    @Query("SELECT * FROM rabbits WHERE isCemental = 1 LIMIT 1")
    Rabbit getCementalSync();

    @Query("SELECT * FROM rabbits WHERE isCemental = 1")
    List<Rabbit> getAllCementalsSync();

    @Query("SELECT * FROM rabbits WHERE gender = :gender AND status = 'ACTIVE' ORDER BY name ASC")
    LiveData<List<Rabbit>> getRabbitsByGender(Gender gender);

    @Query("SELECT * FROM rabbits WHERE gender = 'FEMALE' AND status = 'ACTIVE' ORDER BY name ASC")
    LiveData<List<Rabbit>> getActiveFemales();

    @Query("SELECT * FROM rabbits WHERE gender = 'FEMALE' AND status = 'ACTIVE' ORDER BY name ASC")
    List<Rabbit> getActiveFemalesSync();

    @Query("SELECT * FROM rabbits WHERE sireId = :sireId ORDER BY name ASC")
    LiveData<List<Rabbit>> getOffspringBySire(long sireId);

    @Query("SELECT * FROM rabbits WHERE sireId = :sireId ORDER BY name ASC")
    List<Rabbit> getOffspringBySireSync(long sireId);

    @Query("SELECT * FROM rabbits WHERE damId = :damId ORDER BY name ASC")
    LiveData<List<Rabbit>> getOffspringByDam(long damId);

    @Query("SELECT * FROM rabbits WHERE birthDate IS NOT NULL AND status = 'ACTIVE' ORDER BY birthDate ASC")
    LiveData<List<Rabbit>> getFarmBornRabbits();

    @Query("SELECT COUNT(*) FROM rabbits WHERE status = 'ACTIVE'")
    LiveData<Integer> getActiveRabbitCount();

    @Query("SELECT COUNT(*) FROM rabbits WHERE status = 'ACTIVE'")
    int getActiveRabbitCountSync();

    @Query("SELECT COUNT(*) FROM rabbits WHERE gender = 'MALE' AND status = 'ACTIVE'")
    LiveData<Integer> getActiveMaleCount();

    @Query("SELECT COUNT(*) FROM rabbits WHERE gender = 'FEMALE' AND status = 'ACTIVE'")
    LiveData<Integer> getActiveFemaleCount();

    @Query("SELECT * FROM rabbits WHERE status = 'ACTIVE' AND (name LIKE '%' || :query || '%' OR identifier LIKE '%' || :query || '%')")
    LiveData<List<Rabbit>> searchRabbits(String query);

    @Query("SELECT * FROM rabbits WHERE status = 'ACTIVE' AND (name LIKE '%' || :query || '%' OR identifier LIKE '%' || :query || '%')")
    List<Rabbit> searchRabbitsSync(String query);

    @Query("SELECT * FROM rabbits WHERE breed = :breed AND status = 'ACTIVE' ORDER BY name ASC")
    LiveData<List<Rabbit>> getRabbitsByBreed(String breed);

    @Query("SELECT MAX(CAST(identifier AS INTEGER)) FROM rabbits WHERE LENGTH(identifier) <= 6 AND identifier GLOB '[0-9]*'")
    Integer getMaxNumericIdentifier();

    @Query("UPDATE rabbits SET isCemental = 0 WHERE isCemental = 1")
    void clearCemental();
}
