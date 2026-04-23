package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
    tableName = "photo_records",
    foreignKeys = {
        @ForeignKey(entity = Rabbit.class, parentColumns = "id", childColumns = "rabbitId", onDelete = ForeignKey.CASCADE)
    },
    indices = {
        @Index(value = "rabbitId"),
        @Index(value = "photoDate")
    }
)
public class PhotoRecord {
    @androidx.room.PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public long rabbitId;
    @NonNull
    public byte[] photoData; // BLOB - compressed image
    public int photoWidth;
    public int photoHeight;
    @NonNull
    public long photoDate;
    public String notes;
    @NonNull
    public long createdAt = System.currentTimeMillis();
}
