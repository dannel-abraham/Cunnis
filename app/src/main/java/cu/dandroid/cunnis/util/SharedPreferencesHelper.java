package cu.dandroid.cunnis.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class SharedPreferencesHelper {
    private static volatile SharedPreferencesHelper instance;
    private final SharedPreferences prefs;

    private SharedPreferencesHelper(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static SharedPreferencesHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (SharedPreferencesHelper.class) {
                if (instance == null) {
                    instance = new SharedPreferencesHelper(context);
                }
            }
        }
        return instance;
    }

    public void setTheme(String theme) {
        prefs.edit().putString(Constants.PREF_THEME, theme).apply();
    }

    public String getTheme() {
        return prefs.getString(Constants.PREF_THEME, Constants.THEME_SYSTEM);
    }

    public void setLanguage(String language) {
        prefs.edit().putString(Constants.PREF_LANGUAGE, language).apply();
    }

    public String getLanguage() {
        return prefs.getString(Constants.PREF_LANGUAGE, "");
    }

    public void setProfileSetupDone(boolean done) {
        prefs.edit().putBoolean(Constants.PREF_PROFILE_SETUP, done).apply();
    }

    public boolean isProfileSetupDone() {
        return prefs.getBoolean(Constants.PREF_PROFILE_SETUP, false);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, true);
    }
}
