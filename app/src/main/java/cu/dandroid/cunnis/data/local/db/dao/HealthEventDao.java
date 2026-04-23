package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.HealthEvent;
import java.util.List;

@Dao
public interface HealthEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(HealthEvent event);

    @Update
    void update(HealthEvent event);

    @Delete
    void delete(HealthEvent event);

    @Query("DELETE FROM health_events WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM health_events WHERE rabbitId = :rabbitId ORDER BY eventDate DESC")
    LiveData<List<HealthEvent>> getHealthEventsByRabbit(long rabbitId);

    @Query("SELECT * FROM health_events WHERE rabbitId = :rabbitId ORDER BY eventDate DESC")
    List<HealthEvent> getHealthEventsByRabbitSync(long rabbitId);

    @Query("SELECT * FROM health_events WHERE nextDueDate > 0 AND nextDueDate <= :beforeDate ORDER BY nextDueDate ASC")
    List<HealthEvent> getUpcomingDueEvents(long beforeDate);
}
