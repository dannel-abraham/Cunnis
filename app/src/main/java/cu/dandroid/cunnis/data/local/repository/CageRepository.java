package cu.dandroid.cunnis.data.local.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.data.local.db.dao.CageDao;
import cu.dandroid.cunnis.data.local.db.dao.RabbitDao;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;

public class CageRepository {
    private final CageDao cageDao;
    private final RabbitDao rabbitDao;
    private final ExecutorService executor;

    public CageRepository(Application app) {
        cageDao = ((CunnisApp) app).getDatabase().cageDao();
        rabbitDao = ((CunnisApp) app).getDatabase().rabbitDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Cage>> getAllCages() {
        return cageDao.getAllCages();
    }

    public List<Cage> getAllCagesSync() {
        return cageDao.getAllCagesSync();
    }

    public LiveData<Cage> getCageById(int id) {
        return cageDao.getCageById(id);
    }

    public LiveData<List<Rabbit>> getRabbitsByCage(int cageId) {
        return rabbitDao.getRabbitsByCage(cageId);
    }

    public List<Rabbit> getRabbitsByCageSync(int cageId) {
        return rabbitDao.getRabbitsByCageSync(cageId);
    }

    public LiveData<Integer> getCageCount() {
        return cageDao.getCageCount();
    }

    public long insert(Cage cage) {
        cage.updatedAt = System.currentTimeMillis();
        return cageDao.insert(cage);
    }

    public void update(Cage cage) {
        cage.updatedAt = System.currentTimeMillis();
        executor.execute(() -> cageDao.update(cage));
    }

    public void delete(Cage cage) {
        executor.execute(() -> cageDao.delete(cage));
    }

    public void deleteById(int id) {
        executor.execute(() -> cageDao.deleteById(id));
    }
}
