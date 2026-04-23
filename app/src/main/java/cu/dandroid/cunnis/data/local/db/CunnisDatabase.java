package cu.dandroid.cunnis.data.local.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import cu.dandroid.cunnis.data.local.db.converter.Converters;
import cu.dandroid.cunnis.data.local.db.dao.*;
import cu.dandroid.cunnis.data.local.db.entity.*;

@Database(
    entities = {
        UserProfile.class,
        Cage.class,
        Rabbit.class,
        WeightRecord.class,
        PhotoRecord.class,
        HealthEvent.class,
        EstrusRecord.class,
        MatingRecord.class,
        ParturitionRecord.class,
        MovementRecord.class,
        FeedingRecord.class,
        ExpenseRecord.class,
        Breed.class,
        AlertConfig.class
    },
    version = 1,
    exportSchema = true
)
@TypeConverters({Converters.class})
public abstract class CunnisDatabase extends RoomDatabase {

    private static volatile CunnisDatabase INSTANCE;

    public abstract UserProfileDao userProfileDao();
    public abstract CageDao cageDao();
    public abstract RabbitDao rabbitDao();
    public abstract WeightRecordDao weightRecordDao();
    public abstract PhotoRecordDao photoRecordDao();
    public abstract HealthEventDao healthEventDao();
    public abstract EstrusRecordDao estrusRecordDao();
    public abstract MatingRecordDao matingRecordDao();
    public abstract ParturitionRecordDao parturitionRecordDao();
    public abstract MovementRecordDao movementRecordDao();
    public abstract FeedingRecordDao feedingRecordDao();
    public abstract ExpenseRecordDao expenseRecordDao();
    public abstract BreedDao breedDao();
    public abstract AlertConfigDao alertConfigDao();

    public static CunnisDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (CunnisDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        CunnisDatabase.class,
                        "cunnis_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Close and clear the singleton instance.
     * Must be called before importing a new database file so that
     * the next {@link #getInstance(Context)} call re-opens it fresh.
     */
    public static void resetInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
