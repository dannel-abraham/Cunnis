package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.AlertConfig;
import java.util.List;

@Dao
public interface AlertConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(AlertConfig config);

    @Update
    void update(AlertConfig config);

    @Query("SELECT * FROM alert_configs")
    LiveData<List<AlertConfig>> getAllConfigs();

    @Query("SELECT * FROM alert_configs")
    List<AlertConfig> getAllConfigsSync();

    @Query("SELECT * FROM alert_configs WHERE alertType = :type LIMIT 1")
    AlertConfig getConfigByType(String type);

    @Query("SELECT * FROM alert_configs WHERE enabled = 1")
    List<AlertConfig> getEnabledConfigs();
}
