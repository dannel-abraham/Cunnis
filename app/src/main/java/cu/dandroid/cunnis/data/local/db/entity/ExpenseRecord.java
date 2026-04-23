package cu.dandroid.cunnis.data.local.db.entity;

import androidx.room.Entity;
import androidx.annotation.NonNull;

@Entity(tableName = "expense_records")
public class ExpenseRecord {
    @androidx.room.PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String category; // ExpenseCategory enum value
    public String description;
    public double amount;
    public String currency = "USD";
    public long rabbitId; // optional, related rabbit
    public int cageId; // optional, related cage
    @NonNull
    public long expenseDate;
    public String notes;
    @NonNull
    public long createdAt = System.currentTimeMillis();
}
