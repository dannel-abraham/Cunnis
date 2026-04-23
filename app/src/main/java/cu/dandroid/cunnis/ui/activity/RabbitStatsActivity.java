package cu.dandroid.cunnis.ui.activity;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.databinding.ActivityRabbitStatsBinding;
import cu.dandroid.cunnis.ui.fragment.StatsRabbitFragment;

public class RabbitStatsActivity extends AppCompatActivity {
    public static final String EXTRA_RABBIT_ID = "rabbit_id";

    private ActivityRabbitStatsBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRabbitStatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        long rabbitId = getIntent().getLongExtra(EXTRA_RABBIT_ID, -1);
        if (rabbitId <= 0) {
            finish();
            return;
        }

        StatsRabbitFragment fragment = StatsRabbitFragment.newInstance(rabbitId);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit();
    }
}
