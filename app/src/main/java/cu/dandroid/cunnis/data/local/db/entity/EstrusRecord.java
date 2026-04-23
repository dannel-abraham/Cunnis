package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "estrus_records",
    foreignKeys = {
        @ForeignKey(entity = Rabbit.class, parentColumns = "id", childColumns = "rabbitId", onDelete = ForeignKey.CASCADE)
    },
    indices = {
        @Index(value = "rabbitId"),
        @Index(value = "estrusDate")
    }
)
public class EstrusRecord {
    @androidx.room.PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public long rabbitId; // female rabbit id
    @NonNull
    public long estrusDate;
    public long endDate; // optional
    public String observations;
    public String vulvaColor; // color observation
    public boolean receptive;
    @NonNull
    public long createdAt = System.currentTimeMillis();
}
