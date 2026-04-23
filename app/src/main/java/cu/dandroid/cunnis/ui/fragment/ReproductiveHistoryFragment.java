package cu.dandroid.cunnis.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.databinding.FragmentReproductiveHistoryBinding;
import cu.dandroid.cunnis.ui.adapter.ReproductivePagerAdapter;

public class ReproductiveHistoryFragment extends Fragment {
    private FragmentReproductiveHistoryBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReproductiveHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentActivity activity = requireActivity();

        // Get rabbitId from the parent activity's intent
        long rabbitId = activity.getIntent().getLongExtra("rabbit_id", -1);

        ReproductivePagerAdapter adapter = new ReproductivePagerAdapter(activity, rabbitId);
        binding.viewPager.setAdapter(adapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0: tab.setText(R.string.estrus_title); break;
                        case 1: tab.setText(R.string.mating_title); break;
                        case 2: tab.setText(R.string.parturition_title); break;
                    }
                }).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
