package cu.dandroid.cunnis.ui.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialTextInputLayout;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.EstrusRecordDao;
import cu.dandroid.cunnis.data.local.db.entity.EstrusRecord;
import cu.dandroid.cunnis.databinding.FragmentEstrusListBinding;
import cu.dandroid.cunnis.databinding.ItemEstrusBinding;
import cu.dandroid.cunnis.util.Constants;
import cu.dandroid.cunnis.util.DateUtils;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EstrusListFragment extends Fragment {

    private FragmentEstrusListBinding binding;
    private EstrusAdapter adapter;
    private long rabbitId;
    private EstrusRecordDao estrusRecordDao;
    private long selectedEstrusDate;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            rabbitId = getArguments().getLong("rabbit_id", -1);
        }
        if (rabbitId <= 0) {
            rabbitId = requireActivity().getIntent().getLongExtra("rabbit_id", -1);
        }
        estrusRecordDao = ((CunnisApp) requireActivity().getApplication()).getDatabase().estrusRecordDao();
        selectedEstrusDate = DateUtils.today();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEstrusListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new EstrusAdapter(estrusRecord -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.common_delete)
                    .setMessage(R.string.estrus_delete_confirm)
                    .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                        new Thread(() -> estrusRecordDao.delete(estrusRecord)).start();
                        Toast.makeText(requireContext(), R.string.estrus_deleted, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.common_no, null)
                    .show();
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v -> showAddEstrusDialog());

        if (rabbitId > 0) {
            LiveData<List<EstrusRecord>> liveData = estrusRecordDao.getEstrusRecordsByRabbit(rabbitId);
            liveData.observe(getViewLifecycleOwner(), records -> {
                adapter.submitList(records);
                updateNextEstrusPrediction(records);
                if (records == null || records.isEmpty()) {
                    binding.txtEmpty.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                } else {
                    binding.txtEmpty.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void updateNextEstrusPrediction(List<EstrusRecord> records) {
        if (records == null || records.isEmpty()) {
            binding.cardNextEstrus.setVisibility(View.GONE);
            return;
        }
        EstrusRecord latest = records.get(0);
        if (latest.estrusDate > 0) {
            long predictedDate = DateUtils.addDays(latest.estrusDate, Constants.ESTRUS_CYCLE_AVG);
            String predicted = DateUtils.formatDate(predictedDate);
            String text = getString(R.string.estrus_next_predicted, predicted);
            binding.txtNextEstrus.setText(text);
            binding.cardNextEstrus.setVisibility(View.VISIBLE);
        } else {
            binding.cardNextEstrus.setVisibility(View.GONE);
        }
    }

    private void showAddEstrusDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_estrus, null);
        MaterialTextInputLayout tilDate = dialogView.findViewById(R.id.tilDate);
        MaterialTextInputLayout tilObservations = dialogView.findViewById(R.id.tilObservations);
        Chip chipReceptive = dialogView.findViewById(R.id.chipReceptive);

        tilDate.getEditText().setText(DateUtils.formatDate(selectedEstrusDate));
        chipReceptive.setChecked(true);

        tilDate.getEditText().setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedEstrusDate);
            new DatePickerDialog(requireContext(), (view1, year, month, dayOfMonth) -> {
                Calendar dateCal = Calendar.getInstance();
                dateCal.set(year, month, dayOfMonth);
                selectedEstrusDate = dateCal.getTimeInMillis();
                tilDate.getEditText().setText(DateUtils.formatDate(selectedEstrusDate));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.estrus_add)
                .setView(dialogView)
                .setPositiveButton(R.string.common_save, (dialog, which) -> {
                    String observations = tilObservations.getEditText().getText().toString().trim();
                    boolean receptive = chipReceptive.isChecked();

                    EstrusRecord record = new EstrusRecord();
                    record.rabbitId = rabbitId;
                    record.estrusDate = selectedEstrusDate;
                    record.receptive = receptive;
                    record.observations = observations.isEmpty() ? null : observations;
                    record.createdAt = DateUtils.now();

                    new Thread(() -> estrusRecordDao.insert(record)).start();
                    Toast.makeText(requireContext(), R.string.estrus_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- Inner Adapter ---
    static class EstrusAdapter extends ListAdapter<EstrusRecord, EstrusAdapter.ViewHolder> {

        private final OnDeleteClickListener deleteListener;

        interface OnDeleteClickListener {
            void onDelete(EstrusRecord record);
        }

        EstrusAdapter(OnDeleteClickListener deleteListener) {
            super(DIFF_CALLBACK);
            this.deleteListener = deleteListener;
        }

        private static final DiffUtil.ItemCallback<EstrusRecord> DIFF_CALLBACK = new DiffUtil.ItemCallback<EstrusRecord>() {
            @Override
            public boolean areItemsTheSame(@NonNull EstrusRecord a, @NonNull EstrusRecord b) {
                return a.id == b.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull EstrusRecord a, @NonNull EstrusRecord b) {
                return a.receptive == b.receptive
                        && a.estrusDate == b.estrusDate;
            }
        };

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemEstrusBinding binding = ItemEstrusBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EstrusRecord record = getItem(position);
            holder.binding.txtDate.setText(DateUtils.formatDate(record.estrusDate));

            if (record.receptive) {
                holder.binding.chipReceptive.setText(R.string.estrus_receptive);
                holder.binding.chipReceptive.setChipBackgroundColorResource(R.color.cunnis_success);
                holder.binding.chipReceptive.setTextColor(holder.itemView.getContext().getColor(R.color.cunnis_black));
            } else {
                holder.binding.chipReceptive.setText(R.string.estrus_not_receptive);
                holder.binding.chipReceptive.setChipBackgroundColorResource(R.color.cunnis_error);
                holder.binding.chipReceptive.setTextColor(holder.itemView.getContext().getColor(R.color.cunnis_white));
            }

            if (record.observations != null && !record.observations.isEmpty()) {
                holder.binding.txtObservations.setText(record.observations);
                holder.binding.txtObservations.setVisibility(View.VISIBLE);
            } else {
                holder.binding.txtObservations.setVisibility(View.GONE);
            }

            holder.binding.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(record);
                }
            });
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ItemEstrusBinding binding;

            ViewHolder(ItemEstrusBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
