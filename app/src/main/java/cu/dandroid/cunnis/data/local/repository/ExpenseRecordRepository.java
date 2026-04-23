package cu.dandroid.cunnis.data.local.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cu.dandroid.cunnis.CunnisApp;
import cu.dandroid.cunnis.data.local.db.dao.ExpenseRecordDao;
import cu.dandroid.cunnis.data.local.db.entity.ExpenseRecord;

public class ExpenseRecordRepository {
    private final ExpenseRecordDao dao;
    private final ExecutorService executor;

    public ExpenseRecordRepository(Application app) {
        dao = ((CunnisApp) app).getDatabase().expenseRecordDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<ExpenseRecord>> getAllExpenses() { return dao.getAllExpenses(); }
    public List<ExpenseRecord> getAllExpensesSync() { return dao.getAllExpensesSync(); }
    public LiveData<List<ExpenseRecord>> getExpensesByCategory(String category) { return dao.getExpensesByCategory(category); }
    public double getTotalExpenses() { return dao.getTotalExpenses(); }
    public double getTotalExpensesBetween(long start, long end) { return dao.getTotalExpensesBetween(start, end); }

    public long insert(ExpenseRecord record) {
        record.createdAt = System.currentTimeMillis();
        return dao.insert(record);
    }

    public void update(ExpenseRecord record) {
        executor.execute(() -> dao.update(record));
    }

    public void delete(ExpenseRecord record) {
        executor.execute(() -> dao.delete(record));
    }
}
