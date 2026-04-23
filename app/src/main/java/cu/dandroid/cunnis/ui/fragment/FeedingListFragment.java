package cu.dandroid.cunnis.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.CageDao;
import cu.dandroid.cunnis.data.local.db.dao.FeedingRecordDao;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.data.local.db.entity.FeedingRecord;
import cu.dandroid.cunnis.databinding.FragmentFeedingListBinding;
import cu.dandroid.cunnis.databinding.ItemFeedingBinding;
import cu.dandroid.cunnis.ui.activity.AddFeedingActivity;
import cu.dandroid.cunnis.util.DateUtils;

public class FeedingListFragment extends Fragment {

    private FragmentFeedingListBinding binding;
    private FeedingRecordDao feedingRecordDao;
    private CageDao cageDao;
    private FeedingAdapter adapter;
    private List<Cage> cages = new ArrayList<>();
    private List<FeedingRecord> allRecords = new ArrayList<>();
    private int selectedCageId = -1; // -1 means "All"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFeedingListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        feedingRecordDao = ((CunnisApp) requireActivity().getApplication()).getDatabase().feedingRecordDao();
        cageDao = ((CunnisApp) requireActivity().getApplication()).getDatabase().cageDao();

        adapter = new FeedingAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        // FAB to add feeding
        binding.fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddFeedingActivity.class));
        });

        // Load cages and records
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            cages = cageDao.getAllCagesSync();
            allRecords = feedingRecordDao.getAllFeedingRecordsSync();

            requireActivity().runOnUiThread(() -> {
                setupFilterChips();
                updateMonthSummary();
                applyFilter();
            });
        }).start();
    }

    private void setupFilterChips() {
        binding.chipGroupFilter.removeAllViews();

        // "All" chip
        Chip allChip = new Chip(requireContext());
        allChip.setText(R.string.feeding_filter_all);
        allChip.setId(View.generateViewId());
        allChip.setCheckable(true);
        allChip.setChecked(true);
        allChip.setStyle(R.style.Widget_Cunnis_Chip_Filter);
        allChip.setOnClickListener(v -> {
            selectedCageId = -1;
            applyFilter();
            updateMonthSummary();
        });
        binding.chipGroupFilter.addView(allChip);

        // One chip per cage
        for (Cage cage : cages) {
            Chip cageChip = new Chip(requireContext());
            cageChip.setText(String.format(Locale.getDefault(), "#%d", cage.cageNumber));
            cageChip.setId(View.generateViewId());
            cageChip.setCheckable(true);
            cageChip.setTag(cage.id);
            cageChip.setStyle(R.style.Widget_Cunnis_Chip_Filter);
            cageChip.setOnClickListener(v -> {
                selectedCageId = (int) v.getTag();
                applyFilter();
                updateMonthSummary();
            });
            binding.chipGroupFilter.addView(cageChip);
        }

        binding.chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            // Ensure only the clicked chip logic runs
        });
    }

    private void applyFilter() {
        List<FeedingRecord> filtered;
        if (selectedCageId == -1) {
            filtered = allRecords;
        } else {
            filtered = new ArrayList<>();
            for (FeedingRecord record : allRecords) {
                if (record.cageId == selectedCageId) {
                    filtered.add(record);
                }
            }
        }

        adapter.setItems(filtered);
        if (filtered == null || filtered.isEmpty()) {
            binding.txtEmpty.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.txtEmpty.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateMonthSummary() {
        // Calculate start of current month
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monthStart = cal.getTimeInMillis();

        int monthCount = 0;
        double monthQuantity = 0;

        for (FeedingRecord record : allRecords) {
            if (record.feedingDate >= monthStart) {
                if (selectedCageId == -1 || record.cageId == selectedCageId) {
                    monthCount++;
                    monthQuantity += record.quantity;
                }
            }
        }

        binding.txtMonthCount.setText(String.valueOf(monthCount));
        binding.txtMonthQuantity.setText(
                String.format(Locale.getDefault(), "%.1f lb", monthQuantity));
    }

    static class FeedingAdapter extends RecyclerView.Adapter<FeedingAdapter.ViewHolder> {
        private List<FeedingRecord> items = new ArrayList<>();

        void setItems(List<FeedingRecord> items) {
            this.items = items != null ? items : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemFeedingBinding binding = ItemFeedingBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FeedingRecord record = items.get(position);

            holder.binding.txtFeedType.setText(
                    record.feedType != null ? record.feedType : "");
            holder.binding.txtDate.setText(DateUtils.formatDate(record.feedingDate));
            holder.binding.txtQuantity.setText(
                    String.format(Locale.getDefault(), "%.1f lb", record.quantity));

            // Cage chip
            holder.binding.chipCage.setText(
                    holder.itemView.getContext().getString(R.string.cage_format, record.cageId));

            // Notes
            if (record.notes != null && !record.notes.isEmpty()) {
                holder.binding.txtNotes.setText(record.notes);
                holder.binding.txtNotes.setVisibility(View.VISIBLE);
            } else {
                holder.binding.txtNotes.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ItemFeedingBinding binding;

            ViewHolder(ItemFeedingBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
