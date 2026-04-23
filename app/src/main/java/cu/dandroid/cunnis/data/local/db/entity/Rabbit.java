package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;
import androidx.annotation.NonNull;
import cu.dandroid.cunnis.data.model.Gender;
import cu.dandroid.cunnis.data.model.RabbitStatus;

@Entity(
    tableName = "rabbits",
    foreignKeys = {
        @ForeignKey(entity = Cage.class, parentColumns = "id", childColumns = "cageId", onDelete = ForeignKey.SET_NULL),
        @ForeignKey(entity = Rabbit.class, parentColumns = "id", childColumns = "sireId", onDelete = ForeignKey.SET_NULL),
        @ForeignKey(entity = Rabbit.class, parentColumns = "id", childColumns = "damId", onDelete = ForeignKey.SET_NULL)
    },
    indices = {
        @Index(value = "cageId"),
        @Index(value = "sireId"),
        @Index(value = "damId"),
        @Index(value = "identifier", unique = true),
        @Index(value = "gender"),
        @Index(value = "status")
    }
)
public class Rabbit {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String identifier;

    @NonNull
    public String name;

    @NonNull
    public Gender gender = Gender.UNKNOWN;

    @NonNull
    public RabbitStatus status = RabbitStatus.ACTIVE;

    public String breed;

    public Long birthDate; // null if born outside the farm

    public Long sireId; // father rabbit id (null if not from farm breeding)

    public Long damId; // mother rabbit id (null if not from farm breeding)

    public Integer cageId; // null if in cemental cage or no cage

    public boolean isCemental = false; // true only for the single male reproductor

    public byte[] profilePhoto; // BLOB

    public String notes;

    public Long exitDate; // date when rabbit left the farm

    public String exitReason; // reason for leaving

    public String exitNotes; // additional notes about exit

    public double salePrice; // sale price when rabbit is sold

    public double currentWeight; // in pounds

    @NonNull
    public long createdAt = System.currentTimeMillis();

    @NonNull
    public long updatedAt = System.currentTimeMillis();

    public Rabbit() {}

    @Ignore
    public Rabbit(@NonNull String identifier, @NonNull String name, @NonNull Gender gender) {
        this.identifier = identifier;
        this.name = name;
        this.gender = gender;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
