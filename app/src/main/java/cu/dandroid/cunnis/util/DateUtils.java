package cu.dandroid.cunnis.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class DateUtils {
    private DateUtils() {}

    private static final String DATE_FORMAT = "dd/MM/yyyy";
    private static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm";
    private static final String SHORT_DATE_FORMAT = "dd/MM/yy";

    public static String formatDate(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    public static String formatDateTime(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    public static String formatShortDate(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(SHORT_DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    public static String calculateAge(long birthDateMillis) {
        if (birthDateMillis <= 0) return "";
        long diff = System.currentTimeMillis() - birthDateMillis;
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (days < 0) days = 0;

        int years = (int) (days / 365);
        int months = (int) ((days % 365) / 30);
        int remainDays = (int) (days % 30);

        if (years > 0) {
            return years + "y " + months + "m";
        } else if (months > 0) {
            return months + "m " + remainDays + "d";
        } else {
            return days + "d";
        }
    }

    public static int getDaysBetween(long startMillis, long endMillis) {
        return (int) TimeUnit.MILLISECONDS.toDays(endMillis - startMillis);
    }

    public static int getDaysFromNow(long targetMillis) {
        return (int) TimeUnit.MILLISECONDS.toDays(targetMillis - System.currentTimeMillis());
    }

    public static long addDays(long baseMillis, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(baseMillis);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTimeInMillis();
    }

    public static long today() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static boolean isToday(long millis) {
        return getDaysBetween(millis, today()) == 0;
    }

    public static boolean isPast(long millis) {
        return millis < System.currentTimeMillis();
    }

    public static boolean isFuture(long millis) {
        return millis > System.currentTimeMillis();
    }
}
