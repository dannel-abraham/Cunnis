package cu.dandroid.cunnis.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import java.util.List;
import cu.dandroid.cunnis.data.local.db.entity.Cage;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.local.repository.CageRepository;

public class CageViewModel extends AndroidViewModel {
    private final CageRepository repository;
    private final LiveData<List<Cage>> allCages;
    private final LiveData<Integer> cageCount;

    public CageViewModel(@NonNull Application application) {
        super(application);
        repository = new CageRepository(application);
        allCages = repository.getAllCages();
        cageCount = repository.getCageCount();
    }

    public LiveData<List<Cage>> getAllCages() { return allCages; }
    public LiveData<Integer> getCageCount() { return cageCount; }
    public LiveData<Cage> getCageById(int id) { return repository.getCageById(id); }
    public LiveData<List<Rabbit>> getRabbitsByCage(int cageId) { return repository.getRabbitsByCage(cageId); }
    public List<Rabbit> getRabbitsByCageSync(int cageId) { return repository.getRabbitsByCageSync(cageId); }

    public void insert(Cage cage) {
        new Thread(() -> repository.insert(cage)).start();
    }

    public void update(Cage cage) { repository.update(cage); }
    public void delete(Cage cage) { repository.delete(cage); }
    public void deleteById(int id) { repository.deleteById(id); }
}
