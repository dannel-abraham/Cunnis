package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "weight_records",
    foreignKeys = {
        @ForeignKey(entity = Rabbit.class, parentColumns = "id", childColumns = "rabbitId", onDelete = ForeignKey.CASCADE)
    },
    indices = {
        @Index(value = "rabbitId"),
        @Index(value = "recordDate")
    }
)
public class WeightRecord {
    @androidx.room.PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public long rabbitId;
    @NonNull
    public double weight; // in pounds
    @NonNull
    public long recordDate;
    public String notes;
    @NonNull
    public long createdAt = System.currentTimeMillis();
}
