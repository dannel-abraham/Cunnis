package cu.dandroid.cunnis.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.entity.HealthEvent;
import cu.dandroid.cunnis.databinding.FragmentHealthHistoryBinding;
import cu.dandroid.cunnis.ui.adapter.HealthEventAdapter;
import java.util.List;

public class HealthHistoryFragment extends Fragment {
    private FragmentHealthHistoryBinding binding;
    private HealthEventAdapter adapter;
    private long rabbitId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rabbitId = requireActivity().getIntent().getLongExtra("rabbit_id", -1);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHealthHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new HealthEventAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        if (rabbitId > 0) {
            new Thread(() -> {
                cu.dandroid.cunnis.CunnisApp app = (cu.dandroid.cunnis.CunnisApp) requireActivity().getApplication();
                List<HealthEvent> events = app.getDatabase().healthEventDao()
                        .getHealthEventsByRabbitSync(rabbitId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> adapter.submitList(events));
                }
            }).start();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
