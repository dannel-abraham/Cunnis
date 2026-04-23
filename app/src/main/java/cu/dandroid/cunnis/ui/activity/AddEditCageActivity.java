package cu.dandroid.cunnis.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.databinding.ActivityAddEditCageBinding;
import cu.dandroid.cunnis.viewmodel.CageViewModel;

public class AddEditCageActivity extends AppCompatActivity {
    public static final String EXTRA_CAGE_ID = "cage_id";
    private ActivityAddEditCageBinding binding;
    private CageViewModel viewModel;
    private int cageId = -1;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditCageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(CageViewModel.class);

        // Check if editing
        if (getIntent().hasExtra(EXTRA_CAGE_ID)) {
            cageId = getIntent().getIntExtra(EXTRA_CAGE_ID, -1);
            isEditing = cageId > 0;
            if (isEditing) {
                binding.toolbar.setTitle(getString(R.string.cage_edit));
                viewModel.getCageById(cageId).observe(this, cage -> {
                    if (cage != null) {
                        binding.edtCageNumber.setText(String.valueOf(cage.cageNumber));
                        binding.edtNotes.setText(cage.notes != null ? cage.notes : "");
                    }
                });
            }
        }

        binding.btnSave.setOnClickListener(v -> saveCage());
    }

    private void saveCage() {
        String numberStr = binding.edtCageNumber.getText().toString().trim();
        if (numberStr.isEmpty()) {
            binding.tilCageNumber.setError(getString(R.string.error_required_field));
            return;
        }
        binding.tilCageNumber.setError(null);

        int cageNumber;
        try {
            cageNumber = Integer.parseInt(numberStr);
        } catch (NumberFormatException e) {
            binding.tilCageNumber.setError(getString(R.string.error_invalid_data));
            return;
        }

        String notes = binding.edtNotes.getText().toString().trim();

        if (isEditing) {
            viewModel.getCageById(cageId).observe(this, cage -> {
                if (cage != null) {
                    cage.cageNumber = cageNumber;
                    cage.notes = notes;
                    viewModel.update(cage);
                    Toast.makeText(this, R.string.common_success, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } else {
            Cage cage = new Cage();
            cage.cageNumber = cageNumber;
            cage.notes = notes.isEmpty() ? null : notes;
            viewModel.insert(cage);
            Toast.makeText(this, R.string.common_success, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
