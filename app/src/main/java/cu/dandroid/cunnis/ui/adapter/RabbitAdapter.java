package cu.dandroid.cunnis.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.model.Gender;
import cu.dandroid.cunnis.databinding.ItemRabbitBinding;
import cu.dandroid.cunnis.util.ImageUtils;
import java.util.List;

public class RabbitAdapter extends ListAdapter<Rabbit, RabbitAdapter.RabbitViewHolder> {
    private OnRabbitClickListener listener;

    public interface OnRabbitClickListener {
        void onRabbitClick(Rabbit rabbit);
    }

    public RabbitAdapter(OnRabbitClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Rabbit> DIFF_CALLBACK = new DiffUtil.ItemCallback<Rabbit>() {
        @Override
        public boolean areItemsTheSame(@NonNull Rabbit oldItem, @NonNull Rabbit newItem) {
            return oldItem.id == newItem.id;
        }
        @Override
        public boolean areContentsTheSame(@NonNull Rabbit oldItem, @NonNull Rabbit newItem) {
            return java.util.Objects.equals(oldItem.name, newItem.name)
                    && java.util.Objects.equals(oldItem.identifier, newItem.identifier)
                    && oldItem.gender == newItem.gender;
        }
    };

    @NonNull
    @Override
    public RabbitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRabbitBinding binding = ItemRabbitBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RabbitViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RabbitViewHolder holder, int position) {
        Rabbit rabbit = getItem(position);
        holder.binding.txtName.setText(rabbit.name);
        holder.binding.txtIdentifier.setText(rabbit.identifier);

        // Gender chip
        if (rabbit.gender == Gender.MALE) {
            holder.binding.chipGender.setText(R.string.rabbit_male);
            holder.binding.chipGender.setChipBackgroundColorResource(R.color.cunnis_male);
            holder.binding.chipGender.setTextColor(holder.itemView.getContext().getColor(R.color.cunnis_white));
        } else {
            holder.binding.chipGender.setText(R.string.rabbit_female);
            holder.binding.chipGender.setChipBackgroundColorResource(R.color.cunnis_female);
            holder.binding.chipGender.setTextColor(holder.itemView.getContext().getColor(R.color.cunnis_white));
        }

        // Breed chip
        if (rabbit.breed != null && !rabbit.breed.isEmpty()) {
            holder.binding.chipBreed.setText(rabbit.breed);
            holder.binding.chipBreed.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.chipBreed.setVisibility(android.view.View.GONE);
        }

        // Photo
        if (rabbit.profilePhoto != null) {
            holder.binding.imgRabbit.setImageBitmap(ImageUtils.bytesToBitmap(rabbit.profilePhoto));
        }

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onRabbitClick(rabbit);
        });
    }

    public void submitList(List<Rabbit> list) {
        super.submitList(list);
    }

    static class RabbitViewHolder extends RecyclerView.ViewHolder {
        ItemRabbitBinding binding;
        RabbitViewHolder(ItemRabbitBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
