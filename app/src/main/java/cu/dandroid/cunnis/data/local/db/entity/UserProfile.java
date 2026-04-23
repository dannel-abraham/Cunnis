package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.annotation.NonNull;

@Entity(tableName = "user_profile")
public class UserProfile {
    @androidx.room.PrimaryKey
    @NonNull
    public String id = "default";
    public String username;
    public String farmName;
    public String email;
    public String phone;
    public String address;
    public long createdAt;
    public long updatedAt;
}
