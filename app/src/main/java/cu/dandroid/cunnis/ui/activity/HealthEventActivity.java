package cu.dandroid.cunnis.ui.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.HealthEventDao;
import cu.dandroid.cunnis.data.local.db.entity.HealthEvent;
import cu.dandroid.cunnis.data.model.HealthEventType;
import cu.dandroid.cunnis.databinding.ActivityHealthEventBinding;
import cu.dandroid.cunnis.util.Constants;
import cu.dandroid.cunnis.util.DateUtils;
import java.util.Calendar;
import java.util.Locale;

public class HealthEventActivity extends AppCompatActivity {

    public static final String EXTRA_RABBIT_ID = "rabbit_id";
    public static final String EXTRA_HEALTH_EVENT_ID = "health_event_id";

    private ActivityHealthEventBinding binding;
    private HealthEventDao healthEventDao;
    private long rabbitId = -1;
    private long healthEventId = -1;
    private boolean isEditing = false;
    private long selectedEventDate;
    private long selectedNextDueDate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHealthEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        healthEventDao = ((CunnisApp) getApplication()).getDatabase().healthEventDao();

        // Setup event type dropdown
        HealthEventType[] types = HealthEventType.values();
        String[] typeLabels = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeLabels[i] = types[i].name();
        }
        ArrayAdapter<String> eventTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, typeLabels);
        binding.actEventType.setAdapter(eventTypeAdapter);

        // Set default date to today
        selectedEventDate = DateUtils.today();
        binding.edtEventDate.setText(DateUtils.formatDate(selectedEventDate));

        // Get intent extras
        if (getIntent() != null) {
            rabbitId = getIntent().getLongExtra(EXTRA_RABBIT_ID, -1);
            healthEventId = getIntent().getLongExtra(EXTRA_HEALTH_EVENT_ID, -1);
            isEditing = healthEventId > 0;

            if (isEditing) {
                binding.toolbar.setTitle(getString(R.string.health_event_edit_title));
                loadHealthEvent();
            }
        }

        // Date pickers
        binding.edtEventDate.setOnClickListener(v -> showDatePicker(selectedEventDate, (year, month, day) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);
            selectedEventDate = cal.getTimeInMillis();
            binding.edtEventDate.setText(DateUtils.formatDate(selectedEventDate));
        }));

        binding.edtNextDueDate.setOnClickListener(v -> {
            Calendar initial = Calendar.getInstance();
            if (selectedNextDueDate > 0) {
                initial.setTimeInMillis(selectedNextDueDate);
            }
            showDatePicker(initial.getTimeInMillis(), (year, month, day) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day);
                selectedNextDueDate = cal.getTimeInMillis();
                binding.edtNextDueDate.setText(DateUtils.formatDate(selectedNextDueDate));
            });
        });

        binding.btnSave.setOnClickListener(v -> saveHealthEvent());
    }

    private void showDatePicker(long initialDate, DatePickerCallback callback) {
        Calendar cal = Calendar.getInstance();
        if (initialDate > 0) {
            cal.setTimeInMillis(initialDate);
        }
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            callback.onDateSet(year, month, dayOfMonth);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private interface DatePickerCallback {
        void onDateSet(int year, int month, int day);
    }

    private void loadHealthEvent() {
        new Thread(() -> {
            HealthEvent event = healthEventDao.getHealthEventsByRabbitSync(rabbitId).stream()
                    .filter(e -> e.id == healthEventId)
                    .findFirst()
                    .orElse(null);
            if (event != null) {
                runOnUiThread(() -> {
                    binding.actEventType.setText(event.eventType, false);
                    binding.edtTitle.setText(event.title != null ? event.title : "");
                    binding.edtDescription.setText(event.description != null ? event.description : "");
                    binding.edtVeterinarian.setText(event.veterinarian != null ? event.veterinarian : "");
                    binding.edtMedication.setText(event.medication != null ? event.medication : "");
                    if (event.cost > 0) {
                        binding.edtCost.setText(String.format(Locale.getDefault(), "%.2f", event.cost));
                    }
                    selectedEventDate = event.eventDate;
                    binding.edtEventDate.setText(DateUtils.formatDate(selectedEventDate));
                    if (event.nextDueDate > 0) {
                        selectedNextDueDate = event.nextDueDate;
                        binding.edtNextDueDate.setText(DateUtils.formatDate(selectedNextDueDate));
                    }
                    binding.edtNotes.setText(event.notes != null ? event.notes : "");
                });
            }
        }).start();
    }

    private void saveHealthEvent() {
        String eventTypeStr = binding.actEventType.getText().toString().trim();
        if (eventTypeStr.isEmpty()) {
            binding.tilEventType.setError(getString(R.string.health_event_type_required));
            return;
        }
        binding.tilEventType.setError(null);

        String title = binding.edtTitle.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();
        String veterinarian = binding.edtVeterinarian.getText().toString().trim();
        String medication = binding.edtMedication.getText().toString().trim();
        String costStr = binding.edtCost.getText().toString().trim();
        String notes = binding.edtNotes.getText().toString().trim();

        double cost = 0;
        if (!costStr.isEmpty()) {
            try {
                cost = Double.parseDouble(costStr);
            } catch (NumberFormatException e) {
                binding.tilCost.setError(getString(R.string.error_invalid_data));
                return;
            }
        }

        if (isEditing) {
            new Thread(() -> {
                HealthEvent event = healthEventDao.getHealthEventsByRabbitSync(rabbitId).stream()
                        .filter(e -> e.id == healthEventId)
                        .findFirst()
                        .orElse(null);
                if (event != null) {
                    event.eventType = eventTypeStr;
                    event.title = title.isEmpty() ? null : title;
                    event.description = description.isEmpty() ? null : description;
                    event.veterinarian = veterinarian.isEmpty() ? null : veterinarian;
                    event.medication = medication.isEmpty() ? null : medication;
                    event.cost = cost;
                    event.eventDate = selectedEventDate;
                    event.nextDueDate = selectedNextDueDate;
                    event.notes = notes.isEmpty() ? null : notes;
                    healthEventDao.update(event);
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.health_event_saved, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }).start();
        } else {
            HealthEvent event = new HealthEvent();
            event.rabbitId = rabbitId;
            event.eventType = eventTypeStr;
            event.title = title.isEmpty() ? null : title;
            event.description = description.isEmpty() ? null : description;
            event.veterinarian = veterinarian.isEmpty() ? null : veterinarian;
            event.medication = medication.isEmpty() ? null : medication;
            event.cost = cost;
            event.eventDate = selectedEventDate;
            event.nextDueDate = selectedNextDueDate;
            event.notes = notes.isEmpty() ? null : notes;

            new Thread(() -> {
                healthEventDao.insert(event);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.health_event_saved, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();
        }
    }
}
