package cu.dandroid.cunnis.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.UserProfileDao;
import cu.dandroid.cunnis.data.local.db.entity.UserProfile;
import cu.dandroid.cunnis.databinding.ActivityMainBinding;
import cu.dandroid.cunnis.ui.fragment.*;
import cu.dandroid.cunnis.CunnisApp;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private ActivityMainBinding binding;
    private ActionBarDrawerToggle toggle;
    private FragmentManager fm;
    private UserProfileDao userProfileDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        userProfileDao = ((CunnisApp) getApplication()).getDatabase().userProfileDao();
        fm = getSupportFragmentManager();

        // Setup drawer
        toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar,
                R.string.drawer_open, R.string.drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Load user info in header
        loadUserInfo();

        // Setup navigation
        binding.navView.setNavigationItemSelectedListener(this);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { showFragment(new HomeFragment()); return true; }
            if (id == R.id.nav_cages) { showFragment(new CageListFragment()); return true; }
            if (id == R.id.nav_rabbits) { showFragment(new RabbitListFragment()); return true; }
            if (id == R.id.nav_stats) { showFragment(new StatsGeneralFragment()); return true; }
            return false;
        });

        // Default fragment
        if (savedInstanceState == null) {
            showFragment(new HomeFragment());
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void loadUserInfo() {
        new Thread(() -> {
            UserProfile profile = userProfileDao.getProfileSync();
            if (profile != null) {
                runOnUiThread(() -> {
                    binding.navView.getHeaderView(0).findViewById(R.id.txtNavFarmName)
                        .setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void showFragment(Fragment fragment) {
        fm.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit();
        // Close drawer if open
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_cemental) {
            showFragment(new CementalFragment());
        } else if (id == R.id.nav_expenses) {
            showFragment(new ExpensesFragment());
        } else if (id == R.id.nav_alerts) {
            showFragment(new AlertsFragment());
        } else if (id == R.id.nav_feeding) {
            showFragment(new FeedingListFragment());
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_export_import) {
            startActivity(new Intent(this, ExportImportActivity.class));
        } else if (id == R.id.nav_about) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(
                    getString(R.string.app_name) + "\n" +
                    getString(R.string.about_version, getString(R.string.app_version)) + "\n" +
                    getString(R.string.about_author, getString(R.string.app_author)) + "\n\n" +
                    getString(R.string.app_dedication) + "\n\n" +
                    getString(R.string.app_telegram) + "\n" +
                    getString(R.string.app_email)
                )
                .setPositiveButton(R.string.common_ok, null)
                .show();
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
