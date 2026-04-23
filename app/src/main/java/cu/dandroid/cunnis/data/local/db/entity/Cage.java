package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "cages")
public class Cage {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @NonNull
    public int cageNumber;
    public String notes;
    public long createdAt;
    public long updatedAt;

    public Cage() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
