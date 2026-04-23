package cu.dandroid.cunnis.data.local.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.data.local.db.dao.MatingRecordDao;
import cu.dandroid.cunnis.data.local.db.entity.MatingRecord;

public class MatingRecordRepository {
    private final MatingRecordDao dao;
    private final ExecutorService executor;

    public MatingRecordRepository(Application app) {
        dao = ((CunnisApp) app).getDatabase().matingRecordDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<MatingRecord>> getMatingRecordsByDoe(long doeId) { return dao.getMatingRecordsByDoe(doeId); }
    public LiveData<List<MatingRecord>> getMatingRecordsByBuck(long buckId) { return dao.getMatingRecordsByBuck(buckId); }
    public List<MatingRecord> getMatingRecordsByDoeSync(long doeId) { return dao.getMatingRecordsByDoeSync(doeId); }
    public List<MatingRecord> getMatingRecordsByBuckSync(long buckId) { return dao.getMatingRecordsByBuckSync(buckId); }
    public LiveData<List<MatingRecord>> getAllMatingRecords() { return dao.getAllMatingRecords(); }

    public long insert(MatingRecord record) {
        record.createdAt = System.currentTimeMillis();
        return dao.insert(record);
    }

    public void update(MatingRecord record) {
        executor.execute(() -> dao.update(record));
    }

    public void delete(MatingRecord record) {
        executor.execute(() -> dao.delete(record));
    }
}
