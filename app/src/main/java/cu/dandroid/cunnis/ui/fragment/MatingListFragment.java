package cu.dandroid.cunnis.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.MatingRecordDao;
import cu.dandroid.cunnis.data.local.db.dao.RabbitDao;
import cu.dandroid.cunnis.data.local.db.entity.MatingRecord;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.model.MatingResult;
import cu.dandroid.cunnis.databinding.FragmentMatingListBinding;
import cu.dandroid.cunnis.databinding.ItemMatingBinding;
import cu.dandroid.cunnis.ui.activity.MatingRecordActivity;
import cu.dandroid.cunnis.util.Constants;
import cu.dandroid.cunnis.util.DateUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MatingListFragment extends Fragment {

    private FragmentMatingListBinding binding;
    private MatingAdapter adapter;
    private long rabbitId;
    private MatingRecordDao matingRecordDao;
    private RabbitDao rabbitDao;
    private Map<Long, String> buckNames = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            rabbitId = getArguments().getLong("rabbit_id", -1);
        }
        if (rabbitId <= 0) {
            rabbitId = requireActivity().getIntent().getLongExtra("rabbit_id", -1);
        }
        CunnisApp app = (CunnisApp) requireActivity().getApplication();
        matingRecordDao = app.getDatabase().matingRecordDao();
        rabbitDao = app.getDatabase().rabbitDao();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMatingListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new MatingAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MatingRecordActivity.class);
            intent.putExtra(MatingRecordActivity.EXTRA_RABBIT_ID, rabbitId);
            startActivity(intent);
        });

        if (rabbitId > 0) {
            // Pre-load buck names
            new Thread(() -> {
                List<MatingRecord> records = matingRecordDao.getMatingRecordsByDoeSync(rabbitId);
                for (MatingRecord record : records) {
                    if (!buckNames.containsKey(record.buckId)) {
                        Rabbit buck = rabbitDao.getRabbitByIdSync(record.buckId);
                        if (buck != null) {
                            buckNames.put(record.buckId, String.format(Locale.getDefault(),
                                    "%s (#%s)", buck.name, buck.identifier));
                        }
                    }
                }
            }).start();

            LiveData<List<MatingRecord>> liveData = matingRecordDao.getMatingRecordsByDoe(rabbitId);
            liveData.observe(getViewLifecycleOwner(), records -> {
                // Reload buck names in case new bucks are referenced
                new Thread(() -> {
                    for (MatingRecord record : records) {
                        if (!buckNames.containsKey(record.buckId)) {
                            Rabbit buck = rabbitDao.getRabbitByIdSync(record.buckId);
                            if (buck != null) {
                                buckNames.put(record.buckId, String.format(Locale.getDefault(),
                                        "%s (#%s)", buck.name, buck.identifier));
                            }
                        }
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            adapter.submitList(records);
                            updateEmptyState(records);
                        });
                    }
                }).start();
            });
        }
    }

    private void updateEmptyState(List<MatingRecord> records) {
        if (records == null || records.isEmpty()) {
            binding.txtEmpty.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.txtEmpty.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- Inner Adapter ---
    class MatingAdapter extends ListAdapter<MatingRecord, MatingAdapter.ViewHolder> {

        MatingAdapter() {
            super(DIFF_CALLBACK);
        }

        private static final DiffUtil.ItemCallback<MatingRecord> DIFF_CALLBACK = new DiffUtil.ItemCallback<MatingRecord>() {
            @Override
            public boolean areItemsTheSame(@NonNull MatingRecord a, @NonNull MatingRecord b) {
                return a.id == b.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull MatingRecord a, @NonNull MatingRecord b) {
                return a.matingDate == b.matingDate
                        && a.isEffective == b.isEffective
                        && java.util.Objects.equals(a.result, b.result);
            }
        };

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemMatingBinding binding = ItemMatingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MatingRecord record = getItem(position);
            holder.binding.txtDate.setText(DateUtils.formatDate(record.matingDate));

            // Result chip
            String result = record.result;
            if (result == null) result = MatingResult.PENDING.name();
            switch (result) {
                case "SUCCESSFUL":
                    holder.binding.chipResult.setText(R.string.mating_successful);
                    holder.binding.chipResult.setChipBackgroundColorResource(R.color.cunnis_success);
                    holder.binding.chipResult.setTextColor(holder.itemView.getContext().getColor(R.color.cunnis_black));
                    break;
                case "UNSUCCESSFUL":
                    holder.binding.chipResult.setText(R.string.mating_unsuccessful);
                    holder.binding.chipResult.setChipBackgroundColorResource(R.color.cunnis_error);
                    holder.binding.chipResult.setTextColor(holder.itemView.getContext().getColor(R.color.cunnis_white));
                    break;
                default:
                    holder.binding.chipResult.setText(R.string.mating_pending);
                    holder.binding.chipResult.setChipBackgroundColorResource(R.color.cunnis_warning);
                    holder.binding.chipResult.setTextColor(holder.itemView.getContext().getColor(R.color.cunnis_black));
                    break;
            }

            // Attempt number
            holder.binding.txtAttempt.setText(String.format(Locale.getDefault(), "#%d", record.attemptNumber));

            // Buck name
            String buckName = buckNames.get(record.buckId);
            if (buckName != null) {
                holder.binding.txtBuck.setText(buckName);
            } else {
                holder.binding.txtBuck.setText(getString(R.string.mating_buck));
            }

            // Gestation due date
            long dueDate = DateUtils.addDays(record.matingDate, Constants.GESTATION_AVG);
            String dueDateStr = DateUtils.formatDate(dueDate);
            holder.binding.txtDueDate.setText(getString(R.string.mating_due_date, dueDateStr));
            holder.binding.txtDueDate.setVisibility(View.VISIBLE);

            // Observations
            if (record.observations != null && !record.observations.isEmpty()) {
                holder.binding.txtObservations.setText(record.observations);
                holder.binding.txtObservations.setVisibility(View.VISIBLE);
            } else {
                holder.binding.txtObservations.setVisibility(View.GONE);
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemMatingBinding binding;

            ViewHolder(ItemMatingBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
