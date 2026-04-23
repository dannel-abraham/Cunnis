package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.annotation.NonNull;

@Entity(tableName = "breeds")
public class Breed {
    @androidx.room.PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String name = "";
    public long lastUsed = System.currentTimeMillis();
    public int usageCount = 1;

    public Breed() {}

    @Ignore
    public Breed(@NonNull String name) {
        this.name = name;
        this.lastUsed = System.currentTimeMillis();
        this.usageCount = 1;
    }
}
