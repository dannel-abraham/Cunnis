package cu.dandroid.cunnis.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.CunnisDatabase;
import cu.dandroid.cunnis.data.local.db.dao.CageDao;
import cu.dandroid.cunnis.data.local.db.dao.RabbitDao;
import cu.dandroid.cunnis.data.local.db.dao.MatingRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.ParturitionRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.HealthEventDao;
import cu.dandroid.cunnis.data.local.db.dao.WeightRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.FeedingRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.ExpenseRecordDao;
import cu.dandroid.cunnis.databinding.ActivityExportImportBinding;
import cu.dandroid.cunnis.util.Constants;

public class ExportImportActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";

    private ActivityExportImportBinding binding;

    /** Modern file-picker launcher replacing the deprecated startActivityForResult. */
    private final ActivityResultLauncher<Intent> importFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importDatabase(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExportImportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Load database stats
        loadDatabaseStats();

        // Export click
        binding.cardExport.setOnClickListener(v -> exportDatabase());

        // Import click
        binding.cardImport.setOnClickListener(v -> showImportWarning());
    }

    private void loadDatabaseStats() {
        new Thread(() -> {
            CunnisDatabase db = ((CunnisApp) getApplication()).getDatabase();
            RabbitDao rabbitDao = db.rabbitDao();
            CageDao cageDao = db.cageDao();
            MatingRecordDao matingRecordDao = db.matingRecordDao();
            ParturitionRecordDao parturitionRecordDao = db.parturitionRecordDao();
            HealthEventDao healthEventDao = db.healthEventDao();
            WeightRecordDao weightRecordDao = db.weightRecordDao();
            FeedingRecordDao feedingRecordDao = db.feedingRecordDao();
            ExpenseRecordDao expenseRecordDao = db.expenseRecordDao();

            int rabbitCount = rabbitDao.getActiveRabbitCountSync();
            int cageCount = cageDao.getAllCagesSync().size();

            // Total records count (sum of all record types)
            int totalRecords = rabbitCount + cageCount;

            int rabbitTotal = 0;
            try { rabbitTotal = rabbitDao.getAllRabbitsSync().size(); } catch (Exception ignored) {}
            int matingTotal = 0;
            try { matingTotal = matingRecordDao.getAllMatingRecords().getValue() != null ? matingRecordDao.getAllMatingRecords().getValue().size() : 0; } catch (Exception ignored) {}
            int healthTotal = 0;
            try { healthTotal = healthEventDao.getUpcomingDueEvents(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000).size(); } catch (Exception ignored) {}

            int finalRabbitCount = rabbitTotal > 0 ? rabbitTotal : rabbitCount;
            runOnUiThread(() -> {
                binding.txtRabbitCount.setText(String.valueOf(finalRabbitCount));
                binding.txtCageCount.setText(String.valueOf(cageCount));
                binding.txtRecordCount.setText(getString(R.string.export_total_records, totalRecords));
            });
        }).start();
    }

    private void exportDatabase() {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.txtStatus.setVisibility(android.view.View.VISIBLE);
        binding.txtStatus.setText(getString(R.string.export_in_progress));

        new Thread(() -> {
            try {
                CunnisDatabase db = ((CunnisApp) getApplication()).getDatabase();

                // --- FIX 1: Checkpoint the WAL before copying to avoid corrupt backup ---
                try {
                    db.getOpenHelper().getWritableDatabase().execSQL("PRAGMA wal_checkpoint(FULL);");
                } catch (Exception e) {
                    e.printStackTrace();
                    // Non-fatal: best-effort checkpoint
                }

                // Room database is stored in the app's database directory
                File dbFile = getDatabasePath("cunnis_database");
                if (!dbFile.exists()) {
                    // Try the default Room database path
                    String dbPath = getApplication().getDatabasePath("cunnis_database").getAbsolutePath();
                    dbFile = new File(dbPath);
                }

                // Also look for WAL and SHM files
                File walFile = new File(dbFile.getParent(), "cunnis_database-wal");
                File shmFile = new File(dbFile.getParent(), "cunnis_database-shm");

                if (!dbFile.exists()) {
                    runOnUiThread(() -> {
                        binding.progressBar.setVisibility(android.view.View.GONE);
                        binding.txtStatus.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, R.string.common_error, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Create export directory in downloads
                File exportDir = new File(getExternalFilesDir(null), "backup");
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }

                String timestamp = String.valueOf(System.currentTimeMillis());
                File exportFile = new File(exportDir, Constants.BACKUP_FILE_PREFIX + timestamp + Constants.BACKUP_EXTENSION);

                // Copy database file
                copyFile(dbFile, exportFile);

                // Copy WAL and SHM files if they exist
                if (walFile.exists()) {
                    copyFile(walFile, new File(exportDir, Constants.BACKUP_FILE_PREFIX + timestamp + "-wal" + Constants.BACKUP_EXTENSION));
                }
                if (shmFile.exists()) {
                    copyFile(shmFile, new File(exportDir, Constants.BACKUP_FILE_PREFIX + timestamp + "-shm" + Constants.BACKUP_EXTENSION));
                }

                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.txtStatus.setVisibility(android.view.View.GONE);

                    // Show success dialog
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.common_success)
                            .setMessage(getString(R.string.export_success) + "\n" + exportFile.getAbsolutePath())
                            .setPositiveButton(R.string.common_ok, null)
                            .show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.txtStatus.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, R.string.common_error, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void copyFile(File src, File dst) throws Exception {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    private void showImportWarning() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.import_db)
                .setMessage(R.string.import_confirm)
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                    openFilePicker();
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    /** --- FIX 3: Modern ActivityResultLauncher replaces deprecated startActivityForResult --- */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {"application/octet-stream", "application/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        importFileLauncher.launch(intent);
    }

    private void importDatabase(Uri uri) {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.txtStatus.setVisibility(android.view.View.VISIBLE);
        binding.txtStatus.setText(getString(R.string.import_in_progress));

        new Thread(() -> {
            try {
                File dbFile = getApplication().getDatabasePath("cunnis_database");
                File walFile = new File(dbFile.getParent(), "cunnis_database-wal");
                File shmFile = new File(dbFile.getParent(), "cunnis_database-shm");

                // Copy imported file to database location FIRST (before closing)
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(dbFile)) {
                    if (in == null) {
                        runOnUiThread(() -> {
                            binding.progressBar.setVisibility(android.view.View.GONE);
                            binding.txtStatus.setVisibility(android.view.View.GONE);
                            Toast.makeText(this, R.string.import_error, Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }

                // --- FIX 2: Close and reset the database instance after replacing the file ---
                CunnisDatabase.resetInstance();

                // Clear any leftover WAL and SHM files so the fresh instance starts clean
                if (walFile.exists()) walFile.delete();
                if (shmFile.exists()) shmFile.delete();

                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.txtStatus.setVisibility(android.view.View.GONE);

                    // Show restart-required dialog
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.common_success)
                            .setMessage(R.string.import_restart_required)
                            .setCancelable(false)
                            .setPositiveButton(R.string.import_restart, (dialog, which) -> {
                                // Restart the app by relaunching the main activity with FLAG_ACTIVITY_CLEAR_TASK
                                Intent restartIntent = getPackageManager()
                                        .getLaunchIntentForPackage(getPackageName());
                                if (restartIntent != null) {
                                    restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(restartIntent);
                                }
                                // Ensure the process ends
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(0);
                            })
                            .show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.txtStatus.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, R.string.import_error, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
