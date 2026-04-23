package cu.dandroid.cunnis.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.List;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.CageDao;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.databinding.ActivityCageDetailBinding;
import cu.dandroid.cunnis.ui.adapter.RabbitAdapter;
import cu.dandroid.cunnis.viewmodel.RabbitViewModel;

public class CageDetailActivity extends AppCompatActivity implements RabbitAdapter.OnRabbitClickListener {

    public static final String EXTRA_CAGE_ID = "cage_id";

    private ActivityCageDetailBinding binding;
    private RabbitViewModel rabbitViewModel;
    private CageViewModel cageViewModel;
    private RabbitAdapter rabbitAdapter;
    private int cageId;
    private Cage cage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCageDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        cageId = getIntent().getIntExtra(EXTRA_CAGE_ID, -1);
        if (cageId <= 0) {
            finish();
            return;
        }

        cageViewModel = new ViewModelProvider(this).get(cu.dandroid.cunnis.viewmodel.CageViewModel.class);
        rabbitViewModel = new ViewModelProvider(this).get(RabbitViewModel.class);

        // Load cage info
        cageViewModel.getCageById(cageId).observe(this, c -> {
            if (c != null) {
                cage = c;
                binding.toolbar.setTitle(getString(R.string.cage_detail_title, c.cageNumber));
                if (c.notes != null && !c.notes.isEmpty()) {
                    binding.txtNotes.setText(c.notes);
                } else {
                    binding.txtNotes.setText(R.string.common_na);
                }
            }
        });

        // Setup RecyclerView for rabbits
        rabbitAdapter = new RabbitAdapter(this);
        binding.recyclerViewRabbits.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewRabbits.setAdapter(rabbitAdapter);

        // Observe rabbits in cage
        rabbitViewModel.getRabbitsByCage(cageId).observe(this, rabbits -> {
            rabbitAdapter.submitList(rabbits);
            int count = rabbits != null ? rabbits.size() : 0;
            binding.txtRabbitCount.setText(String.valueOf(count));
            if (count == 0) {
                binding.txtEmpty.setVisibility(View.VISIBLE);
                binding.recyclerViewRabbits.setVisibility(View.GONE);
            } else {
                binding.txtEmpty.setVisibility(View.GONE);
                binding.recyclerViewRabbits.setVisibility(View.VISIBLE);
            }
        });

        // FAB to add rabbit
        binding.fabAddRabbit.setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditRabbitActivity.class));
        });

        // Edit cage from menu
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_cage) {
                Intent intent = new Intent(this, AddEditCageActivity.class);
                intent.putExtra(AddEditCageActivity.EXTRA_CAGE_ID, cageId);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    @Override
    public void onRabbitClick(Rabbit rabbit) {
        Intent intent = new Intent(this, RabbitDetailActivity.class);
        intent.putExtra(RabbitDetailActivity.EXTRA_RABBIT_ID, rabbit.id);
        startActivity(intent);
    }
}
