package cu.dandroid.cunnis.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import cu.dandroid.cunnis.data.local.db.entity.HealthEvent;
import cu.dandroid.cunnis.databinding.ItemInfoRowBinding;
import cu.dandroid.cunnis.util.DateUtils;
import java.util.List;

public class HealthEventAdapter extends ListAdapter<HealthEvent, HealthEventAdapter.ViewHolder> {
    public HealthEventAdapter() { super(DIFF_CALLBACK); }

    private static final DiffUtil.ItemCallback<HealthEvent> DIFF_CALLBACK = new DiffUtil.ItemCallback<HealthEvent>() {
        @Override
        public boolean areItemsTheSame(@NonNull HealthEvent a, @NonNull HealthEvent b) { return a.id == b.id; }
        @Override
        public boolean areContentsTheSame(@NonNull HealthEvent a, @NonNull HealthEvent b) { return java.util.Objects.equals(a.eventType, b.eventType); }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInfoRowBinding binding = ItemInfoRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HealthEvent event = getItem(position);
        holder.binding.txtLabel.setText(DateUtils.formatDate(event.eventDate) + " - " + event.eventType);
        holder.binding.txtValue.setText(event.title != null ? event.title : "");
    }

    public void submitList(List<HealthEvent> list) { super.submitList(list); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemInfoRowBinding binding;
        ViewHolder(ItemInfoRowBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}
