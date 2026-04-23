package cu.dandroid.cunnis.ui.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.model.Gender;
import cu.dandroid.cunnis.databinding.ActivityAddEditRabbitBinding;
import cu.dandroid.cunnis.util.Constants;
import cu.dandroid.cunnis.util.DateUtils;
import cu.dandroid.cunnis.util.IdentifierGenerator;
import cu.dandroid.cunnis.util.ImageUtils;
import cu.dandroid.cunnis.viewmodel.CageViewModel;
import cu.dandroid.cunnis.viewmodel.RabbitViewModel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEditRabbitActivity extends AppCompatActivity {
    public static final String EXTRA_RABBIT_ID = "rabbit_id";
    private ActivityAddEditRabbitBinding binding;
    private RabbitViewModel rabbitViewModel;
    private CageViewModel cageViewModel;
    private long rabbitId = -1;
    private boolean isEditing = false;
    private byte[] photoData = null;
    private Uri photoUri = null;
    private Long selectedBirthDateMillis = null; // millis from DatePicker

    // Parent tracking data - populated from DB on background thread
    private List<Rabbit> activeMales = new ArrayList<>();
    private List<Rabbit> activeFemales = new ArrayList<>();
    private Rabbit currentRabbit = null; // for editing context

    // Cage data kept in sync with the dropdown so we can resolve cageId on save
    private final List<Cage> cageList = new ArrayList<>();

    // Modern ActivityResultLaunchers
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                if (bitmap != null) {
                    photoData = ImageUtils.compressBitmap(bitmap);
                    binding.imgPhoto.setImageBitmap(ImageUtils.bytesToBitmap(photoData));
                }
            }
        });

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), result.getData().getData());
                    photoData = ImageUtils.compressBitmap(bitmap);
                    binding.imgPhoto.setImageBitmap(ImageUtils.bytesToBitmap(photoData));
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditRabbitBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        rabbitViewModel = new ViewModelProvider(this).get(RabbitViewModel.class);
        cageViewModel = new ViewModelProvider(this).get(CageViewModel.class);

        // Setup gender dropdown
        String[] genders = {getString(R.string.rabbit_male), getString(R.string.rabbit_female)};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, genders);
        binding.actGender.setAdapter(genderAdapter);

        // Setup cage dropdown — keep cageList in sync
        cageViewModel.getAllCages().observe(this, cages -> {
            cageList.clear();
            cageList.addAll(cages);

            List<String> cageLabels = new ArrayList<>();
            cageLabels.add(getString(R.string.rabbit_no_cage));
            for (Cage c : cages) {
                cageLabels.add(String.format(Locale.getDefault(), "#%d", c.cageNumber));
            }
            ArrayAdapter<String> cageAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, cageLabels);
            binding.actCage.setAdapter(cageAdapter);

            // If editing, restore cage selection now that the adapter is populated
            if (isEditing && currentRabbit != null) {
                restoreCageSelection();
            }
        });

        // Setup breed autocomplete
        rabbitViewModel.getAllBreedNames().observe(this, breeds -> {
            if (breeds != null && !breeds.isEmpty()) {
                ArrayAdapter<String> breedAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, new ArrayList<>(breeds));
                binding.actBreed.setAdapter(breedAdapter);
            }
        });

        // Load active males/females for sire/dam dropdowns (on background thread)
        loadParentRabbits();

        // Born in farm checkbox
        binding.chkBornInFarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.tilBirthDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            binding.tilSire.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            binding.tilDam.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                selectedBirthDateMillis = null;
                binding.edtBirthDate.setText("");
            }
        });

        // --- FIX: Make birth date field clickable with DatePicker ---
        binding.edtBirthDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (selectedBirthDateMillis != null && selectedBirthDateMillis > 0) {
                cal.setTimeInMillis(selectedBirthDateMillis);
            }
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar dateCal = Calendar.getInstance();
                dateCal.set(year, month, dayOfMonth);
                selectedBirthDateMillis = dateCal.getTimeInMillis();
                binding.edtBirthDate.setText(DateUtils.formatDate(selectedBirthDateMillis));
                binding.edtBirthDate.setHint(R.string.rabbit_birth_date_hint);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Check if editing
        if (getIntent().hasExtra(EXTRA_RABBIT_ID)) {
            rabbitId = getIntent().getLongExtra(EXTRA_RABBIT_ID, -1);
            isEditing = rabbitId > 0;
            if (isEditing) {
                binding.toolbar.setTitle(getString(R.string.rabbit_edit));
                loadRabbit();
            }
        } else {
            // Auto-generate identifier
            String identifier = IdentifierGenerator.generateNext(
                    ((cu.dandroid.cunnis.CunnisApp)getApplication()).getDatabase().rabbitDao());
            binding.edtIdentifier.setText(identifier);
        }

        // Photo button
        binding.btnPhoto.setOnClickListener(v -> showPhotoOptions());

        binding.btnSave.setOnClickListener(v -> saveRabbit());
    }

    /**
     * Load active male and female rabbits on a background thread to populate
     * the sire (father) and dam (mother) dropdowns.
     */
    private void loadParentRabbits() {
        new Thread(() -> {
            List<Rabbit> allActive = ((CunnisApp) getApplication())
                    .getDatabase().rabbitDao().getActiveRabbitsSync();
            List<Rabbit> males = new ArrayList<>();
            List<Rabbit> females = new ArrayList<>();
            for (Rabbit r : allActive) {
                if (r.gender == Gender.MALE) {
                    males.add(r);
                } else if (r.gender == Gender.FEMALE) {
                    females.add(r);
                }
            }
            activeMales = males;
            activeFemales = females;

            // Populate dropdowns on UI thread
            runOnUiThread(() -> setupParentDropdowns());
        }).start();
    }

    /**
     * Set up the sire and dam AutoCompleteTextView adapters with loaded data.
     * Each item shows "Name (ID)" format for easy identification.
     */
    private void setupParentDropdowns() {
        // Sire (male) dropdown
        List<String> sireLabels = new ArrayList<>();
        sireLabels.add(getString(R.string.rabbit_sire_placeholder));
        for (Rabbit r : activeMales) {
            sireLabels.add(String.format(Locale.getDefault(),
                    getString(R.string.rabbit_parent_format), r.name, r.identifier));
        }
        ArrayAdapter<String> sireAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, sireLabels);
        binding.actSire.setAdapter(sireAdapter);

        // Dam (female) dropdown
        List<String> damLabels = new ArrayList<>();
        damLabels.add(getString(R.string.rabbit_dam_placeholder));
        for (Rabbit r : activeFemales) {
            damLabels.add(String.format(Locale.getDefault(),
                    getString(R.string.rabbit_parent_format), r.name, r.identifier));
        }
        ArrayAdapter<String> damAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, damLabels);
        binding.actDam.setAdapter(damAdapter);

        // If editing, restore sire/dam after adapters are set
        if (isEditing && currentRabbit != null) {
            restoreParentSelections();
        }
    }

    /**
     * Restore sire and dam dropdown text when editing an existing rabbit.
     */
    private void restoreParentSelections() {
        if (currentRabbit == null) return;

        // Restore sire
        if (currentRabbit.sireId != null && currentRabbit.sireId > 0) {
            for (Rabbit r : activeMales) {
                if (r.id == currentRabbit.sireId) {
                    String label = String.format(Locale.getDefault(),
                            getString(R.string.rabbit_parent_format), r.name, r.identifier);
                    binding.actSire.setText(label, false);
                    break;
                }
            }
        }

        // Restore dam
        if (currentRabbit.damId != null && currentRabbit.damId > 0) {
            for (Rabbit r : activeFemales) {
                if (r.id == currentRabbit.damId) {
                    String label = String.format(Locale.getDefault(),
                            getString(R.string.rabbit_parent_format), r.name, r.identifier);
                    binding.actDam.setText(label, false);
                    break;
                }
            }
        }
    }

    /**
     * Restore cage dropdown selection when editing an existing rabbit.
     * The adapter stores labels like "#1", "#2". We match by cage id.
     */
    private void restoreCageSelection() {
        if (currentRabbit == null || currentRabbit.cageId == null) return;
        for (Cage c : cageList) {
            if (c.id == currentRabbit.cageId) {
                String label = String.format(Locale.getDefault(), "#%d", c.cageNumber);
                binding.actCage.setText(label, false);
                break;
            }
        }
    }

    private void loadRabbit() {
        rabbitViewModel.getRabbitById(rabbitId).observe(this, rabbit -> {
            if (rabbit == null) return;
            currentRabbit = rabbit;
            binding.edtName.setText(rabbit.name);
            binding.edtIdentifier.setText(rabbit.identifier);
            binding.edtNotes.setText(rabbit.notes != null ? rabbit.notes : "");
            binding.actBreed.setText(rabbit.breed != null ? rabbit.breed : "", false);
            binding.chkBornInFarm.setChecked(rabbit.birthDate != null);
            if (rabbit.birthDate != null) {
                selectedBirthDateMillis = rabbit.birthDate;
                binding.edtBirthDate.setText(DateUtils.formatDate(rabbit.birthDate));
            }
            if (rabbit.gender == Gender.MALE) {
                binding.actGender.setText(getString(R.string.rabbit_male), false);
            } else if (rabbit.gender == Gender.FEMALE) {
                binding.actGender.setText(getString(R.string.rabbit_female), false);
            }
            if (rabbit.profilePhoto != null) {
                photoData = rabbit.profilePhoto;
                binding.imgPhoto.setImageBitmap(ImageUtils.bytesToBitmap(photoData));
            }

            // Restore cage selection (adapter may already be loaded)
            if (!cageList.isEmpty()) {
                restoreCageSelection();
            }

            // Restore sire/dam selections (only if parent data already loaded)
            if (!activeMales.isEmpty() || !activeFemales.isEmpty()) {
                restoreParentSelections();
            }
        });
    }

    private void showPhotoOptions() {
        String[] options = {getString(R.string.rabbit_take_photo), getString(R.string.rabbit_select_photo)};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.rabbit_photo))
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

    private void saveRabbit() {
        String name = binding.edtName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.tilName.setError(getString(R.string.rabbit_name_required));
            return;
        }
        binding.tilName.setError(null);

        String identifier = binding.edtIdentifier.getText().toString().trim();
        String genderStr = binding.actGender.getText().toString().trim();
        String breed = binding.actBreed.getText().toString().trim();
        String notes = binding.edtNotes.getText().toString().trim();

        Gender gender = Gender.UNKNOWN;
        if (genderStr.equals(getString(R.string.rabbit_male))) gender = Gender.MALE;
        else if (genderStr.equals(getString(R.string.rabbit_female))) gender = Gender.FEMALE;

        // Resolve sire ID from dropdown text
        Long sireId = resolveParentId(binding.actSire.getText().toString().trim(), activeMales);

        // Resolve dam ID from dropdown text
        Long damId = resolveParentId(binding.actDam.getText().toString().trim(), activeFemales);

        // --- FIX: Resolve cageId from dropdown ---
        Integer cageId = resolveCageId(binding.actCage.getText().toString().trim());

        if (isEditing) {
            Rabbit rabbit = rabbitViewModel.getRabbitByIdSync(rabbitId);
            if (rabbit != null) {
                rabbit.name = name;
                rabbit.gender = gender;
                rabbit.breed = breed.isEmpty() ? null : breed;
                rabbit.notes = notes.isEmpty() ? null : notes;
                rabbit.profilePhoto = photoData;
                rabbit.sireId = sireId;
                rabbit.damId = damId;
                rabbit.cageId = cageId;
                // --- FIX: Save birth date from DatePicker ---
                rabbit.birthDate = selectedBirthDateMillis;
                rabbitViewModel.update(rabbit);
                Toast.makeText(this, R.string.common_success, Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Rabbit rabbit = new Rabbit();
            rabbit.identifier = identifier;
            rabbit.name = name;
            rabbit.gender = gender;
            rabbit.breed = breed.isEmpty() ? null : breed;
            rabbit.notes = notes.isEmpty() ? null : notes;
            rabbit.profilePhoto = photoData;
            rabbit.status = cu.dandroid.cunnis.data.model.RabbitStatus.ACTIVE;
            rabbit.sireId = sireId;
            rabbit.damId = damId;
            rabbit.cageId = cageId;
            // --- FIX: Save birth date from DatePicker ---
            rabbit.birthDate = selectedBirthDateMillis;
            rabbitViewModel.insert(rabbit);
            Toast.makeText(this, R.string.common_success, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Resolve a cage ID from the dropdown display text.
     * The adapter stores labels like "#1", "#2". We parse the number and match
     * against the cached cageList.
     * Returns null if the text is the placeholder ("No cage assigned") or unrecognised.
     */
    private Integer resolveCageId(String displayText) {
        if (displayText == null || displayText.isEmpty()) return null;

        // Check if it's the placeholder
        if (displayText.equals(getString(R.string.rabbit_no_cage))) {
            return null;
        }

        // Parse "#N" format
        String trimmed = displayText.trim();
        if (trimmed.startsWith("#")) {
            try {
                int cageNumber = Integer.parseInt(trimmed.substring(1));
                for (Cage c : cageList) {
                    if (c.cageNumber == cageNumber) {
                        return c.id;
                    }
                }
            } catch (NumberFormatException ignored) {
                // Not a valid cage number
            }
        }

        return null;
    }

    /**
     * Resolve a parent (sire/dam) rabbit ID from the dropdown display text.
     * The text format is "Name (ID)" - we match by the identifier part in parentheses.
     * Returns null if no match found or text is a placeholder.
     */
    private Long resolveParentId(String displayText, List<Rabbit> candidates) {
        if (displayText == null || displayText.isEmpty()) return null;

        // Check if it's a placeholder
        String sirePlaceholder = getString(R.string.rabbit_sire_placeholder);
        String damPlaceholder = getString(R.string.rabbit_dam_placeholder);
        if (displayText.equals(sirePlaceholder) || displayText.equals(damPlaceholder)) {
            return null;
        }

        // Try to find by matching the display text format "Name (identifier)"
        for (Rabbit r : candidates) {
            String expectedLabel = String.format(Locale.getDefault(),
                    getString(R.string.rabbit_parent_format), r.name, r.identifier);
            if (expectedLabel.equals(displayText)) {
                return r.id;
            }
        }

        return null;
    }
}
