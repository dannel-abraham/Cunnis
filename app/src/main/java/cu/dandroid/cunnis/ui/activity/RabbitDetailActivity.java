package cu.dandroid.cunnis.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AutoCompleteTextView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.CageDao;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.model.ExitReason;
import cu.dandroid.cunnis.data.model.Gender;
import cu.dandroid.cunnis.data.model.RabbitStatus;
import cu.dandroid.cunnis.databinding.ActivityRabbitDetailBinding;
import cu.dandroid.cunnis.ui.adapter.RabbitDetailPagerAdapter;
import cu.dandroid.cunnis.util.DateUtils;
import cu.dandroid.cunnis.viewmodel.RabbitViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RabbitDetailActivity extends AppCompatActivity {
    public static final String EXTRA_RABBIT_ID = "rabbit_id";
    private ActivityRabbitDetailBinding binding;
    private RabbitViewModel viewModel;
    private long rabbitId;
    private long selectedExitDate = DateUtils.today();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRabbitDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(RabbitViewModel.class);
        rabbitId = getIntent().getLongExtra(EXTRA_RABBIT_ID, -1);

        if (rabbitId <= 0) {
            finish();
            return;
        }

        setupViewPager();
        loadRabbit();
    }

    private void setupViewPager() {
        RabbitDetailPagerAdapter adapter = new RabbitDetailPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0: tab.setText(R.string.rabbit_profile); break;
                        case 1: tab.setText(R.string.weight_title); break;
                        case 2: tab.setText(R.string.health_title); break;
                        case 3: tab.setText(R.string.repro_title); break;
                    }
                }).attach();
    }

    private void loadRabbit() {
        viewModel.getRabbitById(rabbitId).observe(this, rabbit -> {
            if (rabbit == null) {
                finish();
                return;
            }
            binding.txtName.setText(rabbit.name);
            binding.txtIdentifier.setText(rabbit.identifier);

            // Gender chip
            if (rabbit.gender == Gender.MALE) {
                binding.chipGender.setText(R.string.rabbit_male);
                binding.chipGender.setChipBackgroundColorResource(R.color.cunnis_male);
                binding.chipGender.setTextColor(getColor(R.color.cunnis_white));
            } else if (rabbit.gender == Gender.FEMALE) {
                binding.chipGender.setText(R.string.rabbit_female);
                binding.chipGender.setChipBackgroundColorResource(R.color.cunnis_female);
                binding.chipGender.setTextColor(getColor(R.color.cunnis_white));
            }

            // Status chip
            binding.chipStatus.setText(rabbit.status.name());

            // Breed chip
            if (rabbit.breed != null && !rabbit.breed.isEmpty()) {
                binding.chipBreed.setText(rabbit.breed);
                binding.chipBreed.setVisibility(View.VISIBLE);
            }

            // Photo
            if (rabbit.profilePhoto != null) {
                binding.imgProfile.setImageBitmap(cu.dandroid.cunnis.util.ImageUtils.bytesToBitmap(rabbit.profilePhoto));
            }

            invalidateOptionsMenu();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.rabbit_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Rabbit rabbit = viewModel.getRabbitByIdSync(rabbitId);
        if (rabbit != null) {
            // Hide record exit if rabbit is not active
            boolean isActive = rabbit.status == RabbitStatus.ACTIVE;
            menu.findItem(R.id.action_record_exit).setVisible(isActive);
            menu.findItem(R.id.action_transfer_cage).setVisible(isActive);
            menu.findItem(R.id.action_set_cemental).setVisible(isActive);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            Intent intent = new Intent(this, AddEditRabbitActivity.class);
            intent.putExtra(AddEditRabbitActivity.EXTRA_RABBIT_ID, rabbitId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_stats) {
            Intent intent = new Intent(this, RabbitStatsActivity.class);
            intent.putExtra(RabbitStatsActivity.EXTRA_RABBIT_ID, rabbitId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_set_cemental) {
            Rabbit rabbit = viewModel.getRabbitByIdSync(rabbitId);
            if (rabbit != null && rabbit.gender == Gender.MALE) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cemental_confirm_change)
                    .setPositiveButton(R.string.common_yes, (d, w) -> {
                        viewModel.setCemental(rabbit);
                        Toast.makeText(this, R.string.common_success, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.common_no, null)
                    .show();
            } else {
                Toast.makeText(this, R.string.cemental_info, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_record_exit) {
            showExitDialog();
            return true;
        } else if (id == R.id.action_transfer_cage) {
            showTransferDialog();
            return true;
        } else if (id == R.id.action_delete) {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.common_delete)
                .setMessage(R.string.rabbit_delete_confirm)
                .setPositiveButton(R.string.common_yes, (d, w) -> {
                    Rabbit rabbit = viewModel.getRabbitByIdSync(rabbitId);
                    if (rabbit != null) {
                        viewModel.delete(rabbit);
                        Toast.makeText(this, R.string.rabbit_deleted, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton(R.string.common_no, null)
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ==================== TASK 1: Exit/Sale Management ====================

    private void showExitDialog() {
        Rabbit rabbit = viewModel.getRabbitByIdSync(rabbitId);
        if (rabbit == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_record_exit, null);

        com.google.android.material.textfield.TextInputLayout tilExitReason = dialogView.findViewById(R.id.tilExitReason);
        AutoCompleteTextView actExitReason = dialogView.findViewById(R.id.actExitReason);
        com.google.android.material.textfield.TextInputEditText edtExitDate = dialogView.findViewById(R.id.edtExitDate);
        com.google.android.material.textfield.TextInputEditText edtExitNotes = dialogView.findViewById(R.id.edtExitNotes);
        com.google.android.material.textfield.TextInputLayout tilSalePrice = dialogView.findViewById(R.id.tilSalePrice);
        com.google.android.material.textfield.TextInputEditText edtSalePrice = dialogView.findViewById(R.id.edtSalePrice);

        // Populate exit reason dropdown
        String[] reasonLabels = {
                getString(R.string.exit_natural_death),
                getString(R.string.exit_disease),
                getString(R.string.exit_sold),
                getString(R.string.exit_slaughtered),
                getString(R.string.exit_accident),
                getString(R.string.common_notes) // OTHER mapped to "Other" - reuse a label
        };
        String[] reasonValues = new String[]{
                ExitReason.NATURAL_DEATH.name(),
                ExitReason.DISEASE.name(),
                ExitReason.SOLD.name(),
                ExitReason.SLAUGHTERED.name(),
                ExitReason.ACCIDENT.name(),
                ExitReason.OTHER.name()
        };

        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, reasonLabels);
        actExitReason.setAdapter(reasonAdapter);

        // Show/hide sale price field based on exit reason selection
        actExitReason.setOnItemClickListener((parent, view, position, id) -> {
            String selectedValue = reasonValues[position];
            if (ExitReason.SOLD.name().equals(selectedValue)) {
                tilSalePrice.setVisibility(View.VISIBLE);
            } else {
                tilSalePrice.setVisibility(View.GONE);
                edtSalePrice.setText("");
            }
        });

        // Default exit date to today
        selectedExitDate = DateUtils.today();
        edtExitDate.setText(DateUtils.formatDate(selectedExitDate));

        // Date picker on click
        edtExitDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.exit_date))
                    .setSelection(selectedExitDate)
                    .build();
            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedExitDate = selection;
                edtExitDate.setText(DateUtils.formatDate(selection));
            });
            datePicker.show(getSupportFragmentManager(), "exit_date_picker");
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.exit_title)
                .setView(dialogView)
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                    String selectedReasonStr = actExitReason.getText().toString().trim();
                    if (selectedReasonStr.isEmpty()) {
                        Toast.makeText(this, R.string.exit_select_reason, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Find the selected reason index
                    String selectedReasonValue = ExitReason.OTHER.name();
                    for (int i = 0; i < reasonLabels.length; i++) {
                        if (reasonLabels[i].equals(selectedReasonStr)) {
                            selectedReasonValue = reasonValues[i];
                            break;
                        }
                    }

                    // Validate sale price when SOLD
                    double salePrice = 0;
                    if (ExitReason.SOLD.name().equals(selectedReasonValue)) {
                        String priceStr = edtSalePrice.getText().toString().trim();
                        if (priceStr.isEmpty()) {
                            Toast.makeText(this, R.string.exit_sale_price, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        try {
                            salePrice = Double.parseDouble(priceStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, R.string.error_invalid_data, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    // Confirm before recording
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.exit_confirm)
                            .setPositiveButton(R.string.common_yes, (confirmDialog, confirmWhich) -> {
                                recordExit(rabbit, selectedReasonValue, edtExitNotes.getText().toString().trim(), salePrice);
                            })
                            .setNegativeButton(R.string.common_no, null)
                            .show();
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    private void recordExit(Rabbit rabbit, String reasonValue, String notes, double salePrice) {
        // Map ExitReason to RabbitStatus
        ExitReason exitReason = ExitReason.valueOf(reasonValue);
        switch (exitReason) {
            case SOLD:
                rabbit.status = RabbitStatus.SOLD;
                break;
            case SLAUGHTERED:
                rabbit.status = RabbitStatus.SLAUGHTERED;
                break;
            case NATURAL_DEATH:
            case DISEASE:
            case ACCIDENT:
            default:
                rabbit.status = RabbitStatus.DEAD;
                break;
        }

        rabbit.exitDate = selectedExitDate;
        rabbit.exitReason = exitReason.name();
        rabbit.exitNotes = notes.isEmpty() ? null : notes;
        rabbit.salePrice = salePrice;

        viewModel.update(rabbit);
        Toast.makeText(this, R.string.exit_recorded, Toast.LENGTH_SHORT).show();
        finish();
    }

    // ==================== TASK 3: Cage Transfer Dialog ====================

    private void showTransferDialog() {
        Rabbit rabbit = viewModel.getRabbitByIdSync(rabbitId);
        if (rabbit == null) return;

        CageDao cageDao = ((CunnisApp) getApplication()).getDatabase().cageDao();
        List<Cage> allCages = cageDao.getAllCagesSync();

        // Filter out current cage from target options
        List<Cage> targetCages = new ArrayList<>();
        for (Cage c : allCages) {
            if (rabbit.cageId == null || c.id != rabbit.cageId) {
                targetCages.add(c);
            }
        }

        if (targetCages.isEmpty()) {
            Toast.makeText(this, R.string.transfer_no_cages, Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transfer_cage, null);

        com.google.android.material.textfield.TextInputEditText edtCurrentCage = dialogView.findViewById(R.id.edtCurrentCage);
        AutoCompleteTextView actTargetCage = dialogView.findViewById(R.id.actTargetCage);
        AutoCompleteTextView actTransferReason = dialogView.findViewById(R.id.actTransferReason);
        com.google.android.material.textfield.TextInputEditText edtTransferNotes = dialogView.findViewById(R.id.edtTransferNotes);

        // Show current cage
        if (rabbit.cageId != null) {
            for (Cage c : allCages) {
                if (c.id == rabbit.cageId) {
                    edtCurrentCage.setText(String.format(Locale.getDefault(), "#%d", c.cageNumber));
                    break;
                }
            }
        } else {
            edtCurrentCage.setText(getString(R.string.rabbit_no_cage));
        }

        // Populate target cage dropdown
        List<String> targetCageLabels = new ArrayList<>();
        int[] targetCageIds = new int[targetCages.size()];
        for (int i = 0; i < targetCages.size(); i++) {
            Cage c = targetCages.get(i);
            targetCageLabels.add(String.format(Locale.getDefault(), "#%d", c.cageNumber));
            targetCageIds[i] = c.id;
        }
        ArrayAdapter<String> cageAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, targetCageLabels);
        actTargetCage.setAdapter(cageAdapter);

        // Populate reason dropdown
        String[] reasons = {
                getString(R.string.movement_weaning),
                getString(R.string.movement_breeding),
                getString(R.string.movement_reorganization),
                getString(R.string.common_notes) // Other
        };
        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, reasons);
        actTransferReason.setAdapter(reasonAdapter);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.movement_title)
                .setView(dialogView)
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                    String targetCageStr = actTargetCage.getText().toString().trim();
                    if (targetCageStr.isEmpty()) {
                        Toast.makeText(this, R.string.transfer_select_cage, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Find target cage ID
                    int targetCageId = -1;
                    for (int i = 0; i < targetCageLabels.size(); i++) {
                        if (targetCageLabels.get(i).equals(targetCageStr)) {
                            targetCageId = targetCageIds[i];
                            break;
                        }
                    }

                    if (targetCageId < 0) return;

                    String reason = actTransferReason.getText().toString().trim();
                    if (reason.isEmpty()) reason = getString(R.string.common_notes);

                    // Confirm transfer
                    new MaterialAlertDialogBuilder(this)
                            .setMessage(String.format(Locale.getDefault(),
                                    getString(R.string.transfer_confirm), getCageNumber(allCages, targetCageId)))
                            .setPositiveButton(R.string.common_yes, (confirmDialog, confirmWhich) -> {
                                viewModel.moveRabbit(rabbitId, rabbit.cageId, targetCageId, reason);
                                Toast.makeText(this, R.string.transfer_success, Toast.LENGTH_SHORT).show();
                                // Reload data by re-observing
                                loadRabbit();
                            })
                            .setNegativeButton(R.string.common_no, null)
                            .show();
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    private int getCageNumber(List<Cage> cages, int cageId) {
        for (Cage c : cages) {
            if (c.id == cageId) return c.cageNumber;
        }
        return cageId;
    }
}
