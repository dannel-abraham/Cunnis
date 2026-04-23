package cu.dandroid.cunnis.ui.fragment;

import android.content.Context;
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
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.dao.ExpenseRecordDao;
import cu.dandroid.cunnis.data.local.db.entity.ExpenseRecord;
import cu.dandroid.cunnis.data.model.ExpenseCategory;
import cu.dandroid.cunnis.databinding.FragmentExpensesBinding;
import cu.dandroid.cunnis.databinding.ItemExpenseBinding;
import cu.dandroid.cunnis.ui.activity.AddExpenseActivity;
import cu.dandroid.cunnis.util.DateUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpensesFragment extends Fragment {

    private FragmentExpensesBinding binding;
    private ExpenseRecordDao expenseRecordDao;
    private ExpenseAdapter adapter;
    private String currentFilter = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExpensesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        expenseRecordDao = ((CunnisApp) requireActivity().getApplication()).getDatabase().expenseRecordDao();

        adapter = new ExpenseAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        loadExpenses();
        loadTotal();

        setupFilterChips();

        binding.fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddExpenseActivity.class));
        });
    }

    private void setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                currentFilter = "";
            } else if (checkedId == R.id.chipFeed) {
                currentFilter = ExpenseCategory.FEED.getValue();
            } else if (checkedId == R.id.chipMedicine) {
                currentFilter = ExpenseCategory.MEDICINE.getValue();
            } else if (checkedId == R.id.chipEquipment) {
                currentFilter = ExpenseCategory.EQUIPMENT.getValue();
            } else if (checkedId == R.id.chipVeterinary) {
                currentFilter = ExpenseCategory.VETERINARY.getValue();
            } else if (checkedId == R.id.chipTransport) {
                currentFilter = ExpenseCategory.TRANSPORT.getValue();
            }
            loadExpenses();
            loadTotal();
        });
    }

    private void loadExpenses() {
        new Thread(() -> {
            List<ExpenseRecord> expenses;
            if (currentFilter == null || currentFilter.isEmpty()) {
                expenses = expenseRecordDao.getAllExpensesSync();
            } else {
                expenses = expenseRecordDao.getExpensesByCategorySync(currentFilter);
            }

            requireActivity().runOnUiThread(() -> {
                adapter.setItems(expenses);
                if (expenses == null || expenses.isEmpty()) {
                    binding.txtEmpty.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                } else {
                    binding.txtEmpty.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private void loadTotal() {
        new Thread(() -> {
            double total;
            if (currentFilter == null || currentFilter.isEmpty()) {
                total = expenseRecordDao.getTotalExpenses();
            } else {
                total = expenseRecordDao.getTotalByCategory(currentFilter);
            }

            requireActivity().runOnUiThread(() -> {
                binding.txtTotalAmount.setText(String.format(Locale.getDefault(), "$%.2f", total));
            });
        }).start();
    }

    static class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {
        private List<ExpenseRecord> items = new ArrayList<>();

        void setItems(List<ExpenseRecord> items) {
            this.items = items != null ? items : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemExpenseBinding binding = ItemExpenseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ExpenseRecord expense = items.get(position);
            holder.binding.txtDescription.setText(
                expense.description != null ? expense.description : "");
            holder.binding.txtDate.setText(DateUtils.formatDate(expense.expenseDate));
            holder.binding.txtAmount.setText(
                String.format(Locale.getDefault(), "$%.2f", expense.amount));

            // Category chip
            holder.binding.chipCategory.setText(getCategoryDisplayName(expense.category, holder.itemView.getContext()));
            int colorRes = getCategoryColor(expense.category);
            holder.binding.chipCategory.setChipBackgroundColorResource(colorRes);
            holder.binding.chipCategory.setTextColor(
                holder.itemView.getContext().getColor(R.color.cunnis_white));
        }

        private String getCategoryDisplayName(String category, Context ctx) {
            if (category == null) return ctx.getString(R.string.category_other);
            switch (category) {
                case "feed": return ctx.getString(R.string.category_feed);
                case "medicine": return ctx.getString(R.string.category_medicine);
                case "equipment": return ctx.getString(R.string.category_equipment);
                case "veterinary": return ctx.getString(R.string.category_veterinary);
                case "transport": return ctx.getString(R.string.category_transport);
                default: return ctx.getString(R.string.category_other);
            }
        }

        private int getCategoryColor(String category) {
            if (category == null) return R.color.cunnis_info;
            switch (category) {
                case "feed": return R.color.cunnis_control_normal;
                case "medicine": return R.color.cunnis_error;
                case "equipment": return R.color.cunnis_male;
                case "veterinary": return R.color.cunnis_accent;
                case "transport": return R.color.cunnis_female;
                default: return R.color.cunnis_info;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ItemExpenseBinding binding;
            ViewHolder(ItemExpenseBinding binding) {
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
