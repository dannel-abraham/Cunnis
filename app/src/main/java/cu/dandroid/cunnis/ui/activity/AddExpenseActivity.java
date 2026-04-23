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
import cu.dandroid.cunnis.data.local.db.dao.ExpenseRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.RabbitDao;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.data.local.db.entity.ExpenseRecord;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.model.ExpenseCategory;
import cu.dandroid.cunnis.databinding.ActivityAddExpenseBinding;
import cu.dandroid.cunnis.util.DateUtils;

public class AddExpenseActivity extends AppCompatActivity {

    private ActivityAddExpenseBinding binding;
    private ExpenseRecordDao expenseRecordDao;
    private long selectedExpenseDate;
    private long selectedRabbitId = -1;
    private int selectedCageId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddExpenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        expenseRecordDao = ((CunnisApp) getApplication()).getDatabase().expenseRecordDao();

        // Set default date to today
        selectedExpenseDate = DateUtils.today();
        binding.edtExpenseDate.setText(DateUtils.formatDate(selectedExpenseDate));

        // Setup category dropdown
        ExpenseCategory[] categories = ExpenseCategory.values();
        String[] categoryLabels = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            categoryLabels[i] = categories[i].name();
        }
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categoryLabels);
        binding.actCategory.setAdapter(categoryAdapter);

        // Load dropdown data
        loadDropdownData();

        // Date picker
        binding.edtExpenseDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedExpenseDate);
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar dateCal = Calendar.getInstance();
                dateCal.set(year, month, dayOfMonth);
                selectedExpenseDate = dateCal.getTimeInMillis();
                binding.edtExpenseDate.setText(DateUtils.formatDate(selectedExpenseDate));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        binding.btnSave.setOnClickListener(v -> saveExpense());
    }

    private void loadDropdownData() {
        new Thread(() -> {
            RabbitDao rabbitDao = ((CunnisApp) getApplication()).getDatabase().rabbitDao();
            CageDao cageDao = ((CunnisApp) getApplication()).getDatabase().cageDao();

            // Load rabbits
            List<Rabbit> rabbits = rabbitDao.getAllRabbitsSync();
            List<String> rabbitLabels = new ArrayList<>();
            rabbitLabels.add(getString(R.string.common_na));
            for (Rabbit r : rabbits) {
                rabbitLabels.add(String.format(Locale.getDefault(), "%s (#%s)", r.name, r.identifier));
            }

            // Load cages
            List<Cage> cages = cageDao.getAllCagesSync();
            List<String> cageLabels = new ArrayList<>();
            cageLabels.add(getString(R.string.common_na));
            for (Cage c : cages) {
                cageLabels.add(String.format(Locale.getDefault(), "#%d", c.cageNumber));
            }

            runOnUiThread(() -> {
                // Rabbit dropdown
                ArrayAdapter<String> rabbitAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, rabbitLabels);
                binding.actRabbit.setAdapter(rabbitAdapter);
                binding.actRabbit.setText(rabbitLabels.get(0), false);

                binding.actRabbit.setOnItemClickListener((parent, view, position, id) -> {
                    if (position == 0) {
                        selectedRabbitId = -1;
                    } else {
                        selectedRabbitId = rabbits.get(position - 1).id;
                    }
                });

                // Cage dropdown
                ArrayAdapter<String> cageAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, cageLabels);
                binding.actCage.setAdapter(cageAdapter);
                binding.actCage.setText(cageLabels.get(0), false);

                binding.actCage.setOnItemClickListener((parent, view, position, id) -> {
                    if (position == 0) {
                        selectedCageId = -1;
                    } else {
                        selectedCageId = cages.get(position - 1).id;
                    }
                });
            });
        }).start();
    }

    private void saveExpense() {
        String categoryStr = binding.actCategory.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();
        String amountStr = binding.edtAmount.getText().toString().trim();
        String notes = binding.edtNotes.getText().toString().trim();

        if (description.isEmpty()) {
            binding.tilDescription.setError(getString(R.string.expense_description_required));
            return;
        }
        binding.tilDescription.setError(null);

        if (amountStr.isEmpty()) {
            binding.tilAmount.setError(getString(R.string.expense_amount_required));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            binding.tilAmount.setError(getString(R.string.error_invalid_data));
            return;
        }
        binding.tilAmount.setError(null);

        ExpenseRecord record = new ExpenseRecord();
        record.category = categoryStr.isEmpty() ? ExpenseCategory.OTHER.name() : categoryStr;
        record.description = description;
        record.amount = amount;
        record.rabbitId = selectedRabbitId > 0 ? selectedRabbitId : 0;
        record.cageId = selectedCageId > 0 ? selectedCageId : 0;
        record.expenseDate = selectedExpenseDate;
        record.notes = notes.isEmpty() ? null : notes;

        new Thread(() -> {
            expenseRecordDao.insert(record);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.expense_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
