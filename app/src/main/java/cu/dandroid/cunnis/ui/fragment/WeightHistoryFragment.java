package cu.dandroid.cunnis.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.entity.WeightRecord;
import cu.dandroid.cunnis.databinding.FragmentWeightHistoryBinding;
import cu.dandroid.cunnis.ui.activity.WeightRecordActivity;
import cu.dandroid.cunnis.ui.adapter.WeightRecordAdapter;
import cu.dandroid.cunnis.util.Constants;
import cu.dandroid.cunnis.util.DateUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeightHistoryFragment extends Fragment {
    private FragmentWeightHistoryBinding binding;
    private WeightRecordAdapter adapter;
    private long rabbitId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rabbitId = requireActivity().getIntent().getLongExtra("rabbit_id", -1);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWeightHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new WeightRecordAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        // FAB click → launch WeightRecordActivity
        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), WeightRecordActivity.class);
            intent.putExtra(WeightRecordActivity.EXTRA_RABBIT_ID, rabbitId);
            startActivity(intent);
        });

        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning from WeightRecordActivity
        loadData();
    }

    private void loadData() {
        if (rabbitId <= 0) return;

        new Thread(() -> {
            CunnisApp app = (CunnisApp) requireActivity().getApplication();

            // Get records sorted by date DESC for the list
            List<WeightRecord> records = app.getDatabase().weightRecordDao()
                    .getWeightRecordsByRabbitSync(rabbitId);

            // Get chronological records (ASC) for chart
            List<WeightRecord> chronoRecords = app.getDatabase().weightRecordDao()
                    .getWeightRecordsByRabbitChronological(rabbitId);

            // Get stats
            WeightRecord latest = app.getDatabase().weightRecordDao().getLatestWeight(rabbitId);
            double avg = app.getDatabase().weightRecordDao().getAverageWeight(rabbitId);
            double max = app.getDatabase().weightRecordDao().getMaxWeight(rabbitId);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.submitList(records);
                    updateStats(latest, avg, max);
                    updateChart(chronoRecords, avg);
                });
            }
        }).start();
    }

    private void updateStats(WeightRecord latest, double avg, double max) {
        DecimalFormat df = new DecimalFormat("#0.0");

        // Latest weight
        if (latest != null && latest.weight > 0) {
            binding.txtLatestWeight.setText(
                    String.format(Locale.getDefault(), "%s %s", df.format(latest.weight), Constants.WEIGHT_UNIT));
        } else {
            binding.txtLatestWeight.setText(getString(R.string.common_na));
        }

        // Average weight
        if (avg > 0) {
            binding.txtAvgWeight.setText(
                    String.format(Locale.getDefault(), "%s %s", df.format(avg), Constants.WEIGHT_UNIT));
        } else {
            binding.txtAvgWeight.setText(getString(R.string.common_na));
        }

        // Max weight
        if (max > 0) {
            binding.txtMaxWeight.setText(
                    String.format(Locale.getDefault(), "%s %s", df.format(max), Constants.WEIGHT_UNIT));
        } else {
            binding.txtMaxWeight.setText(getString(R.string.common_na));
        }
    }

    private void updateChart(List<WeightRecord> records, double average) {
        if (records == null || records.isEmpty()) {
            binding.chartWeight.clear();
            binding.chartWeight.setNoDataText(getString(R.string.common_no_data));
            binding.chartWeight.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            WeightRecord record = records.get(i);
            entries.add(new Entry(i, (float) record.weight));
            dateLabels.add(DateUtils.formatShortDate(record.recordDate));
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.chart_weight_lb));
        dataSet.setColor(Color.parseColor("#00BFA5")); // Teal
        dataSet.setFillColor(Color.parseColor("#A7FFEB")); // Light teal fill
        dataSet.setDrawFilled(true);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(Color.parseColor("#00BFA5"));
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#00897B"));

        LineData lineData = new LineData(dataSet);

        // Average weight line
        if (average > 0) {
            List<Entry> avgEntries = new ArrayList<>();
            avgEntries.add(new Entry(0, (float) average));
            avgEntries.add(new Entry(records.size() - 1, (float) average));

            LineDataSet avgDataSet = new LineDataSet(avgEntries, getString(R.string.chart_average_weight));
            avgDataSet.setColor(Color.parseColor("#FF7043")); // Orange accent
            avgDataSet.setLineWidth(1.5f);
            avgDataSet.setDashPathEffect(new android.graphics.DashPathEffect(
                    new float[]{10f, 10f}, 0f));
            avgDataSet.setDrawValues(false);
            avgDataSet.setDrawCircles(false);
            avgDataSet.setDrawFilled(false);
            avgDataSet.setHighlightEnabled(false);
            lineData.addDataSet(avgDataSet);
        }

        binding.chartWeight.setData(lineData);

        // X Axis
        XAxis xAxis = binding.chartWeight.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < dateLabels.size()) {
                    return dateLabels.get(index);
                }
                return "";
            }
        });
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#757575"));

        // Y Axis (left)
        YAxis leftAxis = binding.chartWeight.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#757575"));
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return value + " " + Constants.WEIGHT_UNIT;
            }
        });
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setGridLineWidth(0.5f);

        // Y Axis (right) - disable
        YAxis rightAxis = binding.chartWeight.getAxisRight();
        rightAxis.setEnabled(false);

        // Description
        Description description = new Description();
        description.setText("");
        binding.chartWeight.setDescription(description);

        // Legend
        Legend legend = binding.chartWeight.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(11f);
        legend.setTextColor(Color.parseColor("#757575"));

        // Interaction
        binding.chartWeight.setTouchEnabled(true);
        binding.chartWeight.setDragEnabled(true);
        binding.chartWeight.setPinchZoom(true);
        binding.chartWeight.setScaleEnabled(true);

        // Animation
        binding.chartWeight.animateXY(800, 800, Easing.EaseInOutCubic);

        binding.chartWeight.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
