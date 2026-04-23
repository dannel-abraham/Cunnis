package cu.dandroid.cunnis.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.data.local.db.dao.*;
import cu.dandroid.cunnis.data.local.db.entity.*;
import java.util.*;
import java.util.stream.Collectors;

public class StatisticsViewModel extends AndroidViewModel {
    private final RabbitDao rabbitDao;
    private final CageDao cageDao;
    private final ParturitionRecordDao parturitionDao;
    private final ExpenseRecordDao expenseDao;
    private final WeightRecordDao weightDao;
    private final MatingRecordDao matingRecordDao;
    private final FeedingRecordDao feedingRecordDao;

    private final MutableLiveData<StatsSummary> generalStats = new MutableLiveData<>();
    private final MutableLiveData<RabbitStats> rabbitStats = new MutableLiveData<>();
    private final MutableLiveData<CageStats> cageStats = new MutableLiveData<>();
    private final MutableLiveData<EnhancedStats> enhancedStats = new MutableLiveData<>();

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        CunnisApp app = (CunnisApp) application;
        rabbitDao = app.getDatabase().rabbitDao();
        cageDao = app.getDatabase().cageDao();
        parturitionDao = app.getDatabase().parturitionRecordDao();
        expenseDao = app.getDatabase().expenseRecordDao();
        weightDao = app.getDatabase().weightRecordDao();
        matingRecordDao = app.getDatabase().matingRecordDao();
        feedingRecordDao = app.getDatabase().feedingRecordDao();
        loadGeneralStats();
        loadEnhancedStats();
    }

    // ===== Existing Methods =====

    public LiveData<StatsSummary> getGeneralStats() { return generalStats; }

    private void loadGeneralStats() {
        new Thread(() -> {
            StatsSummary stats = new StatsSummary();
            stats.totalRabbits = rabbitDao.getActiveRabbitCountSync();
            stats.males = rabbitDao.getActiveRabbitsSync().stream().filter(r -> r.gender.name().equals("MALE")).count();
            stats.females = stats.totalRabbits - stats.males;
            stats.totalCages = cageDao.getAllCagesSync().size();
            stats.totalBornAlive = parturitionDao.getTotalBornAlive();
            stats.totalBornDead = parturitionDao.getTotalBornDead();
            stats.totalParturitions = parturitionDao.getTotalParturitions();
            stats.avgLitterSize = stats.totalParturitions > 0
                ? (double)(stats.totalBornAlive + stats.totalBornDead) / stats.totalParturitions
                : 0;
            stats.mortalityRate = (stats.totalBornAlive + stats.totalBornDead) > 0
                ? (double) stats.totalBornDead / (stats.totalBornAlive + stats.totalBornDead) * 100
                : 0;
            stats.totalExpenses = expenseDao.getTotalExpenses();
            generalStats.postValue(stats);
        }).start();
    }

    public static class StatsSummary {
        public int totalRabbits;
        public long males;
        public long females;
        public int totalCages;
        public int totalBornAlive;
        public int totalBornDead;
        public int totalParturitions;
        public double avgLitterSize;
        public double mortalityRate;
        public double totalExpenses;
    }

    // ===== Per-Rabbit Stats =====

    public LiveData<RabbitStats> getRabbitStats() { return rabbitStats; }

    public void loadRabbitStats(long rabbitId) {
        new Thread(() -> {
            Rabbit rabbit = rabbitDao.getRabbitByIdSync(rabbitId);
            if (rabbit == null) return;

            RabbitStats stats = new RabbitStats();
            stats.rabbitId = rabbitId;
            stats.name = rabbit.name;
            stats.identifier = rabbit.identifier;
            stats.breed = rabbit.breed;
            stats.gender = rabbit.gender.name();

            // Weight history (chronological for chart)
            stats.weightHistory = weightDao.getWeightRecordsByRabbitChronological(rabbitId);

            // Weight stats
            WeightRecord latestWeight = weightDao.getLatestWeight(rabbitId);
            stats.latestWeight = latestWeight != null ? latestWeight.weight : 0;
            stats.averageWeight = weightDao.getAverageWeight(rabbitId);
            stats.maxWeight = weightDao.getMaxWeight(rabbitId);

            // Age in days
            if (rabbit.birthDate != null) {
                stats.ageInDays = (System.currentTimeMillis() - rabbit.birthDate) / (1000 * 60 * 60 * 24);
            } else {
                stats.ageInDays = -1; // unknown
            }

            // Matings - check as doe or buck
            List<MatingRecord> matingsAsDoe = matingRecordDao.getMatingRecordsByDoeSync(rabbitId);
            List<MatingRecord> matingsAsBuck = matingRecordDao.getMatingRecordsByBuckSync(rabbitId);
            List<MatingRecord> allMatings = new ArrayList<>();
            allMatings.addAll(matingsAsDoe);
            allMatings.addAll(matingsAsBuck);
            stats.totalMatings = allMatings.size();
            stats.successfulMatings = (int) allMatings.stream().filter(m -> m.isEffective).count();

            // Parturitions - as doe or buck
            List<ParturitionRecord> parturitionsAsDoe = parturitionDao.getParturitionsByDoeSync(rabbitId);
            List<ParturitionRecord> parturitionsAsBuck = parturitionDao.getParturitionsByBuckSync(rabbitId);
            List<ParturitionRecord> allParturitions = new ArrayList<>(parturitionsAsDoe);
            allParturitions.addAll(parturitionsAsBuck);
            stats.totalParturitions = allParturitions.size();
            stats.totalBorn = allParturitions.stream().mapToInt(p -> p.totalBorn).sum();
            stats.bornAlive = allParturitions.stream().mapToInt(p -> p.bornAlive).sum();
            stats.bornDead = allParturitions.stream().mapToInt(p -> p.bornDead).sum();

            rabbitStats.postValue(stats);
        }).start();
    }

    public static class RabbitStats {
        public long rabbitId;
        public String name;
        public String identifier;
        public String breed;
        public String gender;
        public List<WeightRecord> weightHistory = new ArrayList<>();
        public double latestWeight;
        public double averageWeight;
        public double maxWeight;
        public long ageInDays;
        public int totalMatings;
        public int successfulMatings;
        public int totalParturitions;
        public int totalBorn;
        public int bornAlive;
        public int bornDead;
    }

    // ===== Per-Cage Stats =====

    public LiveData<CageStats> getCageStats() { return cageStats; }

    public void loadCageStats(int cageId) {
        new Thread(() -> {
            CageStats stats = new CageStats();
            stats.cageId = cageId;

            List<Rabbit> rabbitsInCage = rabbitDao.getRabbitsByCageSync(cageId);
            stats.rabbitCount = rabbitsInCage.size();

            // Average weight
            double totalWeight = 0;
            int weightCount = 0;
            for (Rabbit r : rabbitsInCage) {
                double avg = weightDao.getAverageWeight(r.id);
                if (avg > 0) {
                    totalWeight += avg;
                    weightCount++;
                }
            }
            stats.averageWeight = weightCount > 0 ? totalWeight / weightCount : 0;

            // Total feedings
            List<FeedingRecord> feedings = feedingRecordDao.getFeedingByCageSync(cageId);
            stats.totalFeedings = feedings != null ? feedings.size() : 0;

            cageStats.postValue(stats);
        }).start();
    }

    public static class CageStats {
        public int cageId;
        public int rabbitCount;
        public double averageWeight;
        public int totalFeedings;
    }

    // ===== Enhanced General Stats =====

    public LiveData<EnhancedStats> getEnhancedStats() { return enhancedStats; }

    private void loadEnhancedStats() {
        new Thread(() -> {
            EnhancedStats stats = new EnhancedStats();

            // Monthly weight evolution (average per month)
            Calendar cal = Calendar.getInstance();
            List<WeightRecord> allWeights = new ArrayList<>();
            for (Rabbit rabbit : rabbitDao.getActiveRabbitsSync()) {
                allWeights.addAll(weightDao.getWeightRecordsByRabbitSync(rabbit.id));
            }
            // Group by month
            Map<String, List<Double>> weightByMonth = new LinkedHashMap<>();
            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
            for (WeightRecord wr : allWeights) {
                String month = monthFormat.format(new Date(wr.recordDate));
                weightByMonth.computeIfAbsent(month, k -> new ArrayList<>()).add(wr.weight);
            }
            stats.weightEvolutionMonths = new ArrayList<>(weightByMonth.keySet());
            stats.weightEvolutionValues = new ArrayList<>();
            for (String month : stats.weightEvolutionMonths) {
                List<Double> weights = weightByMonth.get(month);
                double avg = weights.stream().mapToDouble(d -> d).average().orElse(0);
                stats.weightEvolutionValues.add(avg);
            }

            // Reproduction timeline (monthly born alive vs dead)
            List<ParturitionRecord> allParturitions = new ArrayList<>();
            for (Rabbit rabbit : rabbitDao.getActiveFemalesSync()) {
                allParturitions.addAll(parturitionDao.getParturitionsByDoeSync(rabbit.id));
            }
            Map<String, int[]> reproductionByMonth = new LinkedHashMap<>();
            SimpleDateFormat reproFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
            for (ParturitionRecord pr : allParturitions) {
                String month = reproFormat.format(new Date(pr.parturitionDate));
                int[] counts = reproductionByMonth.getOrDefault(month, new int[]{0, 0});
                counts[0] += pr.bornAlive;
                counts[1] += pr.bornDead;
                reproductionByMonth.put(month, counts);
            }
            stats.reproductionMonths = new ArrayList<>(reproductionByMonth.keySet());
            stats.reproductionBornAlive = new ArrayList<>();
            stats.reproductionBornDead = new ArrayList<>();
            for (String month : stats.reproductionMonths) {
                int[] counts = reproductionByMonth.get(month);
                stats.reproductionBornAlive.add(counts[0]);
                stats.reproductionBornDead.add(counts[1]);
            }

            // Breed distribution
            Map<String, Long> breedCounts = rabbitDao.getActiveRabbitsSync().stream()
                .filter(r -> r.breed != null && !r.breed.isEmpty())
                .collect(Collectors.groupingBy(r -> r.breed, Collectors.counting()));
            stats.breedNames = new ArrayList<>(breedCounts.keySet());
            stats.breedCounts = new ArrayList<>(breedCounts.values());

            // Gender distribution
            List<Rabbit> activeRabbits = rabbitDao.getActiveRabbitsSync();
            stats.maleCount = (int) activeRabbits.stream().filter(r -> r.gender.name().equals("MALE")).count();
            stats.femaleCount = (int) activeRabbits.stream().filter(r -> r.gender.name().equals("FEMALE")).count();
            stats.unknownGenderCount = activeRabbits.size() - stats.maleCount - stats.femaleCount;

            // Expense by category
            List<ExpenseRecord> allExpenses = expenseDao.getAllExpensesSync();
            Map<String, Double> expenseByCategory = new LinkedHashMap<>();
            for (ExpenseRecord er : allExpenses) {
                String cat = er.category != null ? er.category : "other";
                expenseByCategory.merge(cat, er.amount, Double::sum);
            }
            stats.expenseCategoryNames = new ArrayList<>(expenseByCategory.keySet());
            stats.expenseCategoryValues = new ArrayList<>(expenseByCategory.values());

            // Monthly expenses breakdown
            SimpleDateFormat expenseFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
            Map<String, Double> expenseByMonth = new LinkedHashMap<>();
            for (ExpenseRecord er : allExpenses) {
                String month = expenseFormat.format(new Date(er.expenseDate));
                expenseByMonth.merge(month, er.amount, Double::sum);
            }
            stats.expenseMonths = new ArrayList<>(expenseByMonth.keySet());
            stats.expenseMonthlyValues = new ArrayList<>(expenseByMonth.values());

            enhancedStats.postValue(stats);
        }).start();
    }

    public static class EnhancedStats {
        // Weight evolution
        public List<String> weightEvolutionMonths = new ArrayList<>();
        public List<Double> weightEvolutionValues = new ArrayList<>();

        // Reproduction timeline
        public List<String> reproductionMonths = new ArrayList<>();
        public List<Integer> reproductionBornAlive = new ArrayList<>();
        public List<Integer> reproductionBornDead = new ArrayList<>();

        // Breed distribution
        public List<String> breedNames = new ArrayList<>();
        public List<Long> breedCounts = new ArrayList<>();

        // Gender distribution
        public int maleCount;
        public int femaleCount;
        public int unknownGenderCount;

        // Expense by category
        public List<String> expenseCategoryNames = new ArrayList<>();
        public List<Double> expenseCategoryValues = new ArrayList<>();

        // Monthly expenses
        public List<String> expenseMonths = new ArrayList<>();
        public List<Double> expenseMonthlyValues = new ArrayList<>();
    }
}
