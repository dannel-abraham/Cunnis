package cu.dandroid.cunnis.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.databinding.ItemCageBinding;
import java.util.List;

public class CageAdapter extends ListAdapter<Cage, CageAdapter.CageViewHolder> {
    private OnCageClickListener listener;
    private List<Cage> cageList;

    public interface OnCageClickListener {
        void onCageClick(Cage cage);
    }

    public CageAdapter(OnCageClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Cage> DIFF_CALLBACK = new DiffUtil.ItemCallback<Cage>() {
        @Override
        public boolean areItemsTheSame(@NonNull Cage oldItem, @NonNull Cage newItem) {
            return oldItem.id == newItem.id;
        }
        @Override
        public boolean areContentsTheSame(@NonNull Cage oldItem, @NonNull Cage newItem) {
            return oldItem.cageNumber == newItem.cageNumber
                    && java.util.Objects.equals(oldItem.notes, newItem.notes);
        }
    };

    @NonNull
    @Override
    public CageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCageBinding binding = ItemCageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CageViewHolder holder, int position) {
        Cage cage = getItem(position);
        holder.binding.txtCageNumber.setText(String.format("#%d", cage.cageNumber));
        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onCageClick(cage);
        });
    }

    public void submitList(List<Cage> list) {
        cageList = list;
        super.submitList(list);
    }

    static class CageViewHolder extends RecyclerView.ViewHolder {
        ItemCageBinding binding;
        CageViewHolder(ItemCageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
