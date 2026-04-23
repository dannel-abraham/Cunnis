package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.annotation.NonNull;

@Entity(tableName = "alert_configs")
public class AlertConfig {
    @androidx.room.PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String alertType; // AlertType enum value
    public boolean enabled = true;
    public int advanceDays = 1; // days before to alert
    public int advanceHours = 0;
    @NonNull
    public long createdAt = System.currentTimeMillis();
}
