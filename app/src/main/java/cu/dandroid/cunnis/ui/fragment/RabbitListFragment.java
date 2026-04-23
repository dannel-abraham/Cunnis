package cu.dandroid.cunnis.ui.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.model.Gender;
import cu.dandroid.cunnis.databinding.FragmentRabbitListBinding;
import cu.dandroid.cunnis.ui.activity.AddEditRabbitActivity;
import cu.dandroid.cunnis.ui.activity.RabbitDetailActivity;
import cu.dandroid.cunnis.ui.adapter.RabbitAdapter;
import java.util.ArrayList;
import java.util.List;

public class RabbitListFragment extends Fragment implements RabbitAdapter.OnRabbitClickListener {
    private FragmentRabbitListBinding binding;
    private RabbitAdapter adapter;
    private cu.dandroid.cunnis.viewmodel.RabbitViewModel viewModel;

    // Full unfiltered list of active rabbits
    private List<Rabbit> allActiveRabbits = new ArrayList<>();

    // Filter state
    private String currentTextQuery = "";
    private Gender selectedGender = null; // null = All
    private String selectedBreed = null;  // null = no breed filter
    private Integer selectedCageId = null; // null = no cage filter

    // Breed and cage dropdown data
    private List<String> breedNames = new ArrayList<>();
    private List<Cage> cages = new ArrayList<>();
    private boolean filtersVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRabbitListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(cu.dandroid.cunnis.viewmodel.RabbitViewModel.class);

        adapter = new RabbitAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        // Observe active rabbits from ViewModel
        viewModel.getActiveRabbits().observe(getViewLifecycleOwner(), rabbits -> {
            if (rabbits != null) {
                allActiveRabbits = new ArrayList<>(rabbits);
                applyFilters();
            }
        });

