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
import com.github.mikephil.charting.utils.ColorTemplate;
import cu.dandroid.cunnis.R;
import cu.dandroid.cunnis.databinding.FragmentStatsGeneralBinding;
import cu.dandroid.cunnis.viewmodel.StatisticsViewModel;

import java.util.ArrayList;
import java.util.List;

public class StatsGeneralFragment extends Fragment {
    private FragmentStatsGeneralBinding binding;
    private StatisticsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsGeneralBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(StatisticsViewModel.class);

        viewModel.getGeneralStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats == null) return;
            binding.txtTotalRabbits.setText(String.valueOf(stats.totalRabbits));
            binding.txtTotalCages.setText(String.valueOf(stats.totalCages));
            binding.txtTotalBorn.setText(String.valueOf(stats.totalBornAlive));
            binding.txtAvgLitter.setText(String.format("%.1f", stats.avgLitterSize));
            binding.txtMortality.setText(String.format("%.1f%%", stats.mortalityRate));
            binding.txtTotalExpenses.setText(String.format("$%.2f", stats.totalExpenses));
            binding.txtMales.setText(String.valueOf(stats.males));
            binding.txtFemales.setText(String.valueOf(stats.females));
            binding.txtTotalParturitions.setText(String.valueOf(stats.totalParturitions));
        });

        viewModel.getEnhancedStats().observe(getViewLifecycleOwner(), enhanced -> {
            if (enhanced == null) return;
            setupWeightEvolutionChart(enhanced.weightEvolutionMonths, enhanced.weightEvolutionValues);
            setupReproductionChart(enhanced.reproductionMonths, enhanced.reproductionBornAlive, enhanced.reproductionBornDead);
            setupExpensePieChart(enhanced.expenseCategoryNames, enhanced.expenseCategoryValues);
            setupBreedDistributionChart(enhanced.breedNames, enhanced.breedCounts);
        });
    }

    private void setupWeightEvolutionChart(List<String> months, List<Double> values) {
        if (months.isEmpty() || values.isEmpty()) {
            binding.chartWeightEvolution.setVisibility(View.GONE);
            binding.txtNoWeightData.setVisibility(View.VISIBLE);
            return;
        }
        binding.chartWeightEvolution.setVisibility(View.VISIBLE);
        binding.txtNoWeightData.setVisibility(View.GONE);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            entries.add(new Entry(i, values.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.chart_avg_weight_lb));
        dataSet.setColor(Color.parseColor("#00BFA5"));
        dataSet.setFillColor(Color.parseColor("#A7FFEB"));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#00BFA5"));
        dataSet.setCircleRadius(4f);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setLineWidth(2.5f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#333333"));
        dataSet.setDrawValues(true);

        LineData lineData = new LineData(dataSet);
        binding.chartWeightEvolution.setData(lineData);

        // Style
        binding.chartWeightEvolution.getDescription().setEnabled(false);
        binding.chartWeightEvolution.setDrawGridBackground(false);
        binding.chartWeightEvolution.setTouchEnabled(true);
        binding.chartWeightEvolution.setDragEnabled(true);
        binding.chartWeightEvolution.setPinchZoom(true);
        binding.chartWeightEvolution.setExtraOffsets(10f, 10f, 10f, 5f);

        // X axis
        XAxis xAxis = binding.chartWeightEvolution.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(10f);

        // Left Y axis
        YAxis leftAxis = binding.chartWeightEvolution.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setTextSize(10f);
        leftAxis.setValueFormatter(v -> String.format("%.1f", v));

        binding.chartWeightEvolution.getAxisRight().setEnabled(false);

        Legend legend = binding.chartWeightEvolution.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(11f);

        binding.chartWeightEvolution.animateX(800, Easing.EaseInOutCubic);
        binding.chartWeightEvolution.invalidate();
    }

    private void setupReproductionChart(List<String> months, List<Integer> bornAlive, List<Integer> bornDead) {
        if (months.isEmpty()) {
            binding.chartReproduction.setVisibility(View.GONE);
            binding.txtNoReproductionData.setVisibility(View.VISIBLE);
            return;
        }
        binding.chartReproduction.setVisibility(View.VISIBLE);
        binding.txtNoReproductionData.setVisibility(View.GONE);

        List<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < months.size(); i++) {
            float alive = bornAlive.size() > i ? bornAlive.get(i) : 0f;
            float dead = bornDead.size() > i ? bornDead.get(i) : 0f;
            barEntries.add(new BarEntry(i, new float[]{alive, dead}));
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "");
        barDataSet.setStackLabels(new String[]{getString(R.string.stats_alive), getString(R.string.stats_dead)});
        barDataSet.setColors(Color.parseColor("#69F0AE"), Color.parseColor("#FF5252"));
        barDataSet.setValueTextSize(10f);
        barDataSet.setValueTextColor(Color.parseColor("#333333"));
        barDataSet.setDrawValues(true);

        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.6f);
        binding.chartReproduction.setData(barData);

        // Style
        binding.chartReproduction.getDescription().setEnabled(false);
        binding.chartReproduction.setDrawGridBackground(false);
        binding.chartReproduction.setDrawValueAboveBar(true);
        binding.chartReproduction.setExtraOffsets(10f, 10f, 10f, 5f);

        XAxis xAxis = binding.chartReproduction.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(10f);

        YAxis leftAxis = binding.chartReproduction.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setTextSize(10f);
        leftAxis.setMinValue(0f);

        binding.chartReproduction.getAxisRight().setEnabled(false);

        Legend legend = binding.chartReproduction.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(11f);

        binding.chartReproduction.animateY(800, Easing.EaseInOutCubic);
        binding.chartReproduction.invalidate();
    }

    private void setupExpensePieChart(List<String> categoryNames, List<Double> categoryValues) {
        if (categoryNames.isEmpty()) {
            binding.chartExpenseBreakdown.setVisibility(View.GONE);
            binding.txtNoExpenseData.setVisibility(View.VISIBLE);
            return;
        }
        binding.chartExpenseBreakdown.setVisibility(View.VISIBLE);
        binding.txtNoExpenseData.setVisibility(View.GONE);

        List<PieEntry> pieEntries = new ArrayList<>();
        for (int i = 0; i < categoryNames.size(); i++) {
            pieEntries.add(new PieEntry(categoryValues.get(i).floatValue(), capitalizeCategory(categoryNames.get(i))));
        }

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(
            Color.parseColor("#00BFA5"),  // teal - feed
            Color.parseColor("#FF8A65"),  // orange - medicine
            Color.parseColor("#42A5F5"),  // blue - equipment
            Color.parseColor("#AB47BC"),  // purple - veterinary
            Color.parseColor("#FFD740"),  // yellow - transport
            Color.parseColor("#78909C")   // grey - other
        );
        pieDataSet.setValueTextSize(11f);
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setDrawValues(true);
        pieDataSet.setSliceSpace(2f);

        PieData pieData = new PieData(pieDataSet);
        binding.chartExpenseBreakdown.setData(pieData);

        // Style
        binding.chartExpenseBreakdown.getDescription().setEnabled(false);
        binding.chartExpenseBreakdown.setDrawHoleEnabled(true);
        binding.chartExpenseBreakdown.setHoleColor(Color.TRANSPARENT);
        binding.chartExpenseBreakdown.setHoleRadius(45f);
        binding.chartExpenseBreakdown.setTransparentCircleRadius(50f);
        binding.chartExpenseBreakdown.setCenterText("$");
        binding.chartExpenseBreakdown.setCenterTextSize(14f);
        binding.chartExpenseBreakdown.setDrawEntryLabels(true);
        binding.chartExpenseBreakdown.setEntryLabelTextSize(10f);
        binding.chartExpenseBreakdown.setEntryLabelColor(Color.parseColor("#333333"));

        Legend legend = binding.chartExpenseBreakdown.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(10f);
        legend.setWordWrapEnabled(true);

        binding.chartExpenseBreakdown.animateY(800, Easing.EaseInOutCubic);
        binding.chartExpenseBreakdown.invalidate();
    }

    private void setupBreedDistributionChart(List<String> breedNames, List<Long> breedCounts) {
        if (breedNames.isEmpty()) {
            binding.chartBreedDistribution.setVisibility(View.GONE);
            binding.txtNoBreedData.setVisibility(View.VISIBLE);
            return;
        }
        binding.chartBreedDistribution.setVisibility(View.VISIBLE);
        binding.txtNoBreedData.setVisibility(View.GONE);

        List<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < breedNames.size(); i++) {
            barEntries.add(new BarEntry(i, breedCounts.get(i).floatValue()));
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "");
        barDataSet.setColors(
            Color.parseColor("#00BFA5"),
            Color.parseColor("#FF8A65"),
            Color.parseColor("#42A5F5"),
            Color.parseColor("#EC407A"),
            Color.parseColor("#AB47BC"),
            Color.parseColor("#FFD740"),
            Color.parseColor("#69F0AE")
        );
        barDataSet.setValueTextSize(11f);
        barDataSet.setValueTextColor(Color.parseColor("#333333"));
        barDataSet.setDrawValues(true);

        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.7f);
        binding.chartBreedDistribution.setData(barData);

        // Style
        binding.chartBreedDistribution.getDescription().setEnabled(false);
        binding.chartBreedDistribution.setDrawGridBackground(false);
        binding.chartBreedDistribution.setExtraOffsets(10f, 10f, 10f, 5f);
        binding.chartBreedDistribution.setFitBars(true);

        XAxis xAxis = binding.chartBreedDistribution.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(breedNames));
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(9f);
        xAxis.setLabelRotationAngle(-30f);

        YAxis leftAxis = binding.chartBreedDistribution.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setTextSize(10f);
        leftAxis.setMinValue(0f);

        binding.chartBreedDistribution.getAxisRight().setEnabled(false);

        Legend legend = binding.chartBreedDistribution.getLegend();
        legend.setEnabled(false);

        binding.chartBreedDistribution.animateY(800, Easing.EaseInOutCubic);
        binding.chartBreedDistribution.invalidate();
    }

    private String capitalizeCategory(String category) {
        if (category == null || category.isEmpty()) return getString(R.string.category_other);
        return category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
