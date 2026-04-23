package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "mating_records",
    foreignKeys = {
        @ForeignKey(entity = Rabbit.class, parentColumns = "id", childColumns = "doeId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Rabbit.class, parentColumns = "id", childColumns = "buckId", onDelete = ForeignKey.SET_NULL)
    },
    indices = {
        @Index(value = "doeId"),
        @Index(value = "buckId"),
        @Index(value = "matingDate")
    }
)
public class MatingRecord {
    @androidx.room.PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public long doeId; // female rabbit id
    @NonNull
    public long buckId; // male rabbit id (cemental)
    @NonNull
    public long matingDate;
    public boolean isEffective = false; // successful mating
    public String result; // MatingResult enum value
    public String observations;
    public int attemptNumber = 1;
    @NonNull
    public long createdAt = System.currentTimeMillis();
}
