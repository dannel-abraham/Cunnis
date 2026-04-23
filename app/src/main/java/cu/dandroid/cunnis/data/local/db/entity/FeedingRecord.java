package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "feeding_records",
    foreignKeys = {
        @ForeignKey(entity = Cage.class, parentColumns = "id", childColumns = "cageId", onDelete = ForeignKey.CASCADE)
    },
    indices = {
        @Index(value = "cageId"),
        @Index(value = "feedingDate")
    }
)
public class FeedingRecord {
    @androidx.room.PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public int cageId;
    public String feedType;
    public double quantity; // in pounds
    public String unit = "lb";
    public String notes;
    @NonNull
    public long feedingDate;
    @NonNull
    public long createdAt = System.currentTimeMillis();
}
