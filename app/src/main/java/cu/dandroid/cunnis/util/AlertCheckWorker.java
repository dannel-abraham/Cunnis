package cu.dandroid.cunnis.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.*;
import cu.dandroid.cunnis.data.local.db.entity.*;
import cu.dandroid.cunnis.data.model.AlertType;
import cu.dandroid.cunnis.data.model.Gender;
import cu.dandroid.cunnis.ui.activity.MainActivity;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Periodic worker that checks for pending alerts and sends notifications.
 * Scheduled by CunnisApp to run every 15 minutes.
 * Can also be triggered manually via OneTimeWorkRequest from AlertsFragment.
 */
public class AlertCheckWorker extends Worker {

    private static final String CHANNEL_ID = "farm_alerts";
    private static final String TAG = "AlertCheckWorker";

    // Output data keys
    public static final String KEY_NOTIFICATION_COUNT = "notification_count";

    public AlertCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // 1. Check if notifications are enabled
        if (!SharedPreferencesHelper.getInstance(getApplicationContext()).isNotificationsEnabled()) {
            return Result.success();
        }

        CunnisDatabase db = CunnisDatabase.getInstance(getApplicationContext());
        int notificationCount = 0;

        try {
            // 2. Get all enabled alert configs
            List<AlertConfig> enabledConfigs = db.alertConfigDao().getAllConfigsSync();

            if (enabledConfigs == null || enabledConfigs.isEmpty()) {
                return Result.success();
            }

            for (AlertConfig config : enabledConfigs) {
                if (!config.enabled) continue;

                int advanceDays = config.advanceDays > 0 ? config.advanceDays : 1;
                AlertType alertType = resolveAlertType(config.alertType);

                if (alertType == null) continue;

                switch (alertType) {
                    case ESTRUS_PREDICTION:
                        notificationCount += checkEstrusPrediction(db, advanceDays);
                        break;
                    case GESTATION_DUE:
                        notificationCount += checkGestationDue(db, advanceDays);
                        break;
                    case WEANING:
                        notificationCount += checkWeaning(db, advanceDays);
                        break;
                    case REPRODUCTIVE_AGE:
                        notificationCount += checkReproductiveAge(db, advanceDays);
                        break;
                    case WEIGHT_CHECK:
                        notificationCount += checkWeightCheck(db, advanceDays);
                        break;
                    case VACCINATION_DUE:
                        notificationCount += checkVaccinationDue(db, advanceDays);
                        break;
                    default:
                        break;
                }
            }

            // Set output data with notification count
            androidx.work.Data outputData = new androidx.work.Data.Builder()
                    .putInt(KEY_NOTIFICATION_COUNT, notificationCount)
                    .build();
            return Result.success(outputData);

        } catch (Exception e) {
            // Don't crash the worker, just log and return
            e.printStackTrace();
            return Result.retry();
        }
    }

    // ========================
    // Alert Check Methods
    // ========================

    /**
     * ESTRUS_PREDICTION: Check active females for upcoming estrus.
     * Logic: lastEstrusDate + ESTRUS_CYCLE_AVG - advanceDays <= today <= lastEstrusDate + ESTRUS_CYCLE_AVG + 1
     */
    private int checkEstrusPrediction(CunnisDatabase db, int advanceDays) {
        int count = 0;
        List<Rabbit> females = db.rabbitDao().getActiveFemalesSync();
        if (females == null) return 0;

        long todayStart = DateUtils.today();

        for (Rabbit female : females) {
            try {
                EstrusRecord latestEstrus = db.estrusRecordDao().getLatestEstrus(female.id);
                if (latestEstrus == null) continue;

                long predictedEstrus = DateUtils.addDays(latestEstrus.estrusDate, Constants.ESTRUS_CYCLE_AVG);
                long windowStart = DateUtils.addDays(predictedEstrus, -advanceDays);
                long windowEnd = DateUtils.addDays(predictedEstrus, 1);

                if (todayStart >= windowStart && todayStart <= windowEnd) {
                    String message = getApplicationContext().getString(R.string.alert_estrus_msg,
                            female.name, DateUtils.formatDate(predictedEstrus));
                    sendNotification(female.id, AlertType.ESTRUS_PREDICTION, message);
                    count++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    /**
     * GESTATION_DUE: Check successful matings for upcoming parturition.
     * Logic: dueDate - advanceDays <= today <= dueDate + 3
     * Skip if parturition already recorded for this mating.
     */
    private int checkGestationDue(CunnisDatabase db, int advanceDays) {
        int count = 0;
        List<MatingRecord> allMatings = db.matingRecordDao().getAllMatingRecordsSync();
        if (allMatings == null) return 0;

        long todayStart = DateUtils.today();

        for (MatingRecord mating : allMatings) {
            try {
                // Only check effective/successful matings
                boolean isEffective = mating.isEffective
                        || "SUCCESSFUL".equalsIgnoreCase(mating.result);
                if (!isEffective) continue;

                // Check if parturition already recorded
                ParturitionRecord existingParturition = db.parturitionRecordDao()
                        .getByMatingRecordId(mating.id);
                if (existingParturition != null) continue;

                long dueDate = DateUtils.addDays(mating.matingDate, Constants.GESTATION_AVG);
                long windowStart = DateUtils.addDays(dueDate, -advanceDays);
                long windowEnd = DateUtils.addDays(dueDate, 3);

                if (todayStart >= windowStart && todayStart <= windowEnd) {
                    Rabbit doe = db.rabbitDao().getRabbitByIdSync(mating.doeId);
                    String doeName = (doe != null) ? doe.name
                            : getApplicationContext().getString(R.string.alert_unknown_doe);
                    String message = getApplicationContext().getString(R.string.alert_gestation_msg,
                            doeName, DateUtils.formatDate(dueDate));
                    sendNotification(mating.doeId, AlertType.GESTATION_DUE, message);
                    count++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    /**
     * WEANING: Check parturition records for kits ready to wean.
     * Logic: weaningDate - advanceDays <= today <= weaningDate + 3
     * Only if weanedCount == 0 (not yet weaned).
     */
    private int checkWeaning(CunnisDatabase db, int advanceDays) {
        int count = 0;
        List<ParturitionRecord> allParturitions = db.parturitionRecordDao().getAllParturitionsSync();
        if (allParturitions == null) return 0;

        long todayStart = DateUtils.today();

        for (ParturitionRecord parturition : allParturitions) {
            try {
                // Skip if already weaned
                if (parturition.weanedCount > 0) continue;

                long weaningDate = DateUtils.addDays(parturition.parturitionDate, Constants.WEANING_AVG);
                long windowStart = DateUtils.addDays(weaningDate, -advanceDays);
                long windowEnd = DateUtils.addDays(weaningDate, 3);

                if (todayStart >= windowStart && todayStart <= windowEnd) {
                    Rabbit doe = db.rabbitDao().getRabbitByIdSync(parturition.doeId);
                    String doeName = (doe != null) ? doe.name
                            : getApplicationContext().getString(R.string.alert_unknown_doe);
                    String message = getApplicationContext().getString(R.string.alert_weaning_msg,
                            doeName, DateUtils.formatDate(weaningDate));
                    sendNotification(parturition.doeId, AlertType.WEANING, message);
                    count++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    /**
     * REPRODUCTIVE_AGE: Check active rabbits reaching sexual maturity.
     * Logic: age in days within +/- advanceDays of maturity threshold.
     * Uses SEXUAL_MATURITY_MED_FEMALE for females, SEXUAL_MATURITY_MED_MALE for males.
     */
    private int checkReproductiveAge(CunnisDatabase db, int advanceDays) {
        int count = 0;
        List<Rabbit> activeRabbits = db.rabbitDao().getActiveRabbitsSync();
        if (activeRabbits == null) return 0;

        for (Rabbit rabbit : activeRabbits) {
            try {
                if (rabbit.birthDate == null || rabbit.birthDate <= 0) continue;

                long ageMillis = System.currentTimeMillis() - rabbit.birthDate;
                int ageDays = (int) TimeUnit.MILLISECONDS.toDays(ageMillis);

                int maturityThreshold;
                if (rabbit.gender == Gender.FEMALE) {
                    maturityThreshold = Constants.SEXUAL_MATURITY_MED_FEMALE;
                } else if (rabbit.gender == Gender.MALE) {
                    maturityThreshold = Constants.SEXUAL_MATURITY_MED_MALE;
                } else {
                    continue; // Skip UNKNOWN gender
                }

                // Check if within +/- advanceDays of the maturity threshold
                int lowerBound = maturityThreshold - advanceDays;
                int upperBound = maturityThreshold + advanceDays;

                if (ageDays >= lowerBound && ageDays <= upperBound) {
                    String message = getApplicationContext().getString(R.string.alert_reproductive_msg, rabbit.name);
                    sendNotification(rabbit.id, AlertType.REPRODUCTIVE_AGE, message);
                    count++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    /**
     * WEIGHT_CHECK: Check active rabbits for overdue monthly weight checks.
     * Logic: Latest weight record older than 30 days (1 month).
     */
    private int checkWeightCheck(CunnisDatabase db, int advanceDays) {
        int count = 0;
        List<Rabbit> activeRabbits = db.rabbitDao().getActiveRabbitsSync();
        if (activeRabbits == null) return 0;

        long thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30 + advanceDays);

        for (Rabbit rabbit : activeRabbits) {
            try {
                WeightRecord latestWeight = db.weightRecordDao().getLatestWeight(rabbit.id);
                if (latestWeight == null) {
                    // No weight recorded at all - send notification
                    String message = getApplicationContext().getString(R.string.alert_weight_msg,
                            rabbit.name, getApplicationContext().getString(R.string.common_na));
                    sendNotification(rabbit.id, AlertType.WEIGHT_CHECK, message);
                    count++;
                } else if (latestWeight.recordDate < thirtyDaysAgo) {
                    // Last weight is older than 30 days + advanceDays
                    String message = getApplicationContext().getString(R.string.alert_weight_msg,
                            rabbit.name, DateUtils.formatDate(latestWeight.recordDate));
                    sendNotification(rabbit.id, AlertType.WEIGHT_CHECK, message);
                    count++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    /**
     * VACCINATION_DUE: Check upcoming vaccination due events.
     * Logic: nextDueDate <= now + advanceDays
     */
    private int checkVaccinationDue(CunnisDatabase db, int advanceDays) {
        int count = 0;
        long cutoffDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(advanceDays);

        List<HealthEvent> upcomingEvents = db.healthEventDao().getUpcomingDueEvents(cutoffDate);
        if (upcomingEvents == null) return 0;

        for (HealthEvent event : upcomingEvents) {
            try {
                if (event.nextDueDate <= 0) continue;

                Rabbit rabbit = db.rabbitDao().getRabbitByIdSync(event.rabbitId);
                String rabbitName = (rabbit != null) ? rabbit.name
                        : getApplicationContext().getString(R.string.alert_unknown_rabbit);
                String title = (event.title != null && !event.title.isEmpty()) ? event.title
                        : getApplicationContext().getString(R.string.health_vaccination);
                String message = getApplicationContext().getString(R.string.alert_vaccination_msg,
                        rabbitName, title);
                sendNotification(event.rabbitId, AlertType.VACCINATION_DUE, message);
                count++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    // ========================
    // Notification Helpers
    // ========================

    /**
     * Send a notification with a unique ID based on rabbitId and alert type hash.
     */
    private void sendNotification(long rabbitId, AlertType alertType, String message) {
        Context context = getApplicationContext();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_alerts),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(context.getString(R.string.notification_channel_alerts_desc));
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // Generate unique notification ID from rabbitId + alertType hash
        int notificationId = generateNotificationId(rabbitId, alertType);

        // Create intent to open main activity when notification is tapped
        android.content.Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.alert_notification_title))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 300, 200, 300});

        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Generate a deterministic notification ID from rabbitId + alertType.
     * Uses a simple hash to ensure uniqueness per rabbit per alert type.
     */
    private int generateNotificationId(long rabbitId, AlertType alertType) {
        String combined = rabbitId + "_" + alertType.getValue();
        return combined.hashCode();
    }

    /**
     * Resolve an AlertConfig.alertType string to AlertType enum.
     * Returns null if the string doesn't match any known type.
     */
    private AlertType resolveAlertType(String alertTypeStr) {
        if (alertTypeStr == null) return null;
        for (AlertType type : AlertType.values()) {
            if (type.getValue().equalsIgnoreCase(alertTypeStr)) {
                return type;
            }
        }
        return null;
    }
}
