package cu.dandroid.cunnis.ui.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Calendar;
import java.util.Locale;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.CunnisDatabase;
import cu.dandroid.cunnis.data.local.db.dao.PhotoRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.WeightRecordDao;
import cu.dandroid.cunnis.data.local.db.entity.PhotoRecord;
import cu.dandroid.cunnis.data.local.db.entity.WeightRecord;
import cu.dandroid.cunnis.databinding.ActivityWeightRecordBinding;
import cu.dandroid.cunnis.util.Constants;
import cu.dandroid.cunnis.util.DateUtils;
import cu.dandroid.cunnis.util.ImageUtils;

public class WeightRecordActivity extends AppCompatActivity {

    public static final String EXTRA_RABBIT_ID = "rabbit_id";

    private ActivityWeightRecordBinding binding;
    private WeightRecordDao weightRecordDao;
    private PhotoRecordDao photoRecordDao;
    private long rabbitId = -1;
    private long selectedRecordDate;
    private byte[] photoData = null;

    // Modern ActivityResultLaunchers
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try {
                        Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                        if (bitmap != null) {
                            photoData = ImageUtils.compressBitmap(bitmap);
                            binding.imgPhotoPreview.setImageBitmap(ImageUtils.bytesToBitmap(photoData));
                            binding.btnRemovePhoto.setVisibility(android.view.View.VISIBLE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try {
                        Uri selectedImage = result.getData().getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                        if (bitmap != null) {
                            photoData = ImageUtils.compressBitmap(bitmap);
                            binding.imgPhotoPreview.setImageBitmap(ImageUtils.bytesToBitmap(photoData));
                            binding.btnRemovePhoto.setVisibility(android.view.View.VISIBLE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWeightRecordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        CunnisDatabase db = ((CunnisApp) getApplication()).getDatabase();
        weightRecordDao = db.weightRecordDao();
        photoRecordDao = db.photoRecordDao();

        if (getIntent() != null) {
            rabbitId = getIntent().getLongExtra(EXTRA_RABBIT_ID, -1);
        }

        // Set default date to today
        selectedRecordDate = DateUtils.today();
        binding.edtRecordDate.setText(DateUtils.formatDate(selectedRecordDate));

        // Load weight stats
        loadWeightStats();

        // Date picker
        binding.edtRecordDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedRecordDate);
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar dateCal = Calendar.getInstance();
                dateCal.set(year, month, dayOfMonth);
                selectedRecordDate = dateCal.getTimeInMillis();
                binding.edtRecordDate.setText(DateUtils.formatDate(selectedRecordDate));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Photo button - show dialog: Take Photo or Select from Gallery
        binding.btnTakePhoto.setOnClickListener(v -> showPhotoOptions());

        // Remove photo button
        binding.btnRemovePhoto.setOnClickListener(v -> {
            photoData = null;
            binding.imgPhotoPreview.setImageResource(android.R.drawable.ic_menu_camera);
            binding.btnRemovePhoto.setVisibility(android.view.View.GONE);
        });

        binding.btnSave.setOnClickListener(v -> saveWeightRecord());
    }

    private void showPhotoOptions() {
        String[] options = {getString(R.string.weight_take_photo), getString(R.string.weight_select_gallery)};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.weight_photo))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                }).show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, Constants.REQUEST_CAMERA);
            return;
        }
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, R.string.error_camera, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadWeightStats() {
        new Thread(() -> {
            if (rabbitId <= 0) return;

            WeightRecord latest = weightRecordDao.getLatestWeight(rabbitId);
            double avg = weightRecordDao.getAverageWeight(rabbitId);

            String latestText = getString(R.string.common_na);
            if (latest != null && latest.weight > 0) {
                latestText = String.format(Locale.getDefault(), "%.1f %s", latest.weight, Constants.WEIGHT_UNIT);
            }
            String avgText = getString(R.string.common_na);
            if (avg > 0) {
                avgText = String.format(Locale.getDefault(), "%.1f %s", avg, Constants.WEIGHT_UNIT);
            }

            String finalLatestText = latestText;
            String finalAvgText = avgText;
            runOnUiThread(() -> {
                binding.txtLatestWeight.setText(finalLatestText);
                binding.txtAvgWeight.setText(finalAvgText);
            });
        }).start();
    }

    private void saveWeightRecord() {
        String weightStr = binding.edtWeight.getText().toString().trim();
        if (weightStr.isEmpty()) {
            binding.tilWeight.setError(getString(R.string.weight_required));
            return;
        }

        double weight;
        try {
            weight = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            binding.tilWeight.setError(getString(R.string.error_invalid_data));
            return;
        }

        if (weight <= 0) {
            binding.tilWeight.setError(getString(R.string.weight_required));
            return;
        }
        binding.tilWeight.setError(null);

        String notes = binding.edtNotes.getText().toString().trim();

        WeightRecord record = new WeightRecord();
        record.rabbitId = rabbitId;
        record.weight = weight;
        record.recordDate = selectedRecordDate;
        record.notes = notes.isEmpty() ? null : notes;

        // Capture values for background thread
        final byte[] capturedPhoto = photoData;
        final String photoNote = getString(R.string.weight_photo_note) + weightStr + " " + Constants.WEIGHT_UNIT;

        new Thread(() -> {
            weightRecordDao.insert(record);

            // Also save photo as PhotoRecord if a photo was taken
            if (capturedPhoto != null && rabbitId > 0) {
                PhotoRecord photoRecord = new PhotoRecord();
                photoRecord.rabbitId = rabbitId;
                photoRecord.photoData = capturedPhoto;
                photoRecord.photoDate = selectedRecordDate;
                photoRecord.notes = notes.isEmpty() ? null : photoNote;
                photoRecord.createdAt = System.currentTimeMillis();
                photoRecordDao.insert(photoRecord);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, R.string.weight_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
