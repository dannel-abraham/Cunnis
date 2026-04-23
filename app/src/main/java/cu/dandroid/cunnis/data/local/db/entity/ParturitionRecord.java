package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "parturition_records",
    foreignKeys = {
        @ForeignKey(entity = Rabbit.class, parentColumns = "id", childColumns = "doeId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Rabbit.class, parentColumns = "id", childColumns = "buckId", onDelete = ForeignKey.SET_NULL),
        @ForeignKey(entity = MatingRecord.class, parentColumns = "id", childColumns = "matingRecordId", onDelete = ForeignKey.SET_NULL)
    },
    indices = {
        @Index(value = "doeId"),
        @Index(value = "buckId"),
        @Index(value = "parturitionDate")
    }
)
public class ParturitionRecord {
    @androidx.room.PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public long doeId;
    @NonNull
    public long buckId;
    public Long matingRecordId;
    @NonNull
    public long parturitionDate;
    public long estimatedDueDate; // calculated from mating
    public int totalBorn = 0;
    public int bornAlive = 0;
    public int bornDead = 0;
    public int weanedCount = 0;
    public boolean hadComplications = false;
    public String complicationsNotes;
    public String notes;
    @NonNull
    public long createdAt = System.currentTimeMillis();
}
