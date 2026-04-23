package cu.dandroid.cunnis.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.entity.UserProfile;
import cu.dandroid.cunnis.data.local.db.dao.UserProfileDao;
import cu.dandroid.cunnis.databinding.ActivityProfileSetupBinding;
import cu.dandroid.cunnis.util.SharedPreferencesHelper;

public class ProfileSetupActivity extends AppCompatActivity {
    private ActivityProfileSetupBinding binding;
    private UserProfileDao userProfileDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        userProfileDao = ((CunnisApp) getApplication()).getDatabase().userProfileDao();

        // Check if profile exists
        new Thread(() -> {
            if (userProfileDao.profileExists()) {
                runOnUiThread(() -> {
                    UserProfile profile = userProfileDao.getProfileSync();
                    if (profile != null) {
                        binding.edtUsername.setText(profile.username);
                        binding.edtFarmName.setText(profile.farmName);
                        binding.edtEmail.setText(profile.email);
                        binding.edtPhone.setText(profile.phone);
                        binding.edtAddress.setText(profile.address);
                    }
                });
            }
        }).start();

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String username = binding.edtUsername.getText().toString().trim();
        if (username.isEmpty()) {
            binding.tilUsername.setError(getString(R.string.profile_username_required));
            return;
        }
        binding.tilUsername.setError(null);

        UserProfile profile = new UserProfile();
        profile.id = "default";
        profile.username = username;
        profile.farmName = binding.edtFarmName.getText().toString().trim();
        profile.email = binding.edtEmail.getText().toString().trim();
        profile.phone = binding.edtPhone.getText().toString().trim();
        profile.address = binding.edtAddress.getText().toString().trim();
        profile.createdAt = System.currentTimeMillis();
        profile.updatedAt = System.currentTimeMillis();

        new Thread(() -> {
            userProfileDao.insert(profile);
            SharedPreferencesHelper.getInstance(this).setProfileSetupDone(true);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.common_success, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(ProfileSetupActivity.this, MainActivity.class));
                finish();
            });
        }).start();
    }
}
