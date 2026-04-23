package cu.dandroid.cunnis.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.AlertConfigDao;
import cu.dandroid.cunnis.data.local.db.entity.AlertConfig;
import cu.dandroid.cunnis.data.model.AlertType;
import cu.dandroid.cunnis.databinding.FragmentAlertsBinding;
import cu.dandroid.cunnis.util.AlertCheckWorker;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class AlertsFragment extends Fragment {

    private FragmentAlertsBinding binding;
    private AlertConfigDao alertConfigDao;
    private AlertConfigAdapter adapter;
    private List<AlertConfig> configs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alertConfigDao = ((CunnisApp) requireActivity().getApplication()).getDatabase().alertConfigDao();

        adapter = new AlertConfigAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        loadConfigs();

        binding.btnCheckNow.setOnClickListener(v -> checkAlerts());
    }

    private void loadConfigs() {
        new Thread(() -> {
            List<AlertConfig> dbConfigs = alertConfigDao.getAllConfigsSync();

            if (dbConfigs == null || dbConfigs.isEmpty()) {
                // Initialize default alert configs
                dbConfigs = new ArrayList<>();
                AlertConfig[] defaults = {
                    createDefault(AlertType.ESTRUS_PREDICTION, 2),
                    createDefault(AlertType.GESTATION_DUE, 3),
                    createDefault(AlertType.WEANING, 2),
                    createDefault(AlertType.REPRODUCTIVE_AGE, 7),
                    createDefault(AlertType.WEIGHT_CHECK, 1),
                    createDefault(AlertType.VACCINATION_DUE, 7)
                };
                for (AlertConfig ac : defaults) {
                    long id = alertConfigDao.insert(ac);
                    ac.id = id;
                    dbConfigs.add(ac);
                }
            }

            requireActivity().runOnUiThread(() -> {
                configs = dbConfigs;
                adapter.setItems(configs);
                if (configs.isEmpty()) {
                    binding.txtEmpty.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                } else {
                    binding.txtEmpty.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private AlertConfig createDefault(AlertType type, int advanceDays) {
        AlertConfig ac = new AlertConfig();
        ac.alertType = type.getValue();
        ac.enabled = true;
        ac.advanceDays = advanceDays;
        ac.advanceHours = 0;
        return ac;
    }

    /**
     * Trigger a one-time AlertCheckWorker and show the result.
     */
    private void checkAlerts() {
        // Show "checking" state immediately
        binding.btnCheckNow.setEnabled(false);
        binding.btnCheckNow.setText(getString(R.string.alert_checking));
        binding.cardPendingResult.setVisibility(View.VISIBLE);
        binding.txtPendingResult.setText(getString(R.string.alert_checking));

        // Build and enqueue a OneTimeWorkRequest
        OneTimeWorkRequest alertWork = new OneTimeWorkRequest.Builder(AlertCheckWorker.class)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(alertWork);

        // Observe the work result
        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(alertWork.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        binding.btnCheckNow.setEnabled(true);
                        binding.btnCheckNow.setText(getString(R.string.alerts_check_now));

                        int notificationCount = 0;
                        if (workInfo.getOutputData() != null) {
                            notificationCount = workInfo.getOutputData()
                                    .getInt(AlertCheckWorker.KEY_NOTIFICATION_COUNT, 0);
                        }

                        if (notificationCount > 0) {
                            binding.txtPendingResult.setText(
                                    getString(R.string.alert_notifications_sent, notificationCount));
                        } else {
                            binding.txtPendingResult.setText(R.string.alerts_no_pending);
                        }

                        // Show completion toast
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            Toast.makeText(requireContext(),
                                    getString(R.string.alert_check_complete),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    class AlertConfigAdapter extends RecyclerView.Adapter<AlertConfigAdapter.ViewHolder> {
        private List<AlertConfig> items = new ArrayList<>();

        void setItems(List<AlertConfig> items) {
            this.items = items != null ? items : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert_config, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AlertConfig config = items.get(position);
            holder.txtName.setText(getAlertName(config.alertType));
            holder.txtDescription.setText(getAlertDescription(config.alertType));
            holder.txtAdvanceDays.setText(getString(R.string.alert_advance_days) + ": " + config.advanceDays);
            holder.switchEnabled.setChecked(config.enabled);

            holder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                config.enabled = isChecked;
                updateConfig(config);
            });

            holder.itemView.setOnClickListener(v -> showAdvanceDaysDialog(config));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName;
            TextView txtDescription;
            TextView txtAdvanceDays;
            com.google.android.material.materialswitch.MaterialSwitch switchEnabled;

            ViewHolder(View view) {
                super(view);
                txtName = view.findViewById(R.id.txtAlertName);
                txtDescription = view.findViewById(R.id.txtAlertDescription);
                txtAdvanceDays = view.findViewById(R.id.txtAdvanceDays);
                switchEnabled = view.findViewById(R.id.switchAlertEnabled);
            }
        }
    }

    private String getAlertName(String type) {
        if (type == null) return getString(R.string.alert_type_unknown);
        switch (type) {
            case "estrus_prediction": return getString(R.string.alert_estrus);
            case "gestation_due": return getString(R.string.alert_gestation);
            case "weaning": return getString(R.string.alert_weaning);
            case "reproductive_age": return getString(R.string.alert_reproductive_age);
            case "weight_check": return getString(R.string.alert_weight_check);
            case "vaccination_due": return getString(R.string.alert_vaccination);
            default: return getString(R.string.alert_type_unknown);
        }
    }

    private String getAlertDescription(String type) {
        if (type == null) return "";
        switch (type) {
            case "estrus_prediction": return getString(R.string.alerts_estrus_prediction_desc);
            case "gestation_due": return getString(R.string.alerts_gestation_due_desc);
            case "weaning": return getString(R.string.alerts_weaning_desc);
            case "reproductive_age": return getString(R.string.alerts_reproductive_age_desc);
            case "weight_check": return getString(R.string.alerts_weight_check_desc);
            case "vaccination_due": return getString(R.string.alerts_vaccination_due_desc);
            default: return "";
        }
    }

    private void updateConfig(AlertConfig config) {
        new Thread(() -> {
            alertConfigDao.update(config);
        }).start();
    }

    private void showAdvanceDaysDialog(AlertConfig config) {
        EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(config.advanceDays));
        input.setPadding((int) getResources().getDimension(R.dimen.padding_medium),
            (int) getResources().getDimension(R.dimen.padding_small),
            (int) getResources().getDimension(R.dimen.padding_medium),
            (int) getResources().getDimension(R.dimen.padding_small));

        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.alert_advance_days)
            .setView(input)
            .setPositiveButton(R.string.common_save, (dialog, which) -> {
                String value = input.getText().toString().trim();
                if (!value.isEmpty()) {
                    int days = Integer.parseInt(value);
                    config.advanceDays = days;
                    updateConfig(config);
                    loadConfigs();
                }
            })
            .setNegativeButton(R.string.common_cancel, null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
