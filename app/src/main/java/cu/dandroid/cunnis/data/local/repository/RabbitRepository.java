package cu.dandroid.cunnis.data.local.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.data.local.db.dao.*;
import cu.dandroid.cunnis.data.local.db.entity.*;
import cu.dandroid.cunnis.data.model.Gender;
import cu.dandroid.cunnis.data.model.RabbitStatus;
import cu.dandroid.cunnis.util.Constants;

public class RabbitRepository {
    private final RabbitDao rabbitDao;
    private final BreedDao breedDao;
    private final MovementRecordDao movementDao;
    private final ExecutorService executor;

    public RabbitRepository(Application app) {
        rabbitDao = ((CunnisApp) app).getDatabase().rabbitDao();
        breedDao = ((CunnisApp) app).getDatabase().breedDao();
        movementDao = ((CunnisApp) app).getDatabase().movementRecordDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Rabbit>> getAllRabbits() { return rabbitDao.getAllRabbits(); }
    public List<Rabbit> getAllRabbitsSync() { return rabbitDao.getAllRabbitsSync(); }
    public LiveData<List<Rabbit>> getActiveRabbits() { return rabbitDao.getActiveRabbits(); }
    public List<Rabbit> getActiveRabbitsSync() { return rabbitDao.getActiveRabbitsSync(); }
    public LiveData<Rabbit> getRabbitById(long id) { return rabbitDao.getRabbitById(id); }
    public Rabbit getRabbitByIdSync(long id) { return rabbitDao.getRabbitByIdSync(id); }
    public LiveData<List<Rabbit>> getRabbitsByCage(int cageId) { return rabbitDao.getRabbitsByCage(cageId); }
    public List<Rabbit> getRabbitsByCageSync(int cageId) { return rabbitDao.getRabbitsByCageSync(cageId); }
    public LiveData<Rabbit> getCemental() { return rabbitDao.getCemental(); }
    public Rabbit getCementalSync() { return rabbitDao.getCementalSync(); }
    public LiveData<List<Rabbit>> getActiveFemales() { return rabbitDao.getActiveFemales(); }
    public List<Rabbit> getActiveFemalesSync() { return rabbitDao.getActiveFemalesSync(); }
    public LiveData<List<Rabbit>> getOffspringBySire(long sireId) { return rabbitDao.getOffspringBySire(sireId); }
    public LiveData<List<Rabbit>> getOffspringByDam(long damId) { return rabbitDao.getOffspringByDam(damId); }
    public LiveData<List<Rabbit>> getFarmBornRabbits() { return rabbitDao.getFarmBornRabbits(); }
    public LiveData<Integer> getActiveRabbitCount() { return rabbitDao.getActiveRabbitCount(); }
    public LiveData<Integer> getActiveMaleCount() { return rabbitDao.getActiveMaleCount(); }
    public LiveData<Integer> getActiveFemaleCount() { return rabbitDao.getActiveFemaleCount(); }
    public LiveData<List<Rabbit>> searchRabbits(String query) { return rabbitDao.searchRabbits(query); }
    public List<Rabbit> searchRabbitsSync(String query) { return rabbitDao.searchRabbitsSync(query.trim()); }
    public LiveData<List<Rabbit>> getRabbitsByBreed(String breed) { return rabbitDao.getRabbitsByBreed(breed); }

    public long insert(Rabbit rabbit) {
        rabbit.updatedAt = System.currentTimeMillis();
        if (rabbit.createdAt <= 0) rabbit.createdAt = System.currentTimeMillis();
        return rabbitDao.insert(rabbit);
    }

    public void insertWithBreed(Rabbit rabbit) {
        executor.execute(() -> {
            rabbit.updatedAt = System.currentTimeMillis();
            if (rabbit.createdAt <= 0) rabbit.createdAt = System.currentTimeMillis();
            if (rabbit.isCemental) {
                rabbitDao.clearCemental();
            }
            long id = rabbitDao.insert(rabbit);
            if (rabbit.breed != null && !rabbit.breed.trim().isEmpty()) {
                Breed existing = breedDao.getBreedByName(rabbit.breed.trim());
                if (existing != null) {
                    breedDao.incrementUsage(rabbit.breed.trim(), System.currentTimeMillis());
                } else {
                    breedDao.insert(new Breed(rabbit.breed.trim()));
                }
            }
        });
    }

    public void update(Rabbit rabbit) {
        rabbit.updatedAt = System.currentTimeMillis();
        executor.execute(() -> {
            if (rabbit.isCemental) {
                rabbitDao.clearCemental();
            }
            rabbitDao.update(rabbit);
            if (rabbit.breed != null && !rabbit.breed.trim().isEmpty()) {
                Breed existing = breedDao.getBreedByName(rabbit.breed.trim());
                if (existing != null) {
                    breedDao.incrementUsage(rabbit.breed.trim(), System.currentTimeMillis());
                } else {
                    breedDao.insert(new Breed(rabbit.breed.trim()));
                }
            }
        });
    }

    public void delete(Rabbit rabbit) {
        executor.execute(() -> rabbitDao.delete(rabbit));
    }

    public void deleteById(long id) {
        executor.execute(() -> rabbitDao.deleteById(id));
    }

    public void setCemental(Rabbit rabbit) {
        executor.execute(() -> {
            rabbitDao.clearCemental();
            rabbit.isCemental = true;
            rabbit.updatedAt = System.currentTimeMillis();
            rabbitDao.update(rabbit);
        });
    }

    public void moveRabbit(long rabbitId, Integer fromCageId, Integer toCageId, String reason) {
        executor.execute(() -> {
            Rabbit rabbit = rabbitDao.getRabbitByIdSync(rabbitId);
            if (rabbit != null) {
                MovementRecord movement = new MovementRecord();
                movement.rabbitId = rabbitId;
                movement.fromCageId = fromCageId;
                movement.toCageId = toCageId;
                movement.reason = reason;
                movement.movementDate = System.currentTimeMillis();
                movement.createdAt = System.currentTimeMillis();
                movementDao.insert(movement);
                rabbit.cageId = toCageId;
                rabbit.updatedAt = System.currentTimeMillis();
                rabbitDao.update(rabbit);
            }
        });
    }

    public List<String> getBreedSuggestions(String query) {
        return breedDao.searchBreeds(query);
    }

    public LiveData<List<String>> getAllBreedNames() {
        return breedDao.getAllBreedNames();
    }
}
