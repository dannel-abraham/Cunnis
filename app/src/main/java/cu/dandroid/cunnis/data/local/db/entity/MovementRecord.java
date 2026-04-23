package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "movement_records",
    foreignKeys = {
        @ForeignKey(entity = Rabbit.class, parentColumns = "id", childColumns = "rabbitId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Cage.class, parentColumns = "id", childColumns = "fromCageId", onDelete = ForeignKey.SET_NULL),
        @ForeignKey(entity = Cage.class, parentColumns = "id", childColumns = "toCageId", onDelete = ForeignKey.SET_NULL)
    },
    indices = {
        @Index(value = "rabbitId"),
        @Index(value = "movementDate")
    }
)
public class MovementRecord {
    @androidx.room.PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public long rabbitId;
    public Integer fromCageId; // null if from cemental cage or outside
    public Integer toCageId; // null if going to cemental cage or outside
    public String reason; // e.g. "weaning", "breeding", "reorganization"
    public String notes;
    @NonNull
    public long movementDate;
    @NonNull
    public long createdAt = System.currentTimeMillis();
}
