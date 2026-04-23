package cu.dandroid.cunnis.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import cu.dandroid.cunnis.data.local.db.entity.Rabbit;
import cu.dandroid.cunnis.data.local.repository.RabbitRepository;

public class RabbitViewModel extends AndroidViewModel {
    private final RabbitRepository repository;
    private final LiveData<List<Rabbit>> allRabbits;
    private final LiveData<List<Rabbit>> activeRabbits;
    private final LiveData<Rabbit> cemental;
    private final LiveData<List<Rabbit>> activeFemales;
    private final LiveData<Integer> activeCount;
    private final LiveData<Integer> activeMaleCount;
    private final LiveData<Integer> activeFemaleCount;
    private final MutableLiveData<Rabbit> selectedRabbit = new MutableLiveData<>();
    private final MutableLiveData<List<Rabbit>> searchResults = new MutableLiveData<>();

    public RabbitViewModel(@NonNull Application application) {
        super(application);
        repository = new RabbitRepository(application);
        allRabbits = repository.getAllRabbits();
        activeRabbits = repository.getActiveRabbits();
        cemental = repository.getCemental();
        activeFemales = repository.getActiveFemales();
        activeCount = repository.getActiveRabbitCount();
        activeMaleCount = repository.getActiveMaleCount();
        activeFemaleCount = repository.getActiveFemaleCount();
    }

    public LiveData<List<Rabbit>> getAllRabbits() { return allRabbits; }
    public LiveData<List<Rabbit>> getActiveRabbits() { return activeRabbits; }
    public LiveData<Rabbit> getCemental() { return cemental; }
    public LiveData<List<Rabbit>> getActiveFemales() { return activeFemales; }
    public LiveData<Integer> getActiveCount() { return activeCount; }
    public LiveData<Integer> getActiveMaleCount() { return activeMaleCount; }
    public LiveData<Integer> getActiveFemaleCount() { return activeFemaleCount; }
    public LiveData<Rabbit> getSelectedRabbit() { return selectedRabbit; }
    public LiveData<List<Rabbit>> getSearchResults() { return searchResults; }

    public void selectRabbit(Rabbit rabbit) { selectedRabbit.setValue(rabbit); }
    public void selectRabbitById(long id) { selectedRabbit.setValue(repository.getRabbitByIdSync(id)); }

    public LiveData<Rabbit> getRabbitById(long id) { return repository.getRabbitById(id); }
    public Rabbit getRabbitByIdSync(long id) { return repository.getRabbitByIdSync(id); }
    public LiveData<List<Rabbit>> getRabbitsByCage(int cageId) { return repository.getRabbitsByCage(cageId); }
    public LiveData<List<Rabbit>> getOffspringBySire(long sireId) { return repository.getOffspringBySire(sireId); }
    public LiveData<List<Rabbit>> getOffspringByDam(long damId) { return repository.getOffspringByDam(damId); }
    public LiveData<List<String>> getAllBreedNames() { return repository.getAllBreedNames(); }
    public List<String> getBreedSuggestions(String query) { return repository.getBreedSuggestions(query); }

    public void insert(Rabbit rabbit) {
        new Thread(() -> repository.insertWithBreed(rabbit)).start();
    }

    public void update(Rabbit rabbit) {
        repository.update(rabbit);
    }

    public void delete(Rabbit rabbit) { repository.delete(rabbit); }
    public void setCemental(Rabbit rabbit) { repository.setCemental(rabbit); }

    public void moveRabbit(long rabbitId, Integer fromCageId, Integer toCageId, String reason) {
        repository.moveRabbit(rabbitId, fromCageId, toCageId, reason);
    }

    public void search(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchResults.setValue(null);
        } else {
            searchResults.setValue(repository.searchRabbitsSync(query.trim()));
        }
    }
}
