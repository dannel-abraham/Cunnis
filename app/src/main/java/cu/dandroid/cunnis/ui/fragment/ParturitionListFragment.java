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
import cu.dandroid.cunnis.data.local.db.dao.ParturitionRecordDao;
import cu.dandroid.cunnis.data.local.db.entity.ParturitionRecord;
import cu.dandroid.cunnis.databinding.FragmentParturitionListBinding;
import cu.dandroid.cunnis.databinding.ItemParturitionBinding;
import cu.dandroid.cunnis.ui.activity.ParturitionRecordActivity;
import cu.dandroid.cunnis.util.Constants;
import cu.dandroid.cunnis.util.DateUtils;
import java.util.List;

public class ParturitionListFragment extends Fragment {

    private FragmentParturitionListBinding binding;
    private ParturitionAdapter adapter;
    private long rabbitId;
    private ParturitionRecordDao parturitionRecordDao;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            rabbitId = getArguments().getLong("rabbit_id", -1);
        }
        if (rabbitId <= 0) {
            rabbitId = requireActivity().getIntent().getLongExtra("rabbit_id", -1);
        }
        parturitionRecordDao = ((CunnisApp) requireActivity().getApplication()).getDatabase().parturitionRecordDao();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentParturitionListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new ParturitionAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ParturitionRecordActivity.class);
            intent.putExtra(ParturitionRecordActivity.EXTRA_RABBIT_ID, rabbitId);
            startActivity(intent);
        });

        if (rabbitId > 0) {
            LiveData<List<ParturitionRecord>> liveData = parturitionRecordDao.getParturitionsByDoe(rabbitId);
            liveData.observe(getViewLifecycleOwner(), records -> {
                adapter.submitList(records);
                updateSummary(records);
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

    private void updateSummary(List<ParturitionRecord> records) {
        if (records == null || records.isEmpty()) {
            binding.cardSummary.setVisibility(View.GONE);
            return;
        }
        int totalBorn = 0, bornAlive = 0, bornDead = 0, weaned = 0;
        for (ParturitionRecord r : records) {
            totalBorn += r.totalBorn;
            bornAlive += r.bornAlive;
            bornDead += r.bornDead;
            weaned += r.weanedCount;
        }
        binding.txtSummaryTotal.setText(String.valueOf(totalBorn));
        binding.txtSummaryAlive.setText(String.valueOf(bornAlive));
        binding.txtSummaryDead.setText(String.valueOf(bornDead));
        binding.txtSummaryWeaned.setText(String.valueOf(weaned));
        binding.cardSummary.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- Inner Adapter ---
    static class ParturitionAdapter extends ListAdapter<ParturitionRecord, ParturitionAdapter.ViewHolder> {

        ParturitionAdapter() {
            super(DIFF_CALLBACK);
        }

        private static final DiffUtil.ItemCallback<ParturitionRecord> DIFF_CALLBACK = new DiffUtil.ItemCallback<ParturitionRecord>() {
            @Override
            public boolean areItemsTheSame(@NonNull ParturitionRecord a, @NonNull ParturitionRecord b) {
                return a.id == b.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull ParturitionRecord a, @NonNull ParturitionRecord b) {
                return a.parturitionDate == b.parturitionDate
                        && a.totalBorn == b.totalBorn
                        && a.bornAlive == b.bornAlive
                        && a.bornDead == b.bornDead
                        && a.weanedCount == b.weanedCount;
            }
        };

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemParturitionBinding binding = ItemParturitionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ParturitionRecord record = getItem(position);
            holder.binding.txtDate.setText(DateUtils.formatDate(record.parturitionDate));

            // Estimated weaning date
            long weaningDate = DateUtils.addDays(record.parturitionDate, Constants.WEANING_AVG);
            holder.binding.txtWeaningDate.setText(
                    holder.itemView.getContext().getString(R.string.parturition_est_weaning) + ": "
                    + DateUtils.formatDate(weaningDate));
            holder.binding.txtWeaningDate.setVisibility(View.VISIBLE);

            // Stats
            holder.binding.txtTotalBorn.setText(String.valueOf(record.totalBorn));
            holder.binding.txtBornAlive.setText(String.valueOf(record.bornAlive));
            holder.binding.txtBornDead.setText(String.valueOf(record.bornDead));
            holder.binding.txtWeaned.setText(String.valueOf(record.weanedCount));

            // Complications icon
            if (record.hadComplications) {
                holder.binding.imgComplications.setVisibility(View.VISIBLE);
            } else {
                holder.binding.imgComplications.setVisibility(View.GONE);
            }

            // Notes
            String notes = record.notes != null ? record.notes : record.complicationsNotes;
            if (notes != null && !notes.isEmpty()) {
                holder.binding.txtNotes.setText(notes);
                holder.binding.txtNotes.setVisibility(View.VISIBLE);
            } else {
                holder.binding.txtNotes.setVisibility(View.GONE);
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ItemParturitionBinding binding;

            ViewHolder(ItemParturitionBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