        // Text search
        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentTextQuery = s.toString().trim();
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Gender filter chips
        binding.chipGroupGender.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                selectedGender = null;
            } else if (checkedId == R.id.chipMales) {
                selectedGender = Gender.MALE;
            } else if (checkedId == R.id.chipFemales) {
                selectedGender = Gender.FEMALE;
            }
            applyFilters();
        });

        // Breed dropdown
        setupBreedDropdown();

        // Cage dropdown
        setupCageDropdown();

        // Filter toggle button
        binding.btnToggleFilters.setOnClickListener(v -> {
            filtersVisible = !filtersVisible;
            binding.filterBar.setVisibility(filtersVisible ? View.VISIBLE : View.GONE);
            if (filtersVisible) {
                binding.btnToggleFilters.setIconTint(
                        ColorStateList.valueOf(
                                ContextCompat.getColor(requireContext(), R.color.cunnis_control_normal)));
            } else {
                binding.btnToggleFilters.setIconTint(
                        ColorStateList.valueOf(
                                ContextCompat.getColor(requireContext(), R.color.md_theme_light_on_surface_variant)));
            }
        });

        // FAB
        binding.fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddEditRabbitActivity.class));
        });
    }

    private void setupBreedDropdown() {
        new Thread(() -> {
            CunnisApp app = (CunnisApp) requireActivity().getApplication();
            List<String> breeds = app.getDatabase().breedDao().getAllBreedNamesSync();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    breedNames = breeds;
                    List<String> items = new ArrayList<>();
                    items.add(getString(R.string.common_all));
                    items.addAll(breeds);

                    ArrayAdapter<String> breedAdapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            items
                    );
                    binding.actBreed.setAdapter(breedAdapter);

                    binding.actBreed.setOnItemClickListener((parent, view, position, id) -> {
                        if (position == 0) {
                            selectedBreed = null;
                        } else {
                            selectedBreed = items.get(position);
                        }
                        applyFilters();
                    });

                    // Clear breed filter when text is cleared
                    binding.actBreed.setOnDismissListener(() -> {
                        if (binding.actBreed.getText().toString().trim().isEmpty()) {
                            selectedBreed = null;
                            applyFilters();
                        }
                    });
                });
            }
        }).start();
    }

    private void setupCageDropdown() {
        new Thread(() -> {
            CunnisApp app = (CunnisApp) requireActivity().getApplication();
            List<Cage> cageList = app.getDatabase().cageDao().getAllCagesSync();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    cages = cageList;
                    List<String> items = new ArrayList<>();
                    items.add(getString(R.string.common_all));
                    for (Cage cage : cageList) {
                        items.add(String.format(getString(R.string.cage_detail), cage.cageNumber));
                    }

                    ArrayAdapter<String> cageAdapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            items
                    );
                    binding.actCage.setAdapter(cageAdapter);

                    binding.actCage.setOnItemClickListener((parent, view, position, id) -> {
                        if (position == 0) {
                            selectedCageId = null;
                        } else if (position - 1 < cageList.size()) {
                            selectedCageId = cageList.get(position - 1).id;
                        }
                        applyFilters();
                    });

                    // Clear cage filter when text is cleared
                    binding.actCage.setOnDismissListener(() -> {
                        if (binding.actCage.getText().toString().trim().isEmpty()) {
                            selectedCageId = null;
                            applyFilters();
                        }
                    });
                });
            }
        }).start();
    }

    private void applyFilters() {
        List<Rabbit> filtered = new ArrayList<>(allActiveRabbits);

        // 1. Text search filter
        if (currentTextQuery != null && !currentTextQuery.isEmpty()) {
            String query = currentTextQuery.toLowerCase();
            List<Rabbit> textFiltered = new ArrayList<>();
            for (Rabbit rabbit : filtered) {
                boolean matchName = rabbit.name != null && rabbit.name.toLowerCase().contains(query);
                boolean matchId = rabbit.identifier != null && rabbit.identifier.toLowerCase().contains(query);
                if (matchName || matchId) {
                    textFiltered.add(rabbit);
                }
            }
            filtered = textFiltered;
        }

        // 2. Gender filter
        if (selectedGender != null) {
            List<Rabbit> genderFiltered = new ArrayList<>();
            for (Rabbit rabbit : filtered) {
                if (rabbit.gender == selectedGender) {
                    genderFiltered.add(rabbit);
                }
            }
            filtered = genderFiltered;
        }

        // 3. Breed filter
        if (selectedBreed != null && !selectedBreed.isEmpty()) {
            List<Rabbit> breedFiltered = new ArrayList<>();
            for (Rabbit rabbit : filtered) {
                if (selectedBreed.equalsIgnoreCase(rabbit.breed)) {
                    breedFiltered.add(rabbit);
                }
            }
            filtered = breedFiltered;
        }

        // 4. Cage filter
        if (selectedCageId != null) {
            List<Rabbit> cageFiltered = new ArrayList<>();
            for (Rabbit rabbit : filtered) {
                if (selectedCageId.equals(rabbit.cageId)) {
                    cageFiltered.add(rabbit);
                }
            }
            filtered = cageFiltered;
        }

        // Update adapter
        adapter.submitList(filtered);

        // Show empty state
        boolean hasActiveFilters = currentTextQuery != null && !currentTextQuery.isEmpty()
                || selectedGender != null
                || (selectedBreed != null && !selectedBreed.isEmpty())
                || selectedCageId != null;

        if (filtered.isEmpty()) {
            binding.txtEmpty.setVisibility(View.VISIBLE);
            binding.txtEmpty.setText(hasActiveFilters
                    ? getString(R.string.filter_no_results)
                    : getString(R.string.common_no_data));
        } else {
            binding.txtEmpty.setVisibility(View.GONE);
        }
        binding.recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onRabbitClick(Rabbit rabbit) {
        Intent intent = new Intent(requireContext(), RabbitDetailActivity.class);
        intent.putExtra(RabbitDetailActivity.EXTRA_RABBIT_ID, rabbit.id);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
