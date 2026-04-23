package cu.dandroid.cunnis.ui.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.MatingRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.ParturitionRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.RabbitDao;
import cu.dandroid.cunnis.data.local.db.entity.MatingRecord;
import cu.dandroid.cunnis.data.local.db.entity.ParturitionRecord;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.databinding.ActivityParturitionRecordBinding;
import cu.dandroid.cunnis.util.Constants;
import cu.dandroid.cunnis.util.DateUtils;

public class ParturitionRecordActivity extends AppCompatActivity {

    public static final String EXTRA_RABBIT_ID = "rabbit_id";

    private ActivityParturitionRecordBinding binding;
    private ParturitionRecordDao parturitionRecordDao;
    private MatingRecordDao matingRecordDao;
    private RabbitDao rabbitDao;
    private long doeId = -1;
    private long selectedBuckId = -1;
    private long selectedParturitionDate;
    private long selectedDueDate = 0;
    private Long linkedMatingRecordId = null;
    private List<Rabbit> maleRabbits = new ArrayList<>();
    private List<MatingRecord> doeMatingRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityParturitionRecordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        parturitionRecordDao = ((CunnisApp) getApplication()).getDatabase().parturitionRecordDao();
        matingRecordDao = ((CunnisApp) getApplication()).getDatabase().matingRecordDao();
        rabbitDao = ((CunnisApp) getApplication()).getDatabase().rabbitDao();

        if (getIntent() != null) {
            doeId = getIntent().getLongExtra(EXTRA_RABBIT_ID, -1);
        }

        // Set default date to today
        selectedParturitionDate = DateUtils.today();
        binding.edtParturitionDate.setText(DateUtils.formatDate(selectedParturitionDate));

        // Complications checkbox toggle
        binding.chkComplications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.tilComplicationsNotes.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Load data
        loadData();

        // Date picker for parturition date
        binding.edtParturitionDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedParturitionDate);
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar dateCal = Calendar.getInstance();
                dateCal.set(year, month, dayOfMonth);
                selectedParturitionDate = dateCal.getTimeInMillis();
                binding.edtParturitionDate.setText(DateUtils.formatDate(selectedParturitionDate));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Date picker for estimated due date
        binding.edtEstimatedDueDate.setOnClickListener(v -> {
            Calendar initial = Calendar.getInstance();
            if (selectedDueDate > 0) {
                initial.setTimeInMillis(selectedDueDate);
            }
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar dateCal = Calendar.getInstance();
                dateCal.set(year, month, dayOfMonth);
                selectedDueDate = dateCal.getTimeInMillis();
                binding.edtEstimatedDueDate.setText(DateUtils.formatDate(selectedDueDate));
            }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show();
        });

        binding.btnSave.setOnClickListener(v -> saveParturitionRecord());
    }

    private void loadData() {
        new Thread(() -> {
            // Load doe info
            if (doeId > 0) {
                Rabbit doe = rabbitDao.getRabbitByIdSync(doeId);
                if (doe != null) {
                    String doeLabel = String.format(Locale.getDefault(), "%s (#%s)", doe.name, doe.identifier);
                    runOnUiThread(() -> binding.edtDoe.setText(doeLabel));
                }

                // Load doe's mating records
                doeMatingRecords = matingRecordDao.getMatingRecordsByDoeSync(doeId);
                List<String> matingLabels = new ArrayList<>();
                matingLabels.add(getString(R.string.common_na)); // No linked mating
                for (MatingRecord mr : doeMatingRecords) {
                    matingLabels.add(String.format(Locale.getDefault(), "%s - %s",
                            DateUtils.formatDate(mr.matingDate), mr.result != null ? mr.result : ""));
                }
                runOnUiThread(() -> {
                    ArrayAdapter<String> matingAdapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_dropdown_item_1line, matingLabels);
                    binding.actMatingRecord.setAdapter(matingAdapter);
                    binding.actMatingRecord.setText(matingLabels.get(0), false);

                    binding.actMatingRecord.setOnItemClickListener((parent, view1, position, id1) -> {
                        if (position == 0) {
                            linkedMatingRecordId = null;
                            selectedDueDate = 0;
                            binding.edtEstimatedDueDate.setText("");
                        } else {
                            MatingRecord mr = doeMatingRecords.get(position - 1);
                            linkedMatingRecordId = mr.id;
                            // Auto-calculate estimated due date
                            selectedDueDate = DateUtils.addDays(mr.matingDate, Constants.GESTATION_AVG);
                            binding.edtEstimatedDueDate.setText(DateUtils.formatDate(selectedDueDate));
                        }
                    });
                });
            }

            // Load active males for buck dropdown
            Rabbit cemental = rabbitDao.getCementalSync();
            List<Rabbit> activeRabbits = rabbitDao.getActiveRabbitsSync();
            maleRabbits = new ArrayList<>();
            List<String> buckLabels = new ArrayList<>();

            if (cemental != null) {
                maleRabbits.add(cemental);
                buckLabels.add(String.format(Locale.getDefault(), "%s (#%s) ★", cemental.name, cemental.identifier));
                selectedBuckId = cemental.id;
            }
            for (Rabbit r : activeRabbits) {
                if (r.id != doeId && (cemental == null || r.id != cemental.id)) {
                    maleRabbits.add(r);
                    buckLabels.add(String.format(Locale.getDefault(), "%s (#%s)", r.name, r.identifier));
                }
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> buckAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, buckLabels);
                binding.actBuck.setAdapter(buckAdapter);
                if (!buckLabels.isEmpty()) {
                    binding.actBuck.setText(buckLabels.get(0), false);
                }
            });
        }).start();
    }

    private long findBuckId(String buckText) {
        for (Rabbit r : maleRabbits) {
            String label = String.format(Locale.getDefault(), "%s (#%s)", r.name, r.identifier);
            if (buckText.startsWith(label) || buckText.contains(label)) {
                return r.id;
            }
        }
        return -1;
    }

    private void saveParturitionRecord() {
        String buckText = binding.actBuck.getText().toString().trim();
        long buckId = findBuckId(buckText);
        if (buckId <= 0) {
            binding.tilBuck.setError(getString(R.string.error_required_field));
            return;
        }
        binding.tilBuck.setError(null);

        String totalBornStr = binding.edtTotalBorn.getText().toString().trim();
        String bornAliveStr = binding.edtBornAlive.getText().toString().trim();
        String bornDeadStr = binding.edtBornDead.getText().toString().trim();
        String weanedStr = binding.edtWeaned.getText().toString().trim();
        String complicationsNotes = binding.edtComplicationsNotes.getText().toString().trim();
        String notes = binding.edtNotes.getText().toString().trim();

        int totalBorn = parseIntSafe(totalBornStr);
        int bornAlive = parseIntSafe(bornAliveStr);
        int bornDead = parseIntSafe(bornDeadStr);
        int weanedCount = parseIntSafe(weanedStr);

        ParturitionRecord record = new ParturitionRecord();
        record.doeId = doeId;
        record.buckId = buckId;
        record.matingRecordId = linkedMatingRecordId;
        record.parturitionDate = selectedParturitionDate;
        record.estimatedDueDate = selectedDueDate;
        record.totalBorn = totalBorn;
        record.bornAlive = bornAlive;
        record.bornDead = bornDead;
        record.weanedCount = weanedCount;
        record.hadComplications = binding.chkComplications.isChecked();
        record.complicationsNotes = complicationsNotes.isEmpty() ? null : complicationsNotes;
        record.notes = notes.isEmpty() ? null : notes;

        new Thread(() -> {
            parturitionRecordDao.insert(record);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.parturition_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
