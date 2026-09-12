package com.safespend.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.safespend.app.data.dao.BudgetDao
import com.safespend.app.data.dao.CategoryDao
import com.safespend.app.data.dao.GoalDao
import com.safespend.app.data.dao.TransactionDao
import com.safespend.app.data.entity.BudgetEntity
import com.safespend.app.data.entity.CategoryEntity
import com.safespend.app.data.entity.GoalEntity
import com.safespend.app.data.entity.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SafeSpendDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao

    companion object {
        private const val DB_NAME = "safespend.db"

        @Volatile
        private var instance: SafeSpendDatabase? = null

        fun get(context: Context): SafeSpendDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): SafeSpendDatabase =
            Room.databaseBuilder(context, SafeSpendDatabase::class.java, DB_NAME)
                // Foreign keys are off by default in SQLite; without this the
                // ON DELETE RESTRICT on transactions.category_id does nothing.
                .addCallback(SeedCallback)
                .build()

        /**
         * Seeds the default categories the first time the database file is created.
         *
         * The inserts are raw SQL inside `onCreate` rather than a coroutine kicked
         * off afterwards, because `onCreate` runs inside the same transaction that
         * creates the tables. A user who lands on "Add transaction" one millisecond
         * after first launch still finds a populated category picker.
         */
        private object SeedCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                DefaultCategories.rows.forEachIndexed { index, row ->
                    db.execSQL(
                        "INSERT INTO categories (name, icon_key, color_index, type, sort_order, is_archived) " +
                            "VALUES (?, ?, ?, ?, ?, 0)",
                        arrayOf(row.name, row.iconKey, row.colorIndex, row.type, index),
                    )
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }
    }
}

/** The starter category set. Users can add their own; these just remove the blank-slate problem. */
object DefaultCategories {

    data class Row(val name: String, val iconKey: String, val colorIndex: Int, val type: String)

    val rows = listOf(
        Row("Food & Dining", "restaurant", 0, "EXPENSE"),
        Row("Groceries", "groceries", 1, "EXPENSE"),
        Row("Transport", "transport", 2, "EXPENSE"),
        Row("Bills & Utilities", "bills", 3, "EXPENSE"),
        Row("Rent", "rent", 4, "EXPENSE"),
        Row("Health", "health", 5, "EXPENSE"),
        Row("Education", "education", 6, "EXPENSE"),
        Row("Shopping", "shopping", 7, "EXPENSE"),
        Row("Entertainment", "entertainment", 0, "EXPENSE"),
        Row("Family", "family", 1, "EXPENSE"),
        Row("Other", "other", 7, "EXPENSE"),
        Row("Salary", "salary", 0, "INCOME"),
        Row("Freelance", "freelance", 2, "INCOME"),
        Row("Business", "business", 3, "INCOME"),
        Row("Gift", "gift", 5, "INCOME"),
        Row("Other Income", "other", 7, "INCOME"),
    )
}
