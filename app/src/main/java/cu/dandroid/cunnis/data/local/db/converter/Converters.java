package cu.dandroid.cunnis.data.local.db.converter;

import androidx.room.TypeConverter;
import cu.dandroid.cunnis.data.model.Gender;
import cu.dandroid.cunnis.data.model.RabbitStatus;

public class Converters {

    @TypeConverter
    public Gender fromGenderString(String value) {
        if (value == null) return Gender.UNKNOWN;
        try {
            return Gender.valueOf(value);
        } catch (Exception e) {
            return Gender.UNKNOWN;
        }
    }

    @TypeConverter
    public String genderToString(Gender gender) {
        return gender != null ? gender.name() : Gender.UNKNOWN.name();
    }

    @TypeConverter
    public RabbitStatus fromRabbitStatusString(String value) {
        if (value == null) return RabbitStatus.ACTIVE;
        try {
            return RabbitStatus.valueOf(value);
        } catch (Exception e) {
            return RabbitStatus.ACTIVE;
        }
    }

    @TypeConverter
    public String rabbitStatusToString(RabbitStatus status) {
        return status != null ? status.name() : RabbitStatus.ACTIVE.name();
    }
}
