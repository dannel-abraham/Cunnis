package cu.dandroid.cunnis.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.UserProfileDao;
import cu.dandroid.cunnis.data.local.db.entity.UserProfile;
import cu.dandroid.cunnis.databinding.ActivitySettingsBinding;
import cu.dandroid.cunnis.util.Constants;
import cu.dandroid.cunnis.util.SharedPreferencesHelper;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferencesHelper prefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        prefsHelper = SharedPreferencesHelper.getInstance(this);

        loadUserProfile();
        setupThemeSelector();
        setupLanguageSelector();
        setupNotificationToggle();
        setupDataButtons();
        setupAboutSection();
    }

    private void loadUserProfile() {
        UserProfileDao dao = ((CunnisApp) getApplication()).getDatabase().userProfileDao();
        new Thread(() -> {
            UserProfile profile = dao.getProfileSync();
            if (profile != null) {
                runOnUiThread(() -> {
                    binding.txtUsername.setText(
                        profile.username != null ? profile.username : getString(R.string.common_na));
                    binding.txtFarmName.setText(
                        profile.farmName != null ? profile.farmName : getString(R.string.common_na));
                    binding.txtEmail.setText(
                        profile.email != null ? profile.email : getString(R.string.common_na));
                });
            } else {
                runOnUiThread(() -> {
                    binding.txtUsername.setText(R.string.common_na);
                    binding.txtFarmName.setText(R.string.common_na);
                    binding.txtEmail.setText(R.string.common_na);
                });
            }
        }).start();

        binding.btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileSetupActivity.class));
        });
    }

    private void setupThemeSelector() {
        String currentTheme = prefsHelper.getTheme();
        if (Constants.THEME_LIGHT.equals(currentTheme)) {
            binding.radioGroupTheme.check(R.id.radioLight);
        } else if (Constants.THEME_DARK.equals(currentTheme)) {
            binding.radioGroupTheme.check(R.id.radioDark);
        } else {
            binding.radioGroupTheme.check(R.id.radioSystem);
        }

        binding.radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            String theme;
            if (checkedId == R.id.radioLight) {
                theme = Constants.THEME_LIGHT;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.radioDark) {
                theme = Constants.THEME_DARK;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                theme = Constants.THEME_SYSTEM;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
            prefsHelper.setTheme(theme);
            Toast.makeText(this, R.string.settings_theme_applied, Toast.LENGTH_SHORT).show();
            recreate();
        });
    }

    private void setupLanguageSelector() {
        String currentLang = prefsHelper.getLanguage();
        if (currentLang == null || currentLang.isEmpty() || "en".equals(currentLang)) {
            binding.radioGroupLanguage.check(R.id.radioLangEn);
        } else if ("es".equals(currentLang)) {
            binding.radioGroupLanguage.check(R.id.radioLangEs);
        } else if ("ru".equals(currentLang)) {
            binding.radioGroupLanguage.check(R.id.radioLangRu);
        }

        binding.radioGroupLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String lang;
            if (checkedId == R.id.radioLangEs) {
                lang = "es";
            } else if (checkedId == R.id.radioLangRu) {
                lang = "ru";
            } else {
                lang = "en";
            }
            prefsHelper.setLanguage(lang);

            java.util.Locale locale = new java.util.Locale(lang);
            java.util.Locale.setDefault(locale);
            android.content.res.Configuration config = new android.content.res.Configuration(
                getResources().getConfiguration());
            config.setLocale(locale);
            android.content.Context context = createConfigurationContext(config);
            getResources().updateConfiguration(context.getResources().getConfiguration(),
                context.getResources().getDisplayMetrics());

            Toast.makeText(this, R.string.settings_language_applied, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupNotificationToggle() {
        binding.switchNotifications.setChecked(prefsHelper.isNotificationsEnabled());
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefsHelper.setNotificationsEnabled(isChecked);
        });
    }

    private void setupDataButtons() {
        binding.btnExportData.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExportImportActivity.class);
            intent.putExtra(ExportImportActivity.EXTRA_MODE, "export");
            startActivity(intent);
        });

        binding.btnImportData.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExportImportActivity.class);
            intent.putExtra(ExportImportActivity.EXTRA_MODE, "import");
            startActivity(intent);
        });
    }

    private void setupAboutSection() {
        binding.txtAuthor.setText(String.format(getString(R.string.about_author), getString(R.string.app_author)));
        binding.txtDedication.setText(String.format(getString(R.string.settings_dedication),
            getString(R.string.app_dedication)));
        binding.txtTelegram.setText(getString(R.string.app_telegram));
        binding.txtContactEmail.setText(getString(R.string.app_email));
    }
}
