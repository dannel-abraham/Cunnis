package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.UserProfile;

@Dao
public interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserProfile profile);

    @Update
    void update(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE id = 'default' LIMIT 1")
    LiveData<UserProfile> getProfile();

    @Query("SELECT * FROM user_profile WHERE id = 'default' LIMIT 1")
    UserProfile getProfileSync();

    @Query("SELECT EXISTS(SELECT 1 FROM user_profile WHERE id = 'default')")
    boolean profileExists();
}
