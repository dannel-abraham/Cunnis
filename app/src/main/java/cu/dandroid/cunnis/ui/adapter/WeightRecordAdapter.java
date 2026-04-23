package cu.dandroid.cunnis.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import cu.dandroid.cunnis.data.local.db.entity.WeightRecord;
import cu.dandroid.cunnis.databinding.ItemInfoRowBinding;
import cu.dandroid.cunnis.util.DateUtils;
import java.util.List;
import java.util.Locale;

public class WeightRecordAdapter extends ListAdapter<WeightRecord, WeightRecordAdapter.ViewHolder> {
    public WeightRecordAdapter() { super(DIFF_CALLBACK); }

    private static final DiffUtil.ItemCallback<WeightRecord> DIFF_CALLBACK = new DiffUtil.ItemCallback<WeightRecord>() {
        @Override
        public boolean areItemsTheSame(@NonNull WeightRecord a, @NonNull WeightRecord b) { return a.id == b.id; }
        @Override
        public boolean areContentsTheSame(@NonNull WeightRecord a, @NonNull WeightRecord b) { return a.weight == b.weight; }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInfoRowBinding binding = ItemInfoRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeightRecord record = getItem(position);
        holder.binding.txtLabel.setText(DateUtils.formatDate(record.recordDate));
        holder.binding.txtValue.setText(String.format(Locale.getDefault(), "%.1f lb", record.weight));
    }

    public void submitList(List<WeightRecord> list) { super.submitList(list); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemInfoRowBinding binding;
        ViewHolder(ItemInfoRowBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}
