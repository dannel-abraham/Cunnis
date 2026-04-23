package cu.dandroid.cunnis.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.data.local.db.dao.RabbitDao;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.model.Gender;
import cu.dandroid.cunnis.databinding.FragmentRabbitProfileBinding;
import cu.dandroid.cunnis.util.DateUtils;
import cu.dandroid.cunnis.viewmodel.RabbitViewModel;

public class RabbitProfileFragment extends Fragment {
    private FragmentRabbitProfileBinding binding;
    private RabbitViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRabbitProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(RabbitViewModel.class);

        long rabbitId = requireActivity().getIntent().getLongExtra("rabbit_id", -1);
        if (rabbitId > 0) {
            viewModel.getRabbitById(rabbitId).observe(getViewLifecycleOwner(), rabbit -> {
                if (rabbit == null) return;

                // Breed
                View rowBreed = binding.rowBreed;
                ((android.widget.TextView) rowBreed.findViewById(R.id.txtLabel)).setText(getString(R.string.rabbit_breed));
                ((android.widget.TextView) rowBreed.findViewById(R.id.txtValue))
                    .setText(rabbit.breed != null ? rabbit.breed : getString(R.string.common_na));

                // Age
                View rowAge = binding.rowAge;
                ((android.widget.TextView) rowAge.findViewById(R.id.txtLabel)).setText(getString(R.string.rabbit_age));
                String age = rabbit.birthDate != null ? DateUtils.calculateAge(rabbit.birthDate) : getString(R.string.rabbit_unknown_age);
                ((android.widget.TextView) rowAge.findViewById(R.id.txtValue)).setText(age);

                // Birth date
                View rowBirth = binding.rowBirthDate;
                ((android.widget.TextView) rowBirth.findViewById(R.id.txtLabel)).setText(getString(R.string.rabbit_birth_date));
                ((android.widget.TextView) rowBirth.findViewById(R.id.txtValue))
                    .setText(rabbit.birthDate != null ? DateUtils.formatDate(rabbit.birthDate) : getString(R.string.common_na));

                // Weight
                View rowWeight = binding.rowWeight;
                ((android.widget.TextView) rowWeight.findViewById(R.id.txtLabel)).setText(getString(R.string.rabbit_weight));
                ((android.widget.TextView) rowWeight.findViewById(R.id.txtValue))
                    .setText(rabbit.currentWeight > 0 ? String.format("%.1f lb", rabbit.currentWeight) : getString(R.string.common_na));

                // Cage
                View rowCage = binding.rowCage;
                ((android.widget.TextView) rowCage.findViewById(R.id.txtLabel)).setText(getString(R.string.rabbit_cage));
                ((android.widget.TextView) rowCage.findViewById(R.id.txtValue))
                    .setText(rabbit.cageId != null ? String.format("#%d", rabbit.cageId) : getString(R.string.rabbit_no_cage));

                // Notes
                binding.txtNotes.setText(rabbit.notes != null ? rabbit.notes : "");

                // Sire & Dam (parent) rows
                RabbitDao rabbitDao = ((CunnisApp) requireActivity().getApplication()).getDatabase().rabbitDao();
                final long sireId = rabbit.sireId != null ? rabbit.sireId : 0;
                final long damId = rabbit.damId != null ? rabbit.damId : 0;
                if (sireId > 0 || damId > 0) {
                    new Thread(() -> {
                        Rabbit sire = sireId > 0 ? rabbitDao.getRabbitByIdSync(sireId) : null;
                        Rabbit dam = damId > 0 ? rabbitDao.getRabbitByIdSync(damId) : null;
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // Sire row
                                View rowSire = binding.rowSire;
                                ((android.widget.TextView) rowSire.findViewById(R.id.txtLabel))
                                    .setText(getString(R.string.rabbit_sire_label));
                                if (sire != null) {
                                    rowSire.setVisibility(View.VISIBLE);
                                    ((android.widget.TextView) rowSire.findViewById(R.id.txtValue))
                                        .setText(String.format("%s (%s)", sire.name, sire.identifier));
                                } else {
                                    rowSire.setVisibility(View.GONE);
                                }
                                // Dam row
                                View rowDam = binding.rowDam;
                                ((android.widget.TextView) rowDam.findViewById(R.id.txtLabel))
                                    .setText(getString(R.string.rabbit_dam_label));
                                if (dam != null) {
                                    rowDam.setVisibility(View.VISIBLE);
                                    ((android.widget.TextView) rowDam.findViewById(R.id.txtValue))
                                        .setText(String.format("%s (%s)", dam.name, dam.identifier));
                                } else {
                                    rowDam.setVisibility(View.GONE);
                                }
                            });
                        }
                    }).start();
                } else {
                    binding.rowSire.setVisibility(View.GONE);
                    binding.rowDam.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
