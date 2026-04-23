package cu.dandroid.cunnis.data.local.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.data.local.db.dao.HealthEventDao;
import cu.dandroid.cunnis.data.local.db.entity.HealthEvent;

public class HealthEventRepository {
    private final HealthEventDao dao;
    private final ExecutorService executor;

    public HealthEventRepository(Application app) {
        dao = ((CunnisApp) app).getDatabase().healthEventDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<HealthEvent>> getHealthEventsByRabbit(long rabbitId) {
        return dao.getHealthEventsByRabbit(rabbitId);
    }

    public List<HealthEvent> getHealthEventsByRabbitSync(long rabbitId) {
        return dao.getHealthEventsByRabbitSync(rabbitId);
    }

    public List<HealthEvent> getUpcomingDueEvents(long beforeDate) {
        return dao.getUpcomingDueEvents(beforeDate);
    }

    public long insert(HealthEvent event) {
        event.createdAt = System.currentTimeMillis();
        return dao.insert(event);
    }

    public void update(HealthEvent event) {
        executor.execute(() -> dao.update(event));
    }

    public void delete(HealthEvent event) {
        executor.execute(() -> dao.delete(event));
    }
}
