package cu.dandroid.cunnis.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.MatingRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.RabbitDao;
import cu.dandroid.cunnis.data.local.db.entity.MatingRecord;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.model.Gender;
import cu.dandroid.cunnis.data.model.RabbitStatus;
import cu.dandroid.cunnis.databinding.FragmentCementalBinding;
import cu.dandroid.cunnis.util.DateUtils;
import cu.dandroid.cunnis.util.ImageUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CementalFragment extends Fragment {

    private FragmentCementalBinding binding;
    private RabbitDao rabbitDao;
    private MatingRecordDao matingRecordDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCementalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rabbitDao = ((CunnisApp) requireActivity().getApplication()).getDatabase().rabbitDao();
        matingRecordDao = ((CunnisApp) requireActivity().getApplication()).getDatabase().matingRecordDao();

        loadCemental();

        binding.btnSetNew.setOnClickListener(v -> showSelectMaleDialog());

        binding.btnRemove.setOnClickListener(v -> showRemoveConfirmation());
    }

    private void loadCemental() {
        new Thread(() -> {
            Rabbit cemental = rabbitDao.getCementalSync();

            if (cemental != null) {
                // Load mating stats
                List<MatingRecord> matings = matingRecordDao.getMatingRecordsByBuckSync(cemental.id);
                int totalMatings = matings.size();
                int successfulMatings = 0;
                for (MatingRecord m : matings) {
                    if (m.isEffective) successfulMatings++;
                }

                // Load offspring count
                List<Rabbit> offspring = rabbitDao.getOffspringBySireSync(cemental.id);
                int totalOffspring = offspring != null ? offspring.size() : 0;

                // Recent matings (last 5)
                List<MatingRecord> recentMatings = new ArrayList<>(matings);
                if (recentMatings.size() > 5) {
                    recentMatings = recentMatings.subList(0, 5);
                }

                requireActivity().runOnUiThread(() -> {
                    showCementalInfo(cemental, totalMatings, successfulMatings, totalOffspring, recentMatings);
                });
            } else {
                requireActivity().runOnUiThread(() -> showEmptyState());
            }
        }).start();
    }

    private void showCementalInfo(Rabbit cemental, int totalMatings, int successfulMatings,
                                   int totalOffspring, List<MatingRecord> recentMatings) {
        binding.cardCemental.setVisibility(View.VISIBLE);
        binding.cardEmpty.setVisibility(View.GONE);
        binding.btnRemove.setVisibility(View.VISIBLE);
        binding.cardStats.setVisibility(View.VISIBLE);

        binding.txtCementalName.setText(cemental.name);
        binding.txtCementalIdentifier.setText(cemental.identifier);

        if (cemental.breed != null && !cemental.breed.isEmpty()) {
            binding.chipBreed.setText(cemental.breed);
            binding.chipBreed.setVisibility(View.VISIBLE);
        } else {
            binding.chipBreed.setVisibility(View.GONE);
        }

        if (cemental.birthDate != null && cemental.birthDate > 0) {
            binding.txtCementalAge.setText(DateUtils.calculateAge(cemental.birthDate));
        } else {
            binding.txtCementalAge.setText(R.string.rabbit_unknown_age);
        }

        if (cemental.profilePhoto != null) {
            binding.imgCemental.setImageBitmap(ImageUtils.bytesToBitmap(cemental.profilePhoto));
        }

        binding.txtTotalMatings.setText(String.valueOf(totalMatings));
        binding.txtSuccessfulMatings.setText(String.valueOf(successfulMatings));
        binding.txtTotalOffspring.setText(String.valueOf(totalOffspring));

        // Show recent matings
        if (recentMatings != null && !recentMatings.isEmpty()) {
            binding.txtRecentTitle.setVisibility(View.VISIBLE);
            binding.cardRecentMatings.setVisibility(View.VISIBLE);
            binding.layoutMatings.removeAllViews();

            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (MatingRecord m : recentMatings) {
                View matingItem = inflater.inflate(R.layout.item_info_row, binding.layoutMatings, false);
                TextView txtLabel = matingItem.findViewById(R.id.txtLabel);
                TextView txtValue = matingItem.findViewById(R.id.txtValue);
                txtLabel.setText(DateUtils.formatDate(m.matingDate));
                String doeName = getDoeName(m.doeId);
                txtValue.setText(doeName + (m.isEffective ? " ✓" : ""));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.margin_small));
                matingItem.setLayoutParams(params);
                binding.layoutMatings.addView(matingItem);
            }
        } else {
            binding.txtRecentTitle.setVisibility(View.GONE);
            binding.cardRecentMatings.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        binding.cardCemental.setVisibility(View.GONE);
        binding.cardEmpty.setVisibility(View.VISIBLE);
        binding.btnRemove.setVisibility(View.GONE);
        binding.cardStats.setVisibility(View.GONE);
        binding.txtRecentTitle.setVisibility(View.GONE);
        binding.cardRecentMatings.setVisibility(View.GONE);
    }

    private String getDoeName(long doeId) {
        Rabbit doe = rabbitDao.getRabbitByIdSync(doeId);
        return doe != null ? doe.name : getString(R.string.common_na);
    }

    private void showSelectMaleDialog() {
        new Thread(() -> {
            List<Rabbit> males = rabbitDao.getActiveRabbitsSync();
            List<Rabbit> activeMales = new ArrayList<>();
            for (Rabbit r : males) {
                if (r.gender == Gender.MALE && !r.isCemental) {
                    activeMales.add(r);
                }
            }

            requireActivity().runOnUiThread(() -> {
                if (activeMales.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.cemental_no_male, Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] names = new String[activeMales.size()];
                for (int i = 0; i < activeMales.size(); i++) {
                    Rabbit r = activeMales.get(i);
                    names[i] = r.name + " (" + r.identifier + ")";
                }

                new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.cemental_select_male)
                    .setItems(names, (dialog, which) -> {
                        Rabbit selected = activeMales.get(which);
                        setCemental(selected);
                    })
                    .setNegativeButton(R.string.common_cancel, null)
                    .show();
            });
        }).start();
    }

    private void setCemental(Rabbit rabbit) {
        new Thread(() -> {
            rabbitDao.clearCemental();
            rabbit.isCemental = true;
            rabbitDao.update(rabbit);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), R.string.common_success, Toast.LENGTH_SHORT).show();
                loadCemental();
            });
        }).start();
    }

    private void showRemoveConfirmation() {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.cemental_remove)
            .setMessage(R.string.cemental_confirm_change)
            .setPositiveButton(R.string.common_confirm, (dialog, which) -> removeCemental())
            .setNegativeButton(R.string.common_cancel, null)
            .show();
    }

    private void removeCemental() {
        new Thread(() -> {
            Rabbit cemental = rabbitDao.getCementalSync();
            if (cemental != null) {
                cemental.isCemental = false;
                rabbitDao.update(cemental);
            }
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), R.string.cemental_removed, Toast.LENGTH_SHORT).show();
                loadCemental();
            });
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
