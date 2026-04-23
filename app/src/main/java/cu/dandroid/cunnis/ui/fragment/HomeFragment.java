package cu.dandroid.cunnis.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.AlertConfigDao;
import cu.dandroid.cunnis.data.local.db.dao.CageDao;
import cu.dandroid.cunnis.data.local.db.dao.EstrusRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.MatingRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.ParturitionRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.RabbitDao;
import cu.dandroid.cunnis.data.local.db.entity.AlertConfig;
import cu.dandroid.cunnis.data.local.db.entity.EstrusRecord;
import cu.dandroid.cunnis.data.local.db.entity.MatingRecord;
import cu.dandroid.cunnis.data.local.db.entity.ParturitionRecord;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.local.db.entity.UserProfile;
import cu.dandroid.cunnis.data.local.db.dao.UserProfileDao;
import cu.dandroid.cunnis.databinding.FragmentHomeBinding;
import cu.dandroid.cunnis.ui.activity.AddEditCageActivity;
import cu.dandroid.cunnis.ui.activity.AddEditRabbitActivity;
import cu.dandroid.cunnis.ui.activity.AddExpenseActivity;
import cu.dandroid.cunnis.ui.activity.MatingRecordActivity;
import cu.dandroid.cunnis.util.Constants;
import cu.dandroid.cunnis.util.DateUtils;
import cu.dandroid.cunnis.viewmodel.RabbitViewModel;
import cu.dandroid.cunnis.viewmodel.CageViewModel;
import cu.dandroid.cunnis.CunnisDatabase;
import cu.dandroid.cunnis.CunnisApp;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private RabbitViewModel rabbitViewModel;
    private CageViewModel cageViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rabbitViewModel = new ViewModelProvider(requireActivity()).get(RabbitViewModel.class);
        cageViewModel = new ViewModelProvider(requireActivity()).get(CageViewModel.class);

        // Observe rabbit counts
        rabbitViewModel.getActiveCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) binding.txtTotalRabbits.setText(String.valueOf(count));
        });
        rabbitViewModel.getActiveMaleCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) binding.txtMales.setText(String.valueOf(count));
        });
        rabbitViewModel.getActiveFemaleCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) binding.txtFemales.setText(String.valueOf(count));
        });

        // Observe cage count
        cageViewModel.getCageCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) binding.txtCageCount.setText(String.valueOf(count));
        });

        // Load farm name
        loadFarmName();

        // Load pending alerts
        loadPendingAlerts();

        // Quick actions
        binding.btnAddRabbit.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddEditRabbitActivity.class));
        });
        binding.btnAddCage.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddEditCageActivity.class));
        });
        binding.btnRecordMating.setOnClickListener(v -> {
            showMatingDialog();
        });
        binding.btnAddExpense.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddExpenseActivity.class));
        });

        // View All alerts → navigate to AlertsFragment
        binding.btnViewAllAlerts.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new AlertsFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void loadFarmName() {
        UserProfileDao dao = ((CunnisApp) requireActivity().getApplication()).getDatabase().userProfileDao();
        new Thread(() -> {
            UserProfile profile = dao.getProfileSync();
            if (profile != null && profile.farmName != null && !profile.farmName.isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    binding.txtFarmName.setText(profile.farmName);
                });
            }
        }).start();
    }

    /**
     * Lightweight alert count computation for the Home screen.
     * Runs on a background thread and updates UI with:
     * - estrusCount: active females with predicted estrus in next advanceDays
     * - gestationCount: effective matings with due date in next advanceDays
     * - weaningCount: parturition records ready for weaning
     */
    private void loadPendingAlerts() {
        new Thread(() -> {
            try {
                CunnisDatabase db = ((CunnisApp) requireActivity().getApplication()).getDatabase();
                AlertConfigDao alertConfigDao = db.alertConfigDao();

                // Get enabled alert configs to find advanceDays
                List<AlertConfig> enabledConfigs = alertConfigDao.getAllConfigsSync();
                int estrusAdvance = getAdvanceDays(enabledConfigs, "estrus_prediction", 2);
                int gestationAdvance = getAdvanceDays(enabledConfigs, "gestation_due", 3);
                int weaningAdvance = getAdvanceDays(enabledConfigs, "weaning", 2);

                boolean estrusEnabled = isAlertEnabled(enabledConfigs, "estrus_prediction");
                boolean gestationEnabled = isAlertEnabled(enabledConfigs, "gestation_due");
                boolean weaningEnabled = isAlertEnabled(enabledConfigs, "weaning");

                long todayStart = DateUtils.today();
                int estrusCount = 0;
                int gestationCount = 0;
                int weaningCount = 0;

                // Check estrus predictions
                if (estrusEnabled) {
                    List<Rabbit> females = db.rabbitDao().getActiveFemalesSync();
                    if (females != null) {
                        for (Rabbit female : females) {
                            EstrusRecord latest = db.estrusRecordDao().getLatestEstrus(female.id);
                            if (latest == null) continue;
                            long predicted = DateUtils.addDays(latest.estrusDate, Constants.ESTRUS_CYCLE_AVG);
                            long windowStart = DateUtils.addDays(predicted, -estrusAdvance);
                            long windowEnd = DateUtils.addDays(predicted, 1);
                            if (todayStart >= windowStart && todayStart <= windowEnd) {
                                estrusCount++;
                            }
                        }
                    }
                }

                // Check gestation due
                if (gestationEnabled) {
                    List<MatingRecord> matings = db.matingRecordDao().getAllMatingRecordsSync();
                    if (matings != null) {
                        for (MatingRecord mating : matings) {
                            boolean isEffective = mating.isEffective
                                    || "SUCCESSFUL".equalsIgnoreCase(mating.result);
                            if (!isEffective) continue;
                            ParturitionRecord existing = db.parturitionRecordDao()
                                    .getByMatingRecordId(mating.id);
                            if (existing != null) continue;
                            long dueDate = DateUtils.addDays(mating.matingDate, Constants.GESTATION_AVG);
                            long windowStart = DateUtils.addDays(dueDate, -gestationAdvance);
                            long windowEnd = DateUtils.addDays(dueDate, 3);
                            if (todayStart >= windowStart && todayStart <= windowEnd) {
                                gestationCount++;
                            }
                        }
                    }
                }

                // Check weaning due
                if (weaningEnabled) {
                    List<ParturitionRecord> parturitions = db.parturitionRecordDao().getAllParturitionsSync();
                    if (parturitions != null) {
                        for (ParturitionRecord parturition : parturitions) {
                            if (parturition.weanedCount > 0) continue;
                            long weaningDate = DateUtils.addDays(parturition.parturitionDate, Constants.WEANING_AVG);
                            long windowStart = DateUtils.addDays(weaningDate, -weaningAdvance);
                            long windowEnd = DateUtils.addDays(weaningDate, 3);
                            if (todayStart >= windowStart && todayStart <= windowEnd) {
                                weaningCount++;
                            }
                        }
                    }
                }

                int finalEstrusCount = estrusCount;
                int finalGestationCount = gestationCount;
                int finalWeaningCount = weaningCount;

                requireActivity().runOnUiThread(() -> {
                    int totalAlerts = finalEstrusCount + finalGestationCount + finalWeaningCount;

                    // Show/hide the entire pending alerts section
                    if (totalAlerts > 0) {
                        binding.layoutPendingAlerts.setVisibility(View.VISIBLE);
                        binding.txtNoAlerts.setVisibility(View.GONE);
                        binding.layoutAlertCards.setVisibility(View.VISIBLE);

                        // Estrus card
                        if (finalEstrusCount > 0) {
                            binding.cardEstrusAlert.setVisibility(View.VISIBLE);
                            binding.txtEstrusCount.setText(String.valueOf(finalEstrusCount));
                        } else {
                            binding.cardEstrusAlert.setVisibility(View.GONE);
                        }

                        // Gestation card
                        if (finalGestationCount > 0) {
                            binding.cardGestationAlert.setVisibility(View.VISIBLE);
                            binding.txtGestationCount.setText(String.valueOf(finalGestationCount));
                        } else {
                            binding.cardGestationAlert.setVisibility(View.GONE);
                        }

                        // Weaning card
                        if (finalWeaningCount > 0) {
                            binding.cardWeaningAlert.setVisibility(View.VISIBLE);
                            binding.txtWeaningCount.setText(String.valueOf(finalWeaningCount));
                        } else {
                            binding.cardWeaningAlert.setVisibility(View.GONE);
                        }
                    } else {
                        binding.layoutPendingAlerts.setVisibility(View.GONE);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                // Silently fail - don't show alerts section on error
                requireActivity().runOnUiThread(() -> {
                    binding.layoutPendingAlerts.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private int getAdvanceDays(List<AlertConfig> configs, String alertType, int defaultValue) {
        if (configs == null) return defaultValue;
        for (AlertConfig config : configs) {
            if (alertType.equalsIgnoreCase(config.alertType) && config.enabled) {
                return config.advanceDays > 0 ? config.advanceDays : defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean isAlertEnabled(List<AlertConfig> configs, String alertType) {
        if (configs == null) return false;
        for (AlertConfig config : configs) {
            if (alertType.equalsIgnoreCase(config.alertType)) {
                return config.enabled;
            }
        }
        return false;
    }

    private void showMatingDialog() {
        RabbitDao rabbitDao = ((CunnisApp) requireActivity().getApplication()).getDatabase().rabbitDao();

        new Thread(() -> {
            List<Rabbit> females = rabbitDao.getActiveFemalesSync();
            requireActivity().runOnUiThread(() -> {
                if (females == null || females.isEmpty()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.home_select_doe)
                            .setMessage(R.string.home_no_females)
                            .setPositiveButton(R.string.common_ok, null)
                            .show();
                    return;
                }

                // Build display labels
                List<String> labels = new ArrayList<>();
                for (Rabbit r : females) {
                    labels.add(String.format(Locale.getDefault(), "%s (#%s)", r.name, r.identifier));
                }

                String[] items = labels.toArray(new String[0]);
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.home_select_doe)
                        .setItems(items, (dialog, which) -> {
                            Rabbit selected = females.get(which);
                            Intent intent = new Intent(requireContext(), MatingRecordActivity.class);
                            intent.putExtra(MatingRecordActivity.EXTRA_RABBIT_ID, selected.id);
                            startActivity(intent);
                        })
                        .setNegativeButton(R.string.common_cancel, null)
                        .show();
            });
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
