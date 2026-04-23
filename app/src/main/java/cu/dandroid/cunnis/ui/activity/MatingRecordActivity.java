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
import cu.dandroid.cunnis.data.local.db.dao.MatingRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.RabbitDao;
import cu.dandroid.cunnis.data.local.db.entity.MatingRecord;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.model.MatingResult;
import cu.dandroid.cunnis.databinding.ActivityMatingRecordBinding;
import cu.dandroid.cunnis.util.DateUtils;

public class MatingRecordActivity extends AppCompatActivity {

    public static final String EXTRA_RABBIT_ID = "rabbit_id";

    private ActivityMatingRecordBinding binding;
    private MatingRecordDao matingRecordDao;
    private RabbitDao rabbitDao;
    private long doeId = -1;
    private long selectedBuckId = -1;
    private long selectedMatingDate;
    private List<Rabbit> maleRabbits = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMatingRecordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        matingRecordDao = ((CunnisApp) getApplication()).getDatabase().matingRecordDao();
        rabbitDao = ((CunnisApp) getApplication()).getDatabase().rabbitDao();

        // Get doe id from intent
        if (getIntent() != null) {
            doeId = getIntent().getLongExtra(EXTRA_RABBIT_ID, -1);
        }

        // Set default date to today
        selectedMatingDate = DateUtils.today();
        binding.edtMatingDate.setText(DateUtils.formatDate(selectedMatingDate));

        // Setup result dropdown
        String[] results = {
                MatingResult.PENDING.name(),
                MatingResult.SUCCESSFUL.name(),
                MatingResult.UNSUCCESSFUL.name()
        };
        ArrayAdapter<String> resultAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, results);
        binding.actResult.setAdapter(resultAdapter);
        binding.actResult.setText(MatingResult.PENDING.name(), false);

        // Load doe info and male rabbits
        loadData();

        // Date picker
        binding.edtMatingDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedMatingDate);
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar dateCal = Calendar.getInstance();
                dateCal.set(year, month, dayOfMonth);
                selectedMatingDate = dateCal.getTimeInMillis();
                binding.edtMatingDate.setText(DateUtils.formatDate(selectedMatingDate));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        binding.btnSave.setOnClickListener(v -> saveMatingRecord());
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
            }

            // Load cemental and all active males
            Rabbit cemental = rabbitDao.getCementalSync();
            List<Rabbit> activeMales = rabbitDao.getActiveRabbitsSync();
            maleRabbits = new ArrayList<>();

            // Build label list for bucks
            List<String> buckLabels = new ArrayList<>();
            if (cemental != null) {
                maleRabbits.add(cemental);
                buckLabels.add(String.format(Locale.getDefault(), "%s (#%s) ★", cemental.name, cemental.identifier));
                selectedBuckId = cemental.id;
            }
            for (Rabbit r : activeMales) {
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

    private void saveMatingRecord() {
        String buckText = binding.actBuck.getText().toString().trim();
        if (buckText.isEmpty()) {
            binding.tilBuck.setError(getString(R.string.error_required_field));
            return;
        }
        binding.tilBuck.setError(null);

        // Find selected buck id
        long buckId = -1;
        for (int i = 0; i < maleRabbits.size(); i++) {
            Rabbit r = maleRabbits.get(i);
            String label = String.format(Locale.getDefault(), "%s (#%s)", r.name, r.identifier);
            if (buckText.startsWith(label) || buckText.contains(label)) {
                buckId = r.id;
                break;
            }
        }
        if (buckId <= 0) return;

        boolean isEffective = binding.chkEffective.isChecked();
        String resultStr = binding.actResult.getText().toString().trim();
        String attemptStr = binding.edtAttemptNumber.getText().toString().trim();
        String observations = binding.edtObservations.getText().toString().trim();

        int attemptNumber = 1;
        try {
            attemptNumber = Integer.parseInt(attemptStr);
        } catch (NumberFormatException ignored) {}

        MatingRecord record = new MatingRecord();
        record.doeId = doeId;
        record.buckId = buckId;
        record.matingDate = selectedMatingDate;
        record.isEffective = isEffective;
        record.result = resultStr;
        record.attemptNumber = attemptNumber;
        record.observations = observations.isEmpty() ? null : observations;

        new Thread(() -> {
            matingRecordDao.insert(record);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.mating_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
