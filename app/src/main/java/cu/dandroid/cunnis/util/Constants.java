package cu.dandroid.cunnis.util;

public final class Constants {
    private Constants() {}

    // Rabbit breeding constants (in days)
    public static final int ESTRUS_CYCLE_AVG = 17;
    public static final int ESTRUS_CYCLE_MIN = 14;
    public static final int ESTRUS_CYCLE_MAX = 21;
    public static final int GESTATION_AVG = 31;
    public static final int GESTATION_MIN = 28;
    public static final int GESTATION_MAX = 34;
    public static final int WEANING_MIN = 28;
    public static final int WEANING_AVG = 35;
    public static final int WEANING_MAX = 42;
    public static final int SEXUAL_MATURITY_SMALL_MALE = 135; // ~4.5 months
    public static final int SEXUAL_MATURITY_SMALL_FEMALE = 120; // ~4 months
    public static final int SEXUAL_MATURITY_MED_MALE = 180; // ~6 months
    public static final int SEXUAL_MATURITY_MED_FEMALE = 165; // ~5.5 months
    public static final int SEXUAL_MATURITY_LARGE_MALE = 240; // ~8 months
    public static final int SEXUAL_MATURITY_LARGE_FEMALE = 210; // ~7 months
    public static final int PSEUDOPREGNANCY_AVG = 14;
    public static final int POST_PARTUM_REBREED_MIN = 14;
    public static final int POST_PARTUM_REBREED_AVG = 21;
    public static final int NEST_BOX_DAY = 27;

    // Weight
    public static final String WEIGHT_UNIT = "lb";
    public static final double MAX_IDENTIFIER = 999999;
    public static final int MAX_ID_LENGTH = 6;

    // Backup
    public static final String BACKUP_EXTENSION = ".cbckp";
    public static final String BACKUP_MIME_TYPE = "application/octet-stream";
    public static final String BACKUP_FILE_PREFIX = "cunnis_backup_";

    // Preferences
    public static final String PREFS_NAME = "cunnis_preferences";
    public static final String PREF_THEME = "theme_mode";
    public static final String PREF_LANGUAGE = "app_language";
    public static final String PREF_PROFILE_SETUP = "profile_setup_done";
    public static final String PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";

    // Theme
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";

    // Photo
    public static final int MAX_PHOTO_WIDTH = 1024;
    public static final int MAX_PHOTO_HEIGHT = 1024;
    public static final int PHOTO_QUALITY = 80;

    // Request codes
    public static final int REQUEST_CAMERA = 1001;
    public static final int REQUEST_GALLERY = 1002;
    public static final int REQUEST_PERMISSION = 1003;
    public static final int REQUEST_EXPORT = 1004;
    public static final int REQUEST_IMPORT = 1005;
}
