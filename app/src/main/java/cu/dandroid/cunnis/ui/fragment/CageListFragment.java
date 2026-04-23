package cu.dandroid.cunnis.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.databinding.FragmentCageListBinding;
import cu.dandroid.cunnis.ui.activity.AddEditCageActivity;
import cu.dandroid.cunnis.ui.activity.CageDetailActivity;
import cu.dandroid.cunnis.ui.adapter.CageAdapter;

public class CageListFragment extends Fragment implements CageAdapter.OnCageClickListener {
    private FragmentCageListBinding binding;
    private CageAdapter adapter;
    private cu.dandroid.cunnis.viewmodel.CageViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCageListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(cu.dandroid.cunnis.viewmodel.CageViewModel.class);

        adapter = new CageAdapter(this);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerView.setAdapter(adapter);

        viewModel.getAllCages().observe(getViewLifecycleOwner(), cages -> {
            adapter.submitList(cages);
            binding.txtEmpty.setVisibility(cages == null || cages.isEmpty() ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(cages != null && !cages.isEmpty() ? View.VISIBLE : View.GONE);
        });

        binding.fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddEditCageActivity.class));
        });
    }

    @Override
    public void onCageClick(Cage cage) {
        Intent intent = new Intent(requireContext(), CageDetailActivity.class);
        intent.putExtra(CageDetailActivity.EXTRA_CAGE_ID, cage.id);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
