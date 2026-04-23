package cu.dandroid.cunnis;

import android.app.Application;
import androidx.work.*;
import cu.dandroid.cunnis.data.local.db.CunnisDatabase;
import cu.dandroid.cunnis.util.AlertCheckWorker;

import java.util.concurrent.TimeUnit;

public class CunnisApp extends Application {

    private static CunnisApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // Initialize database
        CunnisDatabase.getInstance(this);

        // Schedule periodic alert checking with WorkManager
        scheduleAlertWorker();
    }

    private void scheduleAlertWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest alertWork = new PeriodicWorkRequest.Builder(
                AlertCheckWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "cunnis_alert_check",
                ExistingPeriodicWorkPolicy.KEEP,
                alertWork
        );
    }

    public static CunnisApp getInstance() {
        return instance;
    }

    public CunnisDatabase getDatabase() {
        return CunnisDatabase.getInstance(this);
    }
}
