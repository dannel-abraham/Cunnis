package cu.dandroid.cunnis.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.data.local.db.entity.WeightRecord;
import cu.dandroid.cunnis.databinding.FragmentStatsRabbitBinding;
import cu.dandroid.cunnis.util.DateUtils;
import cu.dandroid.cunnis.viewmodel.StatisticsViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsRabbitFragment extends Fragment {
    private static final String ARG_RABBIT_ID = "rabbit_id";

    private FragmentStatsRabbitBinding binding;
    private StatisticsViewModel viewModel;
    private long rabbitId;

    public static StatsRabbitFragment newInstance(long rabbitId) {
        StatsRabbitFragment fragment = new StatsRabbitFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_RABBIT_ID, rabbitId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            rabbitId = getArguments().getLong(ARG_RABBIT_ID, -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsRabbitBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(StatisticsViewModel.class);

        if (rabbitId > 0) {
            viewModel.loadRabbitStats(rabbitId);
        }

        viewModel.getRabbitStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats == null || stats.rabbitId != rabbitId) return;
            bindStats(stats);
        });
    }

    private void bindStats(StatisticsViewModel.RabbitStats stats) {
        // Header
        String headerText = stats.name != null && !stats.name.isEmpty() ? stats.name : stats.identifier;
        binding.txtRabbitName.setText(headerText);

        // Gender chip
        if ("MALE".equals(stats.gender)) {
            binding.chipGender.setText(R.string.rabbit_male);
            binding.chipGender.setChipBackgroundColorResource(R.color.cunnis_male);
            binding.chipGender.setTextColor(Color.WHITE);
        } else if ("FEMALE".equals(stats.gender)) {
            binding.chipGender.setText(R.string.rabbit_female);
            binding.chipGender.setChipBackgroundColorResource(R.color.cunnis_female);
            binding.chipGender.setTextColor(Color.WHITE);
        } else {
            binding.chipGender.setVisibility(View.GONE);
        }

        // Weight chart
        setupWeightChart(stats.weightHistory);

        // Info cards
        binding.txtLatestWeight.setText(String.format("%.1f lb", stats.latestWeight));
        binding.txtAverageWeight.setText(String.format("%.1f lb", stats.averageWeight));
        binding.txtMaxWeight.setText(String.format("%.1f lb", stats.maxWeight));

        // Age
        if (stats.ageInDays >= 0) {
            if (stats.ageInDays >= 30) {
                binding.txtAge.setText(String.format("%d mo", stats.ageInDays / 30));
            } else {
                binding.txtAge.setText(String.format("%d d", stats.ageInDays));
            }
        } else {
            binding.txtAge.setText(getString(R.string.common_na));
        }

        // Matings
        binding.txtMatings.setText(String.valueOf(stats.totalMatings));
        binding.txtParturitions.setText(String.valueOf(stats.totalParturitions));

        // Offspring
        binding.txtTotalBorn.setText(String.valueOf(stats.totalBorn));
        binding.txtBornAlive.setText(String.valueOf(stats.bornAlive));
        binding.txtBornDead.setText(String.valueOf(stats.bornDead));
        binding.txtSuccessfulMatings.setText(String.valueOf(stats.successfulMatings));
    }

    private void setupWeightChart(List<WeightRecord> weightHistory) {
        if (weightHistory == null || weightHistory.isEmpty()) {
            binding.chartWeightEvolution.setVisibility(View.GONE);
            binding.txtNoWeightData.setVisibility(View.VISIBLE);
            return;
        }
        binding.chartWeightEvolution.setVisibility(View.VISIBLE);
        binding.txtNoWeightData.setVisibility(View.GONE);

        List<Entry> entries = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.US);

        for (int i = 0; i < weightHistory.size(); i++) {
            WeightRecord wr = weightHistory.get(i);
            entries.add(new Entry(i, (float) wr.weight));
            dateLabels.add(sdf.format(new Date(wr.recordDate)));
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.chart_weight_lb));
        dataSet.setColor(Color.parseColor("#00BFA5"));
        dataSet.setFillColor(Color.parseColor("#A7FFEB"));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#00BFA5"));
        dataSet.setCircleRadius(5f);
        dataSet.setCircleHoleRadius(2.5f);
        dataSet.setLineWidth(3f);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.parseColor("#333333"));
        dataSet.setDrawValues(true);
        dataSet.setValueFormatter(v -> String.format("%.1f", v));

        LineData lineData = new LineData(dataSet);
        binding.chartWeightEvolution.setData(lineData);

        // Professional styling
        binding.chartWeightEvolution.getDescription().setEnabled(false);
        binding.chartWeightEvolution.setDrawGridBackground(false);
        binding.chartWeightEvolution.setTouchEnabled(true);
        binding.chartWeightEvolution.setDragEnabled(true);
        binding.chartWeightEvolution.setPinchZoom(true);
        binding.chartWeightEvolution.setExtraOffsets(10f, 15f, 10f, 5f);
        binding.chartWeightEvolution.setBackgroundColor(Color.TRANSPARENT);

        // X axis
        XAxis xAxis = binding.chartWeightEvolution.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dateLabels));
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.parseColor("#757575"));

        // Left Y axis
        YAxis leftAxis = binding.chartWeightEvolution.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.parseColor("#757575"));
        leftAxis.setValueFormatter(v -> String.format("%.1f", v));

        binding.chartWeightEvolution.getAxisRight().setEnabled(false);

        // Legend
        Legend legend = binding.chartWeightEvolution.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
        legend.setForm(Legend.LegendForm.CIRCLE);

        binding.chartWeightEvolution.animateX(800, Easing.EaseInOutCubic);
        binding.chartWeightEvolution.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
