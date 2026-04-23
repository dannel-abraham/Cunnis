package cu.dandroid.cunnis.ui.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.CageDao;
import cu.dandroid.cunnis.data.local.db.dao.FeedingRecordDao;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.data.local.db.entity.FeedingRecord;
import cu.dandroid.cunnis.databinding.ActivityAddFeedingBinding;
import cu.dandroid.cunnis.util.DateUtils;

public class AddFeedingActivity extends AppCompatActivity {

    private ActivityAddFeedingBinding binding;
    private FeedingRecordDao feedingRecordDao;
    private long selectedFeedingDate;
    private List<Cage> cages = new ArrayList<>();
    private int selectedCageId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddFeedingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        feedingRecordDao = ((CunnisApp) getApplication()).getDatabase().feedingRecordDao();

        // Set default date to today
        selectedFeedingDate = DateUtils.today();
        binding.edtFeedingDate.setText(DateUtils.formatDate(selectedFeedingDate));

        // Load cages
        loadCages();

        // Date picker
        binding.edtFeedingDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedFeedingDate);
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar dateCal = Calendar.getInstance();
                dateCal.set(year, month, dayOfMonth);
                selectedFeedingDate = dateCal.getTimeInMillis();
                binding.edtFeedingDate.setText(DateUtils.formatDate(selectedFeedingDate));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        binding.btnSave.setOnClickListener(v -> saveFeedingRecord());
    }

    private void loadCages() {
        new Thread(() -> {
            CageDao cageDao = ((CunnisApp) getApplication()).getDatabase().cageDao();
            cages = cageDao.getAllCagesSync();

            List<String> cageLabels = new ArrayList<>();
            for (Cage c : cages) {
                cageLabels.add(String.format(Locale.getDefault(), "#%d", c.cageNumber));
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> cageAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, cageLabels);
                binding.actCage.setAdapter(cageAdapter);

                binding.actCage.setOnItemClickListener((parent, view, position, id) -> {
                    if (position >= 0 && position < cages.size()) {
                        selectedCageId = cages.get(position).id;
                    }
                });
            });
        }).start();
    }

    private void saveFeedingRecord() {
        String cageText = binding.actCage.getText().toString().trim();
        if (cageText.isEmpty()) {
            binding.tilCage.setError(getString(R.string.error_required_field));
            return;
        }
        if (selectedCageId <= 0) {
            binding.tilCage.setError(getString(R.string.error_required_field));
            return;
        }
        binding.tilCage.setError(null);

        String feedType = binding.edtFeedType.getText().toString().trim();
        if (feedType.isEmpty()) {
            binding.tilFeedType.setError(getString(R.string.feeding_feed_type_required));
            return;
        }
        binding.tilFeedType.setError(null);

        String quantityStr = binding.edtQuantity.getText().toString().trim();
        if (quantityStr.isEmpty()) {
            binding.tilQuantity.setError(getString(R.string.feeding_quantity_required));
            return;
        }

        double quantity;
        try {
            quantity = Double.parseDouble(quantityStr);
        } catch (NumberFormatException e) {
            binding.tilQuantity.setError(getString(R.string.error_invalid_data));
            return;
        }

        if (quantity <= 0) {
            binding.tilQuantity.setError(getString(R.string.feeding_quantity_required));
            return;
        }
        binding.tilQuantity.setError(null);

        String notes = binding.edtNotes.getText().toString().trim();

        FeedingRecord record = new FeedingRecord();
        record.cageId = selectedCageId;
        record.feedType = feedType;
        record.quantity = quantity;
        record.feedingDate = selectedFeedingDate;
        record.notes = notes.isEmpty() ? null : notes;

        new Thread(() -> {
            feedingRecordDao.insert(record);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.feeding_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
