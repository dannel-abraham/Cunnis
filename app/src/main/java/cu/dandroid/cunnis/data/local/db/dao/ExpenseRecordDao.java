package cu.dandroid.cunnis.data.local.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import cu.dandroid.cunnis.data.local.db.entity.ExpenseRecord;
import java.util.List;

@Dao
public interface ExpenseRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ExpenseRecord record);

    @Update
    void update(ExpenseRecord record);

    @Delete
    void delete(ExpenseRecord record);

    @Query("SELECT * FROM expense_records ORDER BY expenseDate DESC")
    LiveData<List<ExpenseRecord>> getAllExpenses();

    @Query("SELECT * FROM expense_records ORDER BY expenseDate DESC")
    List<ExpenseRecord> getAllExpensesSync();

    @Query("SELECT SUM(amount) FROM expense_records WHERE expenseDate BETWEEN :startDate AND :endDate")
    double getTotalExpensesBetween(long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM expense_records")
    double getTotalExpenses();

    @Query("SELECT * FROM expense_records WHERE category = :category ORDER BY expenseDate DESC")
    LiveData<List<ExpenseRecord>> getExpensesByCategory(String category);

    @Query("SELECT * FROM expense_records WHERE category = :category ORDER BY expenseDate DESC")
    List<ExpenseRecord> getExpensesByCategorySync(String category);

    @Query("SELECT SUM(amount) FROM expense_records WHERE category = :category")
    double getTotalByCategory(String category);
}
