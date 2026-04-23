package cu.dandroid.cunnis.data.local.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.data.local.db.dao.WeightRecordDao;
import cu.dandroid.cunnis.data.local.db.entity.WeightRecord;

public class WeightRecordRepository {
    private final WeightRecordDao dao;
    private final ExecutorService executor;

    public WeightRecordRepository(Application app) {
        dao = ((CunnisApp) app).getDatabase().weightRecordDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<WeightRecord>> getWeightRecordsByRabbit(long rabbitId) {
        return dao.getWeightRecordsByRabbit(rabbitId);
    }

    public List<WeightRecord> getWeightRecordsByRabbitSync(long rabbitId) {
        return dao.getWeightRecordsByRabbitSync(rabbitId);
    }

    public List<WeightRecord> getWeightRecordsChronological(long rabbitId) {
        return dao.getWeightRecordsByRabbitChronological(rabbitId);
    }

    public WeightRecord getLatestWeight(long rabbitId) {
        return dao.getLatestWeight(rabbitId);
    }

    public double getAverageWeight(long rabbitId) {
        return dao.getAverageWeight(rabbitId);
    }

    public double getMaxWeight(long rabbitId) {
        return dao.getMaxWeight(rabbitId);
    }

    public long insert(WeightRecord record) {
        record.createdAt = System.currentTimeMillis();
        return dao.insert(record);
    }

    public void update(WeightRecord record) {
        executor.execute(() -> dao.update(record));
    }

    public void delete(WeightRecord record) {
        executor.execute(() -> dao.delete(record));
    }
}
