package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "health_events",
    foreignKeys = {
        @ForeignKey(entity = Rabbit.class, parentColumns = "id", childColumns = "rabbitId", onDelete = ForeignKey.CASCADE)
    },
    indices = {
        @Index(value = "rabbitId"),
        @Index(value = "eventDate")
    }
)
public class HealthEvent {
    @androidx.room.PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public long rabbitId;
    @NonNull
    public String eventType; // HealthEventType enum value
    public String title;
    public String description;
    public String veterinarian;
    public String medication;
    public double cost;
    public String currency = "USD";
    @NonNull
    public long eventDate;
    @NonNull
    public long nextDueDate = 0; // for recurring events like vaccinations, 0 = no next due
    public String notes;
    @NonNull
    public long createdAt = System.currentTimeMillis();
}
